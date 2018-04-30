package repository.model

import java.sql.Timestamp

case class IssueNotClassified (issueId:Long,summary:String,createdDate:Timestamp,chatId:Long)

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

class IssueNotClassifiedTable(tag: Tag) extends Table[IssueNotClassified](tag,"issue_not_classified"){
  def issueId = column[Long]("issue_id",O.PrimaryKey,O.AutoInc)
  def summary= column[String]("issue_summary")
  def createdDate = column[Timestamp]("created_date")
  def chatId = column[Long]("chat_id")
  override def * : ProvenShape[IssueNotClassified] = (issueId,summary,createdDate,chatId) <> (IssueNotClassified.tupled,IssueNotClassified.unapply)
}