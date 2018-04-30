package asigno

case class LabelView(label:String) {
  override def toString: String = s"{ label = $label }"
}
