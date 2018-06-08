package repository.model

import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
object OntologyContextDao extends TableQuery(new OntologyContextTable(_)) with JdbcConnector {
  def findActiveContext():Option[OntologyContext] = {
    Await.result(db.run(this.filter(_.isActive === true).result).map(_.headOption),3.second)
  }

  
}
