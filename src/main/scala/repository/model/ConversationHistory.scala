package repository.model

import java.sql.Date

case class ConversationHistory(historyId:Long,intent:String,message:String,from:String,to:String,createdDate:Date,chatId:Long)

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
class ConversationHistoryTable(tag:Tag) extends Table[ConversationHistory](tag,"conversation_history"){
  def historyId = column[Long]("conversation_history_id",O.AutoInc,O.PrimaryKey)
  def intent= column[String]("conversation_intent")
  def message = column[String]("conversation_message")
  def from = column[String]("from")
  def to = column[String]("to")
  def createdDate = column[Date]("created_date")
  def chatId = column[Long]("chat_id")

  override def * : ProvenShape[ConversationHistory]  = (historyId,intent,message,from,to,createdDate,chatId) <> (ConversationHistory.tupled,ConversationHistory.unapply)
}
