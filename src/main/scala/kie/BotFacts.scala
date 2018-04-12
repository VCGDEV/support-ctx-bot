package kie
import wit.WitIntent

import scala.beans.BeanInfo
sealed trait BotFact
class BotFacts {

}

@BeanInfo
case class MessageResponse(intent:String,entities:Map[String,List[WitIntent]]) extends BotFact{

}

