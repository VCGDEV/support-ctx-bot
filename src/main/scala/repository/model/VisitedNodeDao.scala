package repository.model

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import slick.lifted.TableQuery

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
object VisitedNodeDao extends TableQuery(new VisitedNodeTable(_)) with JdbcConnector {
  val logger = Logger(LoggerFactory.getLogger(VisitedNode.getClass))

  def create(node:VisitedNode):Future[VisitedNode] = {
    logger.debug(s"Create new visited node: $node")
    db.run(this returning this.map(_.visitedNodeId) into((cve,id)=>cve.copy(visitedNodeId = id)) += node)
  }

  def update(node:VisitedNode):Future[Int] = {
    logger.debug(s"Update visited node $node")
    val q = this.filter(_.visitedNodeId===node.visitedNodeId).update(node)
    db.run(q)
  }

  
}
