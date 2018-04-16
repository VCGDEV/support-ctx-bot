package kie
import wit.WitIntent

import scala.beans.BeanInfo
sealed trait BotFact
class BotFacts {

}

/**
  * @author Vitor de la Cruz
  * @version 1.0.0
  * Class definition to manage intent and the entities to process in rule engine
  * */
@BeanInfo
case class MessageResponse(intent:String,entities:Map[String,List[WitIntent]]) extends BotFact{

}

