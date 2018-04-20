
package telegram
import java.util.UUID

import bot.BotResponse
import com.typesafe.scalalogging.Logger
import config.oauth.OauthFactory
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.GetFile

import scala.io.Source
import org.slf4j.{LoggerFactory, MDC}
import wit.WitAiProcessor
import com.typesafe.config.{Config, ConfigFactory}
import net.liftweb.json.{DefaultFormats, parse}

import scala.util.{Failure, Success}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import google.GoogleSpeechRecognition
import kie.{BotResponseEngine, MessageResponse}

import scala.concurrent.Future
import scala.util.Random

/**
  * @author Victor de la Cruz Gonzalez
  * @version 1.0.0
  * Object definition to process incoming messages from telegram bot,
  *  for full documentation go to: <strong>https://github.com/mukel/telegrambot4s<strong>
  * */
object SafeBot extends TelegramBot with Polling with Commands {

  //load bot configuration for telegram
  val conf: Config = ConfigFactory.load
  lazy val token: String = scala.util.Properties.envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromInputStream(getClass.getResourceAsStream("/bot.token")).getLines().mkString)
  implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
  val botResponses: Array[BotResponse] = loadResponses()
  val ignoredWords:Seq[String] = Seq("/start","/credentials")
  override val logger = Logger(LoggerFactory.getLogger(SafeBot.getClass))

  /**
    * Process <strong>/start</strong> command from telegram
    * */
  onCommand('start) { implicit msg => reply("Bienvenido, Mi nombre es Luky y soy un Bot de soporte tecnico, En que puedo ayudarte?!!!") }

  /**
    * Process <strong>/credentials</strong> command from telegram
    * */
  onCommand("credentials") {implicit msg=>reply(OauthFactory.name())}


  /**
    * Process incoming messages from telegram
    * */
  onMessage({implicit msg =>{
    MDC.put("UUID",UUID.randomUUID().toString)
    val name = msg.from.get.firstName
    msg.voice match {
      case Some(voice) =>
          logger.info(s"Download Telegram voice record ${voice.fileId}")
        downloadFile(voice.fileId).onComplete{
          case Success(bytes)=>
            reply(getUserIntent(GoogleSpeechRecognition.recognizeSpeech(bytes),msg.chat.id,name))
          case Failure(t)=>logger.error(s"Error trying to download audio from telegram ${t}")
        }
      case None =>
        msg.text match {
          case Some(text) =>
            if(!text.isEmpty && !ignoredWords.contains(text))
              reply(getUserIntent(text,msg.chat.id,name))
          case None => logger.error("Message without voice and text")
        }
    }
    MDC.remove("UUID")
    }
  })

  /**
    *  Determine user intent and get the correct response
    *  @param chatId unique chat ID from telegram
    *  @param msgText the message to process using an NLP to determine the intent
    *  @return Message to send as response to user
    * */
  def getUserIntent(msgText:String,chatId:Long,name:String): String ={
    logger.info(s"Get user intent ${msgText}")
    var reply:String = ""
    if(!ignoredWords.contains(msgText)){
      val witResponse = WitAiProcessor.getIntents(msgText)
      logger.info(s"$witResponse")
      val intent = witResponse.entities.get("intent")
      intent match {
        case Some(witIntents) =>
          witIntents.foreach(intent=>{
            BotResponseEngine.determineBotResponse(MessageResponse(intent.value, witResponse.entities.filterKeys(!_.equals("intent"))),chatId) //TODO pass the intent to drools and make something
            logger.info(s"Processing intent ${intent.value} with confidence: ${intent.confidence}")
            val answers = botResponses.filter(p=>p.tag==intent.value)
            if(answers.length>0){
              reply = getRandomElement(answers(0).responses, new Random(System.currentTimeMillis())).replace("{name}",name)
            }else{
              reply = "No training for your request"
            }
          })
        case None =>
          BotResponseEngine.determineBotResponse(MessageResponse("None", witResponse.entities.filterKeys(!_.equals("intent"))),chatId) //TODO pass the intent to drools and make something
          reply = "Working in this bot"
      }
    }else logger.info("Bot don process word")
    reply
  }

  /***
    *  Load default responses from <strong>/intents.json</strong> file
    *  @return array with BotResponse objects
    */
  def loadResponses():Array[BotResponse] = {
    val stream = getClass.getResourceAsStream("/intents.json")
    val jsonString:String = Source.fromInputStream(stream).getLines
        .mkString
    parse(jsonString).extract[Array[BotResponse]]
  }

  /**
    * Download file from telegram using the file ID
    * @param fileId the file to be downloaded
    * @return Future[ByteString] of the file
    * */
  def downloadFile(fileId:String): Future[ByteString] ={
    return request(GetFile(fileId)).flatMap(s=>
      s.filePath match {
        case Some(filePath) =>
          val url:String = s"https://api.telegram.org/file/bot$token/$filePath"
          for{
            res <- Http().singleRequest(HttpRequest(uri=Uri(url)))
            if res.status.isSuccess()
            bytes <- Unmarshal(res).to[ByteString]
          } yield bytes
        case None => throw new Exception("No file was found")
      }
    )
  }

  /**
    * Take a random element from possible responses array
    *  @param list a sequence of posible arrays
    *  @param random Random object with configurations to obtain next position of array
    *  @return message response
    * */
  def getRandomElement(list: Seq[String], random: Random): String = list(random.nextInt(list.length))
}
