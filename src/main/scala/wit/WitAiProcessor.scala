package wit
import com.stackmob.newman.ApacheHttpClient
import com.stackmob.newman.dsl.{GET, _}
import com.typesafe.config.{Config, ConfigFactory}
import net.liftweb.json.{DefaultFormats, parse}
import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
/**
  * @author Victor de la Cruz Gonzalez
  * @version 1.0.0
  * Class to GET intents of user using, request to <strong>https://wit.ai</strong>
  * */
object WitAiProcessor {
  //load configuration from application.properties in src/main/resources
  val conf: Config = ConfigFactory.load
  //token to access wit.ai
  lazy val witToken: String = conf.getString("wit.ai.token")
  //ID of project in wit.ai
  lazy val witId: String = conf.getString("wit.ai.id")
  //URL to make request
  lazy val witUrl: String = conf.getString("wit.ai.url")
  //Version of app, required for request
  lazy val witVersion: String = conf.getString("wit.ai.version")
  //default json format parser
  implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats

  val logger = LoggerFactory.getLogger(WitAiProcessor.getClass)
  /**
    * Method to send request at <strong>https://wit.ai<strong>, using configurations from application.properties
    * a timeout is throw if the request exceeds 5 seconds
    * @param msgText text to be send as request
    * @return WitResponse with entities and intent
    * */
  def getIntents(msgText:String): WitResponse ={
    logger.info(s"Send request to :${witUrl} -  message: $msgText")
    implicit val httpClient: ApacheHttpClient = new ApacheHttpClient()
    val uri = new URIBuilder(witUrl).addParameter("v", s"$witVersion").addParameter("q", s"$msgText")
    val response = Await.result(GET(uri.build().toURL)
      .addHeaders("Authorization" -> witToken)
      .addHeaders("Accept" -> "application/json").apply, 20.second) //this will throw if the response doesn't return within 5 second
    parse(response.bodyString).extract[WitResponse]
  }
}
