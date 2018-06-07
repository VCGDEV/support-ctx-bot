package sparql.entities

import org.w3.banana.RDF

case class User(name:String, email:String, id:String, officePhone:String,mobilePhone:String,
                hasAddress:Option[RDF#URI],isInCompany:Option[RDF#URI],hasProperty:Set[RDF#URI])
