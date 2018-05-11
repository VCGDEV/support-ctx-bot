
package telegram
import java.sql.Timestamp
import java.util.{Date, UUID}

import config.oauth.OauthFactory
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.GetFile

import scala.io.Source
import org.slf4j.{LoggerFactory, MDC}
import wit.WitAiProcessor
import com.typesafe.config.{Config, ConfigFactory}
import net.liftweb.json.DefaultFormats

import scala.util.{Failure, Success}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import asigno.{AttachmentView, CommentView}
import google.GoogleSpeechRecognition
import info.mukel.telegrambot4s.models.Message
import kie.{BotResponseEngine, MessageResponse}
import org.apache.commons.codec.binary.Base64
import repository.model.{Conversation, ConversationDao}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
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
  val ignoredWords:Seq[String] = Seq("/start","/credentials","/clean")
  val LOG = LoggerFactory.getLogger(SafeBot.getClass)
  val db = slick.jdbc.JdbcBackend.Database.forConfig("db.config")

  /**
    * Process <strong>/start</strong> command from telegram
    * */
  onCommand('start) { implicit msg => reply("Bienvenido, Mi nombre es Luky y soy un Bot de soporte tecnico, En que puedo ayudarte?!!!") }


  /**
    * Process <strong>/credentials</strong> command from telegram
    * */
  onCommand("credentials") {implicit msg=>reply(OauthFactory.name())}

  onCommand("clean"){implicit msg =>
    ConversationDao.findById(msg.chat.id).onComplete{
      case Success(s)=>
        s match {
          case Some(c)=>
            c.summary = ""
            c.category = ""
            c.subcategory = ""
            c.description = ""
            c.sendToNlpNext = true
            c.currentContext = "request_summary"
            ConversationDao.update(c)
            reply("Proceso cancelado exitosamente")
          case None=>
           reply("No existe proceso a cancelar")
        }
      case Failure(f)=>
        reply("No fue posible seleccionar el proceso a ancelar")
    }
  }


  /**
    * Process incoming messages from telegram
    * */
  onMessage({implicit msg =>{
    MDC.put("UUID",UUID.randomUUID().toString)
    ConversationDao.findById(msg.chat.id).onComplete{
      case Success(s)=>
        s match {
          case Some(c)=>
            processMessage(msg,c)
          case None=>
            LOG.info("Create new conversation for telegram chat")
            val conversation:Conversation = new Conversation(msg.chat.id,"",true,
              new Timestamp(new Date().getTime),"","","","","","")
            ConversationDao.create(conversation)
                .onComplete{
                  case Success(c)=>
                    LOG.info(s"New conversation was created ${c.chatId}")
                  case Failure(e)=>
                    LOG.error(s"Cant create new conversation ${e}")
                }
            processMessage(msg, conversation)
        }
      case Failure(f)=>
        LOG.error("Error al obtener conversacion",f)
        val conversation:Conversation = new Conversation(msg.chat.id,"",true,
          new Timestamp(new Date().getTime),"","","","","","")
        processMessage(msg, conversation)
    }
    MDC.remove("UUID")
    }
  })

  def processMessage(implicit msg:Message,conversation:Conversation): Unit ={
    val name = msg.from.get.firstName
    msg.voice match {
      case Some(voice) =>
        LOG.info(s"Download Telegram voice record ${voice.fileId}")
        downloadFile(voice.fileId).onComplete{
          case Success(bytes)=>
            val caption = msg.caption match {
              case Some(c)=>c
              case None => "Evidencia"
            }
            val comment:CommentView = CommentView(caption,OauthFactory.name,OauthFactory.username,"",getFilesFromMessage)
            reply(getUserIntent(GoogleSpeechRecognition.recognizeSpeech(bytes),msg.chat.id,name,conversation,comment))
          case Failure(t)=>LOG.error(s"Error trying to download audio from telegram ${t}")
        }
      case None =>
        msg.text match {
          case Some(text) =>
            if(!text.isEmpty && !ignoredWords.contains(text)){
              val caption = msg.caption match {
                case Some(c)=>c
                case None => "Evidencia"
              }
              val comment:CommentView = CommentView(caption,OauthFactory.name,OauthFactory.username,"",getFilesFromMessage)
              reply(getUserIntent(text,msg.chat.id,name,conversation,comment))
            }
          case None =>
            if(conversation.currentContext.equals("wait_evidence")){
              val caption = msg.caption match {
                case Some(c)=>c
                case None => "Evidencia"
              }
              val comment:CommentView = CommentView(caption,OauthFactory.name,OauthFactory.username,"",getFilesFromMessage)
              reply(getUserIntent("",msg.chat.id,name,conversation,comment))
            }
        }
    }
  }

  def downloadAttachment(fileId:String,fileName:String,mimeType:String):List[AttachmentView] = {
    try{
      val res = Await.result(downloadFile(fileId),20.second)
      List(AttachmentView(fileName,Base64.encodeBase64String(res.toArray),mimeType))
    }catch {
      case e: Exception => LOG.error("Error on file download")
        List()
    }
  }

  def getFilesFromMessage(implicit msg:Message):List[AttachmentView] = {
    var attachments = List[AttachmentView]()
    msg.document match {
      case Some(document) =>
        val fileName = getValue(document.fileName)
        val mimeType = getValue(document.mimeType)
        attachments = attachments ::: downloadAttachment(document.fileId,fileName,mimeType)
      case None => LOG.info("Message has no documents")
    }
    msg.video match {
      case Some(video) =>
        val fileName = "video_note"
        val mimeType = getValue(video.mimeType)
        attachments = attachments ::: downloadAttachment(video.fileId,fileName,mimeType)
      case None => LOG.info("Message has no video")
    }
    msg.audio match {
      case Some(audio)=>
        val fileName = "audio"
        val mimeType = getValue(audio.mimeType)
        attachments = attachments ::: downloadAttachment(audio.fileId,fileName,mimeType)
      case None => LOG.info("Message has no audio")
    }
    msg.voice match {
      case Some(voice)=>
        val fileName = "voice"
        val mimeType = getValue(voice.mimeType)
        attachments = attachments ::: downloadAttachment(voice.fileId,fileName,mimeType)
      case None => LOG.info("Message has no voice")
    }
    msg.photo match {
      case Some(pics)=>
        val p = pics(pics.size -1)
        val fileName = p.fileId+".jpeg"
        val mimeType ="image/jpeg"
        attachments = attachments ::: downloadAttachment(p.fileId,fileName,mimeType);
      case None => LOG.info("Message has no images")
    }
    LOG.info(s"Attachments in message: ${attachments.size}")
    attachments
  }

  def getValue(option:Option[String]):String = {
    option match {
      case Some(value)=>value
      case None => ""
    }
  }

  /**
    *  Determine user intent and get the correct response
    *  @param chatId unique chat ID from telegram
    *  @param msgText the message to process using an NLP to determine the intent
    *  @return Message to send as response to user
    * */
  def getUserIntent(msgText:String,chatId:Long,name:String,conversation:Conversation,
                    commentView: CommentView): String ={
    LOG.info(s"Get user intent ${msgText}")
    var reply:String = ""
    if(!ignoredWords.contains(msgText)){
      val messageResponse = MessageResponse("", Map(),conversation,msgText,chatId,commentView)
      if(conversation.sendToNlpNext){//enviar a wit.ai
        val witResponse = WitAiProcessor.getIntents(msgText)
        LOG.info(s"${witResponse}")
        messageResponse.entities = witResponse.entities.filterKeys(!_.equals("intent"))
        val intent = witResponse.entities.get("intent")
        intent match {
          case Some(witIntents) =>
            witIntents.foreach(intent=>{
              messageResponse.intent = intent.value
              reply = BotResponseEngine.determineBotResponse(messageResponse)
            })
          case None => //take previous context
            reply = BotResponseEngine.determineBotResponse(messageResponse)
        }
      }else{
        reply = BotResponseEngine.determineBotResponse(messageResponse)
      }
    }
    reply.replace("{name}",name)
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
          val request = HttpRequest(uri=Uri(s"https://api.telegram.org/file/bot$token/$filePath"))
          for{
            res <- Http().singleRequest(request)
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
