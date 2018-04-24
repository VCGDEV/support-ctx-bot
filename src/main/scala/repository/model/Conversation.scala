package repository.model

import java.sql.Date
//import java.util.Date

import slick.lifted.Tag


case class Conversation(chatId:Int,currentContext:String,sendToNlpNext:Boolean,createdDate:Date)


import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
class ConversationTable(tag:Tag) extends Table[Conversation](tag,"conversation_context"){
  def chatId = column[Int]("chatId",O.PrimaryKey)
  def currentContext= column[String]("current_context")
  def sendToNlpNext = column[Boolean]("send_to_nlp_next")
  def createdDate = column[Date]("created_date")
  override def * : ProvenShape[Conversation] = (chatId,currentContext,sendToNlpNext,createdDate) <> (Conversation.tupled,Conversation.unapply)
  val conversations = TableQuery[ConversationTable]
}
