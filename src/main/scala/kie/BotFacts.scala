package kie
import java.sql.Timestamp
import java.util.Date

import asigno._
import bot.IntentClassification
import com.typesafe.scalalogging.Logger
import mail.MailService
import net.liftweb.json.DefaultFormats
import repository.model.{Conversation, IssueNotClassified, IssueNotClassifiedDao, OntologyContextDao}
import wit.WitIntent
import net.liftweb.json.Serialization.write
import org.slf4j.LoggerFactory
import sparql.AsignoKnowledgeManagerImpl
import sparql.entities._

import scala.beans.BeanInfo
sealed trait BotFact
class BotFacts {

}

case class Customer(isInAsigno:Boolean)
case class TicketCreated(isCreated:Boolean,folio:String)
case class CommentCreated(isCreated:Boolean)
@BeanInfo
case class ProcessIntention(user:User,intention:Intention,conversation:Conversation,
                            entities:Map[String,List[WitIntent]],message:String,chatId:Long){
  val logger = Logger(LoggerFactory.getLogger(ProcessIntention.getClass))
  var answer:String = ""
  def findAnswer() = {
    val rdfURI = AsignoKnowledgeManagerImpl.getRandomElement(intention.hasAnswer)
    answer = AsignoKnowledgeManagerImpl.getAnswer(rdfURI.toString).getOrElse(OntologyAnswer("")).value
  }

  def getCategory()={
    val category = AsignoKnowledgeManagerImpl.getCategory(intention.value)
    category match {
      case Some(c)=>answer = s"Dare de alta un ticket de tipo: ${c.name}"
      case None=>answer = "Aun no puedo procesar el problema que me presentas, solicitare capacitacion"
    }
  }

  def searchMainNode():Unit = {
    val ontologyContext = OntologyContextDao.findActiveContext(chatId);
    //here we have to search the node of graph where the magic starts
    ontologyContext match {
      case Some(o)=>answer = s"Current context ${o.mainNode} - ${o.currentNode}"
      case None=>
        val category = AsignoKnowledgeManagerImpl.getCategory(intention.value)
        category match {
          case Some(c)=>
            var nodes:List[NodeProperty] = List()
            user.hasProperty.foreach(h=>nodes = nodes ::: AsignoKnowledgeManagerImpl.hasProperty(h.toString,c.value))
            logger.info("{}",nodes)
            if(nodes.isEmpty) {
              logger.info("There are no main node in user Entity, searching in company")
              nodes = AsignoKnowledgeManagerImpl.hasProperty(user.isInCompany.get.toString, c.value)
              logger.info("{}",nodes)
              nodes.foreach(n=>answer = answer.concat(s"\n ${n.name}"))
            }
          case None=>logger.info("There are no category . bot is not trained to answer this")
        }
    }
  }

  def setAnswer(answer:String): Unit ={
    this.answer = answer
  }
}
/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Class definition to manage intent and the entities to process in rule engine
  * */
@BeanInfo
case class MessageResponse(var intent:String,var entities:Map[String,List[WitIntent]],var conversation:Conversation, val message:String,val chatId:Long,
                           val commentView: CommentView,val username:String) extends BotFact{
  var responseString:String = ""
  var classification:IntentClassification = null
  var customerView:CustomerView=null
  def setResponse(response:String) = this.responseString= response
  def setContext(context:String) = this.conversation.currentContext = context
  def context():String = this.conversation.currentContext
  def classifyConversation() = {
    this.conversation.summary = message
    if (classification != null) {
      this.conversation.category = this.classification.mainCategoryId
      this.conversation.subcategory = this.classification.categoryId
    }
  }

  def sendNextMessageToWit(send:Boolean) = this.conversation.sendToNlpNext = send
  def setSummary(summary:String) = this.conversation.summary = summary
  def setDescription() = this.conversation.description = this.message
  def containsEntity(entity:String) = this.entities.contains(entity)
  def setClassification(classification: IntentClassification) = this.classification = classification
  def cleanConversation() = {
    this.conversation.currentContext = ""
    this.conversation.summary = ""
    this.conversation.sendToNlpNext = true
    this.conversation.description = ""
    this.conversation.category = ""
    this.conversation.subcategory = ""
    this.conversation.customer = ""
    this.conversation.ticketId = ""
  }
  def findFirstEntityValue(key:String) : String = {
    if(this.entities.contains(key)){
      this.entities.get(key) match {
        case Some(list) =>
          list(0).value
        case None => ""
      }
    }else ""
  }
  def setCustomer(customerView: CustomerView) = this.customerView = customerView
  def findCustomer(email:String):CustomerView = {
    this.customerView = AsignoRestClient.getCustomerByEmail(email)
    this.customerView
  }

  def isCustomerNull():Boolean = {
    this.customerView == null
  }

  //2 productivo
  //28 pruebas
  def createTicket():String = {
    val ticket = Ticket(new Date().getTime,customerData(),List(),TicketType(28),
      TicketCategory(conversation.subcategory,conversation.category),conversation.summary,conversation.description)
    val ticketId = AsignoRestClient.createTicket(ticket)
    conversation.ticketId = ticketId
    ticketId
  }

  def customerData():CustomerData = {
    implicit val formats = DefaultFormats
    val values = List(
      CustomerInfo("idCustomer","Id de Cliente",this.customerView.idCustomer.toString),
      CustomerInfo("createdDate","Fecha de Creación",this.customerView.createdDate.toString),
      CustomerInfo("customerType","Tipo de Cliente",this.customerView.customerType),
      CustomerInfo("name","Nombre",this.customerView.name),
      CustomerInfo("paternalName","Apellido Paterno",this.customerView.paternalName),
      CustomerInfo("maternalName","Apellido Materno",this.customerView.maternalName),
      CustomerInfo("rfc","RFC",this.customerView.rfc),
      CustomerInfo("commercialName","Nombre Comercial",this.customerView.commercialName),
      CustomerInfo("customerStatus","Estatus",this.customerView.customerStatus)
    ):::
    this.customerView.addressList.zipWithIndex.map{case(element,index)=>CustomerInfo("address"+index,"Dirección",write(element))} :::
    this.customerView.numberList.zipWithIndex.map{case(element,index)=>CustomerInfo("phone"+index,"Teléfono "+element.phoneType,write(element))}
    //ADD address and phones to LIST
    CustomerData(values,this.customerView.idCustomer)
  }

  def saveNotClassified(): Unit ={
    val issue = IssueNotClassified(0,this.conversation.summary,new Timestamp(new Date().getTime),chatId)
    IssueNotClassifiedDao.save(issue)
  }

  def sendNotClassifiedMail(): Unit ={
    val mailService = new MailService
    mailService.sendMail(List("vcruz@amentum.net"),"Elemento sin clasificar",message)
  }

  def sendComment():Boolean = {
    AsignoRestClient.sendComment(conversation.ticketId,commentView)
  }

  def hasAttachments():Boolean = this.commentView.attachments.size>0
}