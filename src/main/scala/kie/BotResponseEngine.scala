package kie

import java.util

object BotResponseEngine {

  def determineBotResponse(process:MessageResponse): List[AnyRef] = {
    val session = Kie.newSession
    val elegibleResponses = new util.ArrayList[String]()
    session.setGlobal("elegibleResponses",elegibleResponses)
    session.insert(process)
    session.fireAllRules()
    session.dispose()
    elegibleResponses.toArray.toList
  }
}
