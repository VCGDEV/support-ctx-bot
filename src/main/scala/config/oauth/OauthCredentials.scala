package config.oauth

import com.stackmob.newman.ApacheHttpClient

import scala.concurrent.duration._
import com.stackmob.newman.dsl.POST
import com.typesafe.config.{Config, ConfigFactory}
import com.stackmob.newman.dsl._
import com.stackmob.newman.response.HttpResponseCode
import org.apache.http.client.utils.URIBuilder
import net.liftweb.json._

import scala.concurrent.Await

class OauthCredentials(var access_token:String,var refresh_token:String,var token_type:String, var name:String) {

}

object OauthFactory{
  val conf: Config = ConfigFactory.load
  lazy val username:String = conf.getString("application.username")
  lazy val password:String = conf.getString("application.password")
  lazy val clientId:String = conf.getString("application.oauth.client-id")
  lazy val clientSecret:String = conf.getString("application.oauth.client-secret")
  lazy val oauthServerUrl:String = conf.getString("application.oauth.url")
  lazy val oauthGrantType:String = conf.getString("application.oauth.grant")
  private var _credentials : OauthCredentials = null

  def credentials():OauthCredentials = {
    if(_credentials==null){
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
      if(response.code==HttpResponseCode.Ok)
        _credentials = parse(response.bodyString).extract[OauthCredentials]
    }
    _credentials
  }

  def name():String = {
    if(_credentials==null)
      return "No credentials"
    _credentials.name
  }
}
