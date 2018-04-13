package google

import java.net.URL

import akka.util.ByteString
import com.stackmob.newman.ApacheHttpClient
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.binary.Base64
import com.stackmob.newman.dsl.{POST, _}
import com.stackmob.newman.response.HttpResponseCode
import com.typesafe.scalalogging.Logger
import net.liftweb.json.{DefaultFormats, parse}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
object GoogleSpeechRecognition {
  //load configuration from application.properties in src/main/resources
  val conf: Config = ConfigFactory.load
  lazy val googleApiKey:String = conf.getString("application.google.token")
  val logger = Logger(LoggerFactory.getLogger(GoogleSpeechRecognition.getClass))
  implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats

  def recognizeSpeech(file:ByteString): String ={
    logger.info("Send request to google and recognize speech to text")
    val url:String = s"https://speech.googleapis.com/v1/speech:recognize?alt=json&key=$googleApiKey"
    val request:RecognitionRequest = new RecognitionRequest(new RecognitionConfig("OGG_OPUS","es-MX",16000),new RecognitionAudio(Base64.encodeBase64String(file.toArray)))
    implicit val httpClient: ApacheHttpClient = new ApacheHttpClient()()
    val response = Await.result(POST(new URL(url)).setBody(request).apply,10.second)
    var transcript: String = ""
    if(response.code ==  HttpResponseCode.Ok) {
      val results: Map[String, List[GoogleSpeechResult]] = parse(response.bodyString).extract[Map[String, List[GoogleSpeechResult]]]
      results.get("results") match {
        case Some(results) => results.foreach(v => transcript += v.alternatives(0).transcript)
        case None => logger.warn("No results for voice record")
      }
    }
    transcript
  }
}
