package repository.model

import slick.lifted.TableQuery
import telegram.SafeBot
import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._

object IssueNotClassifiedDao extends TableQuery(new IssueNotClassifiedTable(_)) with JdbcConnector {
  def save(issueNotClassified: IssueNotClassified): Future[IssueNotClassified] ={
    db.run(this returning this.map(_.issueId) into((cve,id)=>cve.copy(issueId = id))+=issueNotClassified)
  }
}
