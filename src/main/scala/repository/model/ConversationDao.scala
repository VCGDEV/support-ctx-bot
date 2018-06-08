package repository.model


import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration._
object ConversationDao extends TableQuery(new ConversationTable(_)) with JdbcConnector {

  val logger = Logger(LoggerFactory.getLogger(ConversationDao.getClass))

  def findById(id:Long):Option[Conversation] = {
    logger.debug(s"Search Conversation using ID: $id")
    Await.result(db .run(this.filter(_.chatId === id).result).map(_.headOption),5.second)
  }

  def create(conversation: Conversation):Future[Conversation] = {
    logger.debug(s"Create new conversation $conversation")
    db.run(this returning this.map(_.chatId) into((cve,id)=>cve.copy(chatId = id)) += conversation)
  }

  def update(conversation: Conversation): Unit ={
    logger.debug(s"Update conversation $conversation")
    val q = this.filter(_.chatId === conversation.chatId).update(conversation)
    db.run(q)
  }
}
