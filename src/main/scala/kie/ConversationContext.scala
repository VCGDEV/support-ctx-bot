package kie

/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Class definition to manage conversation context of bot into rules engine
  * */
class ConversationContext(var context:String) {
  def setContext(context:String) = this.context = context


  override def toString():String = this.context
}
