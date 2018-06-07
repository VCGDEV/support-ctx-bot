package sparql.entities

import org.w3.banana.RDF

case class Company(name:String,entityType:String,hasAddress:Option[RDF#URI],hasProperty:Set[RDF#URI])
