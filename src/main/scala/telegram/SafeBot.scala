
package telegram
import java.util.UUID

import bot.BotResponse
import com.typesafe.scalalogging.Logger
import config.oauth.OauthFactory
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.GetFile
import kie.{BotResponseEngine, MessageResponse}

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

import scala.concurrent.Future
import scala.util.Random
object SafeBot extends TelegramBot with Polling with Commands {

  val conf: Config = ConfigFactory.load
  lazy val token: String = scala.util.Properties.envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromInputStream(getClass.getResourceAsStream("/bot.token")).getLines().mkString)
  implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
  val botResponses: Array[BotResponse] = loadResponses()
  val googleKey:String = ""
  val ignoredWords:Seq[String] = Seq("/start","/credentials")
  override val logger = Logger(LoggerFactory.getLogger(SafeBot.getClass))
  onCommand('start) { implicit msg => reply("Bienvenido, Mi nombre es Luky y soy un Bot de soporte tecnico, En que puedo ayudarte?!!!") }
  onCommand("credentials") {implicit msg=>reply(OauthFactory.name())}
  onMessage({implicit msg =>{
    MDC.put("UUID",UUID.randomUUID().toString)
    val name = msg.from.get.firstName
    msg.voice match {
      case Some(voice) =>
          logger.info(s"Download Telegram voice record ${voice.fileId}")
        downloadAudio(voice.fileId).onComplete{
          case Success(bytes)=>
            reply(getUserIntent(GoogleSpeechRecognition.recognizeSpeech(bytes),msg.chat.id,name))
          case Failure(t)=>logger.error(s"Error trying to download audio from telegram ${t}")
        }
      case None =>
        msg.text match {
          case Some(text) => reply(getUserIntent(text,msg.chat.id,name))
          case None => logger.error("Message without voice and text")
        }
    }
    MDC.remove("UUID")
    }
  })

  def getUserIntent(msgText:String,chatId:Long,name:String): String ={
    logger.info(s"Get user intent ${msgText}")
    var reply:String = ""
    if(!ignoredWords.contains(msgText)){
      val witResponse = WitAiProcessor.getIntents(msgText)
      logger.debug(s"$witResponse")
      val intent = witResponse.entities.get("intent")
      //BotResponseEngine.determineBotResponse(MessageResponse(w.value, witResponse.entities.filterKeys(!_.equals("intent"))),chatId) TODO pass the intent to drools and make something
      intent match {
        case Some(witIntents) =>
          witIntents.foreach(intent=>{
            logger.info(s"Processing intent ${intent.value} with confidence: ${intent.confidence}")
            val answers = botResponses.filter(p=>p.tag==intent.value)
            if(answers.length>0){
              reply = getRandomElement(answers(0).responses, new Random(System.currentTimeMillis())).replace("{name}",name)
            }else{
              reply = "No training for your request"
            }
          })
        case None => reply = "Lo lamento no puedo entenderte"
      }
    }else logger.info("Bot don process word")
    reply
  }

  def loadResponses():Array[BotResponse] = {
    val stream = getClass.getResourceAsStream("/intents.json")
    val jsonString:String = Source.fromInputStream(stream).getLines
        .mkString
    parse(jsonString).extract[Array[BotResponse]]
  }

  def downloadAudio(fileId:String): Future[ByteString] ={
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

  def getRandomElement(list: Seq[String], random: Random): String = list(random.nextInt(list.length))
}
