package kie

import bot.WitIntent
import collection.JavaConversions._
import scala.beans.BeanInfo
sealed trait BotFact
class BotFacts {

}

@BeanInfo
case class MessageResponse(intent:String,entities:Map[String,List[WitIntent]]) extends BotFact{
  def relativeJavaMap:java.util.Map[String,List[WitIntent]] = entities

}

