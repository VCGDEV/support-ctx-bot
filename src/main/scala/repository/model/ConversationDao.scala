package repository.model


import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.PostgresProfile.api._
import telegram.SafeBot
object ConversationDao extends TableQuery(new ConversationTable(_)){
  //val db = slick.jdbc.JdbcBackend.Database.forConfig("db.config")
  def findById(id:Long):Future[Option[Conversation]] = {
    SafeBot.db.run(this.filter(_.chatId === id).result).map(_.headOption)
  }

  def create(conversation: Conversation):Future[Conversation] = {
    SafeBot.db.run(this returning this.map(_.chatId) into((cve,id)=>cve.copy(chatId = id)) += conversation)
  }

  def update(conversation: Conversation): Unit ={
    val q = this.filter(_.chatId === conversation.chatId).update(conversation)
    SafeBot.db.run(q)
  }
}
