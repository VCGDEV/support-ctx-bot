package repository.model


import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.duration._
object ConversationDao extends TableQuery(new ConversationTable(_)) with JdbcConnector {
  def findById(id:Long):Option[Conversation] = {
    Await.result(db .run(this.filter(_.chatId === id).result).map(_.headOption),5.second)
  }

  def create(conversation: Conversation):Future[Conversation] = {
    db.run(this returning this.map(_.chatId) into((cve,id)=>cve.copy(chatId = id)) += conversation)
  }

  def update(conversation: Conversation): Unit ={
    val q = this.filter(_.chatId === conversation.chatId).update(conversation)
    db.run(q)
  }
}
