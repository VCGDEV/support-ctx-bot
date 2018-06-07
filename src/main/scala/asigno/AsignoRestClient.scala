package asigno

import java.net.URL

import com.stackmob.newman.ApacheHttpClient
import com.stackmob.newman.dsl.{GET, _}
import com.stackmob.newman.response.HttpResponseCode
import com.typesafe.config.{Config, ConfigFactory}
import config.oauth.OauthFactory
import net.liftweb.json.{DefaultFormats, MappingException, parse}
import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
object AsignoRestClient {
  val conf: Config = ConfigFactory.load
  lazy val urlEndPoint = conf.getString("application.rest.base.url")
  val logger = LoggerFactory.getLogger(AsignoRestClient.getClass)
  implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
  implicit val httpClient: ApacheHttpClient = new ApacheHttpClient()
  def getCustomerByEmail(email:String):CustomerView = {
    try {
      logger.info(s"Obtener datos de cliente en asigno ${email}")
      val uri = new URIBuilder(s"$urlEndPoint/customers/findByEmail").addParameter("email", s"$email")
      val response = Await.result(GET(uri.build().toURL)
        .addHeaders("Authorization" -> s" ${OauthFactory.credentials().token_type} ${OauthFactory.credentials().access_token}")
        .addHeaders("Accept" -> "application/json").apply, 15.second) //this will throw if the response doesn't return within 5 second
      logger.debug(s"Respuesta de servidor ${response.bodyString}")
      parse(response.bodyString).extract[CustomerView]
    }catch {
      case m: MappingException =>
        logger.error("No se recibio contenido JSON del servidor")
        null
      case e: Exception =>
        logger.error("Error al aseleccionar cliente",e)
        null
    }
  }

  def createTicket(ticket: Ticket): String = {
    logger.info(s"Crear nuevo Ticket $ticket")
    val uri = s"$urlEndPoint/tickets"
    val response = Await.result(
      POST(new URL(uri))
        .setBody(ticket)
        .addHeaders("Authorization" -> s"${OauthFactory.credentials().token_type} ${OauthFactory.credentials().access_token}")
        .addHeaders("Content-Type"->"application/json")
        .apply
      ,20.second)
    logger.info(s"Response code: ${response.code} - body ${response.bodyString}")
    if(response.code == HttpResponseCode.Ok || response.code == HttpResponseCode.Created)
      response.bodyString
    else
      ""
  }

  def sendComment(ticketId:String,commentView: CommentView):Boolean = {
    logger.info(s"Agregar comentario a Ticket")
    val uri = s"$urlEndPoint/tickets/$ticketId/comments"
    val response = Await.result(
    POST(new URL(uri))
    .setBody(commentView)
    .addHeaders("Authorization" -> s"${OauthFactory.credentials().token_type} ${OauthFactory.credentials().access_token}")
    .addHeaders("Content-Type"->"application/json")
    .apply
    ,20.second)
    logger.info(s"Response code: ${response.code} - body ${response.bodyString}")
    response.code == HttpResponseCode.Ok
  }
}
