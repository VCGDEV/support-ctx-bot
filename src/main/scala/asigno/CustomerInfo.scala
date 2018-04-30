package asigno

case class CustomerInfo(key:String,legend:String,value:String) {
  override def toString: String =
    s"{ key = $key, legend = $legend,value = $value }"
}
