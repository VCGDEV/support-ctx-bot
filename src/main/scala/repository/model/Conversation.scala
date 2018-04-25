package repository.model

import java.sql.{Timestamp}
//import java.util.Date

import slick.lifted.Tag


case class Conversation(chatId:Long,var currentContext:String,var sendToNlpNext:Boolean,createdDate:Timestamp,
                        var description:String,var summary:String,var category:String,var subcategory:String,var customer:String)


import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
class ConversationTable(tag:Tag) extends Table[Conversation](tag,"conversation_context"){
  def chatId = column[Long]("chat_id",O.PrimaryKey)
  def currentContext= column[String]("current_context")
  def sendToNlpNext = column[Boolean]("send_to_nlp_next")
  def createdDate = column[Timestamp]("created_date")
  def description = column[String]("issue_description")
  def summary = column[String]("issue_summary")
  def category = column[String]("category")
  def subcategory = column[String]("subcategory")
  def customer = column[String]("customer")
  override def * : ProvenShape[Conversation] = (chatId,currentContext,sendToNlpNext,createdDate,description,summary,category,subcategory,customer) <> (Conversation.tupled,Conversation.unapply)
  override def toString(): String = s"chatId: ${chatId} - context: ${currentContext}"
}
