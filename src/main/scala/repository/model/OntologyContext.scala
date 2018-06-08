package repository.model

import slick.lifted.Tag

case class OntologyContext(ontologyContextId:Long,mainNode:String,currentNode:String,chatId:Long,isActive:Boolean)

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
class OntologyContextTable(tag:Tag) extends Table[OntologyContext](tag,"ontology_context") {
  def ontologyContextId = column[Long]("ontology_context_id",O.PrimaryKey)
  def mainNode = column[String]("main_node")
  def currentNode = column[String]("current_node")
  def chatId = column[Long]("chat_id")
  def isActive = column[Boolean]("is_active")
  override def * : ProvenShape[OntologyContext]= (ontologyContextId,mainNode,currentNode,chatId,isActive) <> (OntologyContext.tupled,OntologyContext.unapply)
}
