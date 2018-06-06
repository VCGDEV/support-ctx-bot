package sparql.entities

import org.w3.banana.RDF

case class User(name:String, email:String, id:String, hasPC:RDF#URI)
