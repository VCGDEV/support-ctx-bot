package sparql.entities

import org.w3.banana.RDF

case class Intention(intentType:String,value:String,hasAnswer:Set[RDF#URI])
