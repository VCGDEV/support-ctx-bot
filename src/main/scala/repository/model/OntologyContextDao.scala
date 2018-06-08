package repository.model

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
object OntologyContextDao extends TableQuery(new OntologyContextTable(_)) with JdbcConnector {
  val logger = Logger(LoggerFactory.getLogger(OntologyContextDao.getClass))
  def findActiveContext(chatId:Long):Option[OntologyContext] = {
    logger.debug(s"Search active ontology context for chat: $chatId")
    Await.result(db.run(this.filter(_.isActive === true).filter(_.chatId===chatId).result).map(_.headOption),3.second)
  }

  def create(context:OntologyContext):Future[OntologyContext] = {
    logger.debug(s"Create new ontology context $context")
    db.run(this returning this.map(_.ontologyContextId) into((cve,id)=>cve.copy(ontologyContextId = id)) += context)
  }

  def update(context: OntologyContext):Future[Int] = {
    logger.debug(s"Update ontology context $context")
    val q = this.filter(_.ontologyContextId === context.ontologyContextId).update(context)
    db.run(q)
  }

}
