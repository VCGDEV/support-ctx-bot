
package telegram
import bot.{BotResponse, WitResponse}
import com.stackmob.newman._
import com.stackmob.newman.dsl._
import com.typesafe.config.ConfigFactory

import scala.concurrent._
import scala.concurrent.duration._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import org.apache.http.client.utils.URIBuilder

import scala.io.Source
import net.liftweb.json._

import scala.util.Random

object SafeBot extends TelegramBot with Polling with Commands {

  val conf = ConfigFactory.load
  lazy val token = scala.util.Properties.envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromInputStream(getClass.getResourceAsStream("/bot.token")).getLines().mkString)
  lazy val witToken = conf.getString("wit.ai.token")
  lazy val witId = conf.getString("wit.ai.id")
  lazy val witUrl = conf.getString("wit.ai.url")
  lazy val witVersion = conf.getString("wit.ai.version")
  implicit val formats = net.liftweb.json.DefaultFormats
  val botResponses = loadResponses
  val ignoredWords:Seq[String] = Seq("/start")
  onCommand('start) { implicit msg => reply("Bienvenido!!!") }

  onMessage({implicit msg =>{
    val name = msg.from.get.firstName
    if(null!=msg.text && !ignoredWords.contains(msg.text.mkString)) {
      val msgText:String = msg.text.mkString;
      println(s"Get intent for: ${msgText}")
      val witResponse = getIntents(msgText)
      println(witResponse)
      val intent = witResponse.entities.get("intent")
      if(intent!=null && intent!=None && intent.get!=null){
        intent.get.foreach(w=>{
          val answers = botResponses.filter(p=>p.tag==w.value)
          if(answers.size>0){
            val message:String = getRandomElement(answers(0).responses, new Random(System.currentTimeMillis())).replace("{name}",name)
            reply(message)
          }else{
            reply("No training for your request")
          }
        })
      }else reply("Lo lamento no puedo entenderte")
    }
    }
  })

  def loadResponses():Array[BotResponse] = {
    val stream = getClass.getResourceAsStream("/intents.json")
    val jsonString:String = Source.fromInputStream(stream).getLines
        .mkString
    parse(jsonString).extract[Array[BotResponse]]
  }

  def getIntents(msgText:String): WitResponse ={
    implicit val httpClient = new ApacheHttpClient()
    val uri = new URIBuilder(witUrl).addParameter("v", s"${witVersion}").addParameter("q", s"${msgText}")
    val response = Await.result(GET(uri.build().toURL)
      .addHeaders("Authorization" -> witToken)
      .addHeaders("Accept" -> "application/json").apply, 5.second) //this will throw if the response doesn't return within 1 second
    parse(response.bodyString).extract[WitResponse]
  }

  def getRandomElement(list: Seq[String], random: Random): String = list(random.nextInt(list.length))

}
