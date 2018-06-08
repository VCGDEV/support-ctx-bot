package repository.model

import slick.lifted.TableQuery

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
object ConversationHistoryDao extends TableQuery(new ConversationHistoryTable(_)) with JdbcConnector {
  def save(history: ConversationHistory):Future[ConversationHistory]={
    db.run(this returning this.map(_.historyId) into((cve,id)=>cve.copy(historyId = id)) += history)
  }
}
