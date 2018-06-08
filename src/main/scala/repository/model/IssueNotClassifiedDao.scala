package repository.model

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import slick.lifted.TableQuery

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._

object IssueNotClassifiedDao extends TableQuery(new IssueNotClassifiedTable(_)) with JdbcConnector {

  val logger = Logger(LoggerFactory.getLogger(IssueNotClassifiedDao.getClass))

  def save(issueNotClassified: IssueNotClassified): Future[IssueNotClassified] ={
    logger.debug(s"Insert new unclassified issue: $issueNotClassified")
    db.run(this returning this.map(_.issueId) into((cve,id)=>cve.copy(issueId = id))+=issueNotClassified)
  }
}
