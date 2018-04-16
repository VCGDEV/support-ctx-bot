package kie

import java.util

import com.typesafe.scalalogging.Logger
import config.logger.CustomAgendaEventListener
import org.slf4j.LoggerFactory

/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Object definition to fire rules
  * */
object BotResponseEngine {
  val logger = Logger(LoggerFactory.getLogger(BotResponseEngine.getClass))
  /**
    *  Fire rules to obtain bot response
    *  @param process the response to process in the rules
    *  @param sessionId chat Id from telegram
    * */
  def determineBotResponse(process:MessageResponse,sessionId:Long): List[String] = {
    logger.info(s"Create session for chat: $sessionId")
    val session = Kie.newStatelessSession
    val conversationContext = new ConversationContext("")
    val elegibleResponses = new util.ArrayList[String]()
    session.setGlobal("elegibleResponses",elegibleResponses)
    session.setGlobal("conversationContext",conversationContext)
    //session.insert(process)
    session.addEventListener(new CustomAgendaEventListener())
    //session.fireAllRules()
    //session.dispose()
    session.execute(process)
    logger.info(s"Kie session result $elegibleResponses context ${conversationContext.context}")
    elegibleResponses.toArray.toList.map(s=>s.toString)
  }
}
