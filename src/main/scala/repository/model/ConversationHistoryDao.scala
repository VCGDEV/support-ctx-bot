package repository.model

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import slick.lifted.TableQuery

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
object ConversationHistoryDao extends TableQuery(new ConversationHistoryTable(_)) with JdbcConnector {
  val logger = Logger(LoggerFactory.getLogger(ConversationHistoryDao.getClass))
  def save(history: ConversationHistory):Future[ConversationHistory]={
    logger.debug(s"Insert new conversation history into database: $history")
    db.run(this returning this.map(_.historyId) into((cve,id)=>cve.copy(historyId = id)) += history)
  }
}
