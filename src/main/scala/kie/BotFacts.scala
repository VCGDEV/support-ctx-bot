package kie
import bot.IntentClassification
import repository.model.Conversation
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
case class MessageResponse(var intent:String,var entities:Map[String,List[WitIntent]],var conversation:Conversation, val message:String) extends BotFact{
  var responseString:String = ""
  var classification:IntentClassification = null
  def setResponse(response:String) = this.responseString= response
  def setContext(context:String) = this.conversation.currentContext = context
  def context():String = this.conversation.currentContext
  def clasifyConversation() = {
    this.conversation.summary = message
    if (classification != null) {
      this.conversation.category = this.classification.mainCategoryId
      this.conversation.subcategory = this.classification.categoryId
    }
  }
  def sendNextMessageToWit(send:Boolean) = this.conversation.sendToNlpNext = send
  def setSummary(summary:String) = this.conversation.summary = summary
  def setDescription(description:String) = this.conversation.description = description
  def containsEntity(entity:String) = this.entities.contains(entity)
  def setClassification(classification: IntentClassification) = this.classification = classification
  def cleanConversation() = {
    this.conversation.currentContext = ""
    this.conversation.summary = ""
    this.conversation.sendToNlpNext = true
    this.conversation.description = ""
    this.conversation.category = ""
    this.conversation.subcategory = ""
    this.conversation.customer = ""
  }
}

