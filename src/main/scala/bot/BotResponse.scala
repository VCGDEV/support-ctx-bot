package bot

class BotResponse(var tag:String,var responses:Seq[String],var context:String){

  override def toString: String ={
    s"""TAG:$tag - RESPONSES: $responses - CONTEXT: $context"""
  }
}
