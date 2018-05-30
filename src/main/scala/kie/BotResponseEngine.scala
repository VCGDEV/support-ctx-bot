package kie
import java.sql.Timestamp
import java.util.Date

import bot.IntentClassification
import config.logger.CustomAgendaEventListener
import mail.MailService
import org.slf4j.LoggerFactory
import repository.model.{Conversation, ConversationDao, ConversationHistory, ConversationHistoryDao}

import scala.io.Source
import net.liftweb.json.{DefaultFormats, parse}
/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Object definition to fire rules
  * */
object BotResponseEngine {
  val logger = LoggerFactory.getLogger(BotResponseEngine.getClass)
  val classifications = loadIntentClassifications()
  val mailService = new MailService
  /**
    *  Fire rules to obtain bot response
    *  @param process the response to process in the rules
    * */
  def determineBotResponse(process:MessageResponse): String = {
    //save incoming message, for history
    val history = new ConversationHistory(0,process.intent,process.message,process.username,"luky",new Timestamp(new Date().getTime),
      process.chatId)
    ConversationHistoryDao.save(history)
    val session = Kie.newSession
    logger.info(s"Sending data to rule engine - context: ${process.conversation.currentContext}")
    val classFind = classifications.find(c => c.tag == process.intent)
    if(classFind.isDefined)
      process.setClassification(classFind.get)
    session.insert(process)
    session.addEventListener(new CustomAgendaEventListener())
    session.fireAllRules()
    session.dispose()
    //update conversation with new values
    val conversation:Conversation = process.conversation
    ConversationDao.update(conversation)
    //save response message
    val response = new ConversationHistory(0,process.intent,process.responseString,"luky",process.username,new Timestamp(new Date().getTime),
      process.chatId)
    ConversationHistoryDao.save(response)
    process.responseString
  }

  def loadIntentClassifications():List[IntentClassification] = {
    implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
    val stream = getClass.getResourceAsStream("/intents.json")
    val jsonString:String = Source.fromInputStream(stream).getLines
      .mkString
    parse(jsonString).extract[List[IntentClassification]]
  }
}
