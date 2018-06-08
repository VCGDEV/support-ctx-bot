package repository.model

import slick.lifted.{ProvenShape, Tag}

case class VisitedNode(visitedNodeId:Long,nodeURI:String,ontologyContextId:Long)

import slick.jdbc.PostgresProfile.api._
class VisitedNodeTable(tag:Tag) extends Table[VisitedNode](tag,"visited_node"){
  def visitedNodeId = column[Long]("visited_node_id",O.PrimaryKey)
  def nodeURI = column[String]("node_uri")
  def ontologyContextId = column[Long]("ontology_context_id")
  override def * : ProvenShape[VisitedNode]= (visitedNodeId,nodeURI,ontologyContextId) <> (VisitedNode.tupled,VisitedNode.unapply)
}


