package wit

/**
  * @author Victor de la Cruz Gonzalez
  * @version 1.0.0
  * Wrapper for response from <strong>https://wit.ai</strong>
  * */
class WitResponse(var _text:String,var entities:Map[String,List[WitIntent]], var msg_id:String) {
  override def toString: String = s"{_text=${_text}, entities=${entities}, msg_id=${msg_id}}"
}
