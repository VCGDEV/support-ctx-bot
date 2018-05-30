package config.oauth

import com.stackmob.newman.ApacheHttpClient
import com.stackmob.newman.dsl.{POST, _}
import com.stackmob.newman.response.HttpResponseCode
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import net.liftweb.json._
import org.apache.http.client.utils.URIBuilder

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Class to maintain in memory user details from oauth login
  * */
class OauthCredentials(var access_token:String,var refresh_token:String,var token_type:String, var name:String) {

}

/**
  *  @author Victor de la Cruz
  *  @version 1.0.0
  *  Object definition to send request at security microservice and make login, using oauth server
  * */
object OauthFactory{
  val conf: Config = ConfigFactory.load
  lazy val username:String = conf.getString("application.username")
  lazy val password:String = conf.getString("application.password")
  lazy val clientId:String = conf.getString("application.oauth.client-id")
  lazy val clientSecret:String = conf.getString("application.oauth.client-secret")
  lazy val oauthServerUrl:String = conf.getString("application.oauth.url")
  lazy val oauthGrantType:String = conf.getString("application.oauth.grant")
  private var _credentials : OauthCredentials = null
  val logger = Logger(OauthFactory.getClass)

  /**
    * Method to send a request to <strong>security</strong> microservice and login into app
    * @return OauthCredentials from server response
    * */
  def credentials():OauthCredentials = {
    if(_credentials==null){
      logger.debug("Send request to security service")
      implicit val httpClient: ApacheHttpClient = new ApacheHttpClient()
      val uri = new URIBuilder(oauthServerUrl)
        .addParameter("username",username)
        .addParameter("password",password)
        .addParameter("client_id",clientId)
        .addParameter("client_secret",clientSecret)
        .addParameter("grant_type",oauthGrantType)
      val response = Await.result(POST(uri.build().toURL)
          .addHeaders("Content-Type"->"application/x-www-form-urlencoded")
        .apply,5.second)
      implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
      logger.debug(s"Response from URL ${uri.toString}  was: ${response.code} - content: ${response.bodyString}")
      if(response.code==HttpResponseCode.Ok){
        logger.debug("Success login to security service")
        _credentials = parse(response.bodyString).extract[OauthCredentials]
      }
      else
        logger.error("Login was not successful")
    }
    _credentials
  }

  def name():String = {
    if(_credentials==null)
      return "No credentials"
    _credentials.name
  }
}
