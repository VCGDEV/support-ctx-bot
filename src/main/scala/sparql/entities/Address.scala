package sparql.entities

case class Address(government:String,postalCode:String,country:String,city:String,numInt:Option[String],
                   adjacent1:Option[String],adjacent2:Option[String],
                   numExt:Option[String],street:String)
