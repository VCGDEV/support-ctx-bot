
package telegram
import java.sql.Timestamp
import java.util.{Date, UUID}

import config.oauth.OauthFactory
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.GetFile

import scala.io.Source
import org.slf4j.MDC
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
import kie.{BotResponseEngine, MessageResponse, ProcessIntention}
import org.apache.commons.codec.binary.Base64
import repository.model.{Conversation, ConversationDao}
import sparql.AsignoKnowledgeManagerImpl
import sparql.entities.{Intention, User}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * @author Victor de la Cruz Gonzalez
  * @version 1.0.0
  * Object definition to process incoming messages from telegram bot,
  *  for full documentation go to: <strong>https://github.com/mukel/telegrambot4s<strong>
  * */
object SafeBot extends TelegramBot with Polling with Commands {

  //load bot configuration for telegram
  lazy val conf: Config = ConfigFactory.load
  lazy val token: String = scala.util.Properties.envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromInputStream(getClass.getResourceAsStream("/bot.token")).getLines().mkString)
  implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
  val ignoredWords:Seq[String] = Seq("/start","/credentials","/clean")
  lazy val db = slick.jdbc.JdbcBackend.Database.forConfig("db.config")

  /**
    * Process <strong>/start</strong> command from telegram
    * */
  onCommand('start) { implicit msg =>
    reply("Bienvenido, Mi nombre es Luky y soy un Bot de soporte tecnico, En que puedo ayudarte?!!!")
  }


  /**
    * Process <strong>/credentials</strong> command from telegram
    * */
  onCommand("credentials") {implicit msg=>reply(OauthFactory.name())}

  onCommand("clean"){implicit msg =>
    logger.info("Clean current context from bot")
    ConversationDao.findById(msg.chat.id) match {
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
  }


  /**
    * Process incoming messages from telegram,get user details from SPARQL endpoint and verify if exists a previous conversation with the user
    * */
  onMessage({implicit msg =>{
    val uuid = UUID.randomUUID().toString
    MDC.put("UUID",uuid)
    logger.info("Process incoming request from Telegram message")
    var usernameTelegram = ""
    msg.chat.username match {
      case Some(username) => usernameTelegram = username
      case None => usernameTelegram = msg.chat.id.toString
    }
    ///search user in knwoledge base
    try {
      val user = AsignoKnowledgeManagerImpl.getUser(msg.chat.id.toString).getOrElse(throw new Exception("No user was found"))
      ConversationDao.findById(msg.chat.id) match {
            case Some(c)=>
              logger.info("Luky has a previous conversation with : {}",user.name)
              processMessage(msg,c,user)
            case None=>
              logger.info("Luky is going to create new conversation with: {}",user.name)
              val conversation:Conversation = new Conversation(msg.chat.id,"",true,
                new Timestamp(new Date().getTime),"","","","","",
                "",usernameTelegram)
              ConversationDao.create(conversation)
                .onComplete{
                  case Success(c)=>logger.debug(s"New conversation was created ${c.chatId}")
                  case Failure(e)=>logger.error(s"Cant create new conversation ${e}")
                }
              processMessage(msg, conversation,user)
          }
    }catch {
      case e: Exception => reply("No pude encontrar tu usuario, favor de solicitar el registro en asigno")
        logger.error("An error ocurred in users select",e)
    }
    MDC.remove("UUID")
    }
  })

  /**
    * Reply to user based on intent of message,
    * sends a request to NLP to know the user intent and after that uses the intent in a Rule engine to determine the correct answer
    * */
  def processMessage(implicit msg:Message,conversation:Conversation,user:User): Unit ={
    msg.voice match {
      case Some(voice) =>
        logger.info(s"Download Telegram voice record ${voice.fileId}")
        downloadFile(voice.fileId).onComplete{
          case Success(bytes)=>
            val caption = msg.caption match {
              case Some(c)=>c
              case None => "Evidencia"
            }
            val comment:CommentView = CommentView(caption,OauthFactory.name,OauthFactory.username,"",getFilesFromMessage)
            reply(getUserIntent(GoogleSpeechRecognition.recognizeSpeech(bytes),msg.chat.id,conversation,comment,user))
          case Failure(t)=>logger.error(s"Error trying to download audio from telegram ${t}")
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
              reply(getUserIntent(text,msg.chat.id,conversation,comment,user))
            }
          case None =>
            if(conversation.currentContext.equals("wait_evidence")){
              val caption = msg.caption match {
                case Some(c)=>c
                case None => "Evidencia"
              }
              val comment:CommentView = CommentView(caption,OauthFactory.name,OauthFactory.username,"",getFilesFromMessage)
              reply(getUserIntent("",msg.chat.id,conversation,comment,user))
            }
        }
    }
  }

  /**
    * Download attachment using telegram API REST
    * @param fileId from telegram response
    * @param fileName name of file to download
    * @param mimeType content type image, json, video, etc.
    * @return a new list with one element if the download is success, otherwise empty list
    * */
  def downloadAttachment(fileId:String,fileName:String,mimeType:String):List[AttachmentView] = {
    try{
      val res = Await.result(downloadFile(fileId),20.second)
      List(AttachmentView(fileName,Base64.encodeBase64String(res.toArray),mimeType))
    }catch {
      case e: Exception => logger.error("Error on file download")
        List()
    }
  }

  /**
    * Download all attachments from incoming message, this is for evidence on  ticket creation
    * @param msg the message from telegram
    * @return a list with all the attachments: pictures, voice, video...etc
    * */
  def getFilesFromMessage(implicit msg:Message):List[AttachmentView] = {
    var attachments = List[AttachmentView]()
    msg.document match {
      case Some(document) =>
        val fileName = getValue(document.fileName)
        val mimeType = getValue(document.mimeType)
        attachments = attachments ::: downloadAttachment(document.fileId,fileName,mimeType)
      case None => logger.debug("Message has no documents")
    }
    msg.video match {
      case Some(video) =>
        val fileName = "video_note"
        val mimeType = getValue(video.mimeType)
        attachments = attachments ::: downloadAttachment(video.fileId,fileName,mimeType)
      case None => logger.debug("Message has no video")
    }
    msg.audio match {
      case Some(audio)=>
        val fileName = "audio"
        val mimeType = getValue(audio.mimeType)
        attachments = attachments ::: downloadAttachment(audio.fileId,fileName,mimeType)
      case None => logger.debug("Message has no audio")
    }
    msg.voice match {
      case Some(voice)=>
        val fileName = "voice"
        val mimeType = getValue(voice.mimeType)
        attachments = attachments ::: downloadAttachment(voice.fileId,fileName,mimeType)
      case None => logger.debug("Message has no voice")
    }
    msg.photo match {
      case Some(pics)=>
        val p = pics(pics.size -1)
        val fileName = p.fileId+".jpeg"
        val mimeType ="image/jpeg"
        attachments = attachments ::: downloadAttachment(p.fileId,fileName,mimeType);
      case None => logger.debug("Message has no images")
    }
    logger.debug(s"Attachments in message: ${attachments.size}")
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
  def getUserIntent(msgText:String,chatId:Long,conversation:Conversation,
                    commentView: CommentView,user:User): String ={
    logger.debug(s"Get user intent ${msgText}")
    var reply:String = ""
    if(!ignoredWords.contains(msgText)){
      val messageResponse = MessageResponse("", Map(),conversation,msgText,chatId,commentView,conversation.username)
      if(conversation.sendToNlpNext){//enviar a wit.ai
        val witResponse = WitAiProcessor.getIntents(msgText)
        logger.info(s"NLP: \n ${witResponse}")
        reply = "In construction"
        messageResponse.entities = witResponse.entities.filterKeys(!_.equals("intent"))
        val nlpIntent = witResponse.entities.getOrElse("intent",List())
        var intentNode:Intention = Intention("","",null)
        if(nlpIntent.size>0)//solo si se tiene la intencion
          intentNode = AsignoKnowledgeManagerImpl.searchIntent(nlpIntent.head.value).getOrElse(Intention("","",null))
        val processIntention = ProcessIntention(user,intentNode,
          conversation,witResponse.entities.filterKeys(!_.equals("intent")),
          msgText,chatId)
        reply = BotResponseEngine.determineResponse(processIntention)
      }else{
        reply = BotResponseEngine.determineBotResponse(messageResponse)
      }
    }
    reply.replace("{name}",user.name)
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

}
