package bot

class WitIntent(var confidence:Double, var value:String){

  override def toString: String = s"VAL: ${value} - CONFIDENCE:${confidence}"
}
