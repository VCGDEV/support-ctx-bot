package wit

import scala.beans.BeanInfo

/**
  * @author Victor de la Cruz Gonzalez
  * @version 1.0.0
  * Intent or entity from <strong>https://wit.ai</strong>
  * */
@BeanInfo
class WitIntent(var confidence:Double, var value:String){
  override def toString: String = s"VAL: ${value} - CONFIDENCE:${confidence}"
}
