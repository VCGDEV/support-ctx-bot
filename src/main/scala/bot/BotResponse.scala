package bot

/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Class for bot responses
  * */
class BotResponse(var tag:String,var responses:Seq[String],var context:String){

  override def toString: String ={
    s"""TAG:$tag - RESPONSES: $responses - CONTEXT: $context"""
  }
}
