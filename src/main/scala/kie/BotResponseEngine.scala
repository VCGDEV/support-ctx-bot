package kie

import java.util

import com.typesafe.scalalogging.Logger
import config.logger.CustomAgendaEventListener
import org.slf4j.LoggerFactory

object BotResponseEngine {
  val logger = Logger(LoggerFactory.getLogger(BotResponseEngine.getClass))
  def determineBotResponse(process:MessageResponse): List[AnyRef] = {
    val session = Kie.newSession
    val elegibleResponses = new util.ArrayList[String]()
    session.setGlobal("elegibleResponses",elegibleResponses)
    session.insert(process)
    session.addEventListener(new CustomAgendaEventListener())
    session.fireAllRules()
    session.dispose()
    elegibleResponses.toArray.toList
  }
}
