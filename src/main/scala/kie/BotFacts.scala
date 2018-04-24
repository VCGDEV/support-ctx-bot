package kie
import wit.WitIntent

import scala.beans.BeanInfo
sealed trait BotFact
class BotFacts {

}

/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Class definition to manage intent and the entities to process in rule engine
  * */
@BeanInfo
case class MessageResponse(intent:String,entities:Map[String,List[WitIntent]],response:String) extends BotFact{
  var responseString:String = response
  def setResponse(response:String) = this.responseString= response;
  def getReply():String = this.responseString
  //def getResponse():String = this.response
}

