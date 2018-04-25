package kie

import java.sql.Date

import bot.{IntentClassification}
import com.typesafe.scalalogging.Logger
import config.logger.CustomAgendaEventListener
import org.slf4j.LoggerFactory
import repository.model.{Conversation, ConversationDao}

import scala.io.Source
import net.liftweb.json.{DefaultFormats, parse}
/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Object definition to fire rules
  * */
object BotResponseEngine {
  val logger = Logger(LoggerFactory.getLogger(BotResponseEngine.getClass))
  val classifications = loadIntentClassifications()

  /**
    *  Fire rules to obtain bot response
    *  @param process the response to process in the rules
    * */
  def determineBotResponse(process:MessageResponse): String = {
    val session = Kie.newSession
    logger.info(s"Sending data to rule engine - ConversationContext: ${process.conversation.currentContext}")
    val classFind = classifications.find(c => c.tag == process.intent)
    if(classFind.isDefined)
      process.setClassification(classFind.get)
    session.insert(process)
    session.addEventListener(new CustomAgendaEventListener())
    session.fireAllRules()
    //update conversation with new values
    val conversation:Conversation = process.conversation// Conversation(process.chatId,process.context,process.sendNextToWit, new Date(new java.util.Date().getTime))
    ConversationDao.update(conversation)
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
