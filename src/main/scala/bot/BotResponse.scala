package bot

class BotResponse(var tag:String,var responses:Seq[String]){

  override def toString={
    s"TAG:${tag} - RESPONSES: ${responses}"
  }
}
