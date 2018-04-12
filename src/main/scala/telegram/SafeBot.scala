
package telegram
import java.util.UUID

import bot.BotResponse
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import config.oauth.OauthFactory
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import kie.{BotResponseEngine, MessageResponse}
import scala.io.Source
import net.liftweb.json._
import org.slf4j.{LoggerFactory, MDC}
import wit.WitAiProcessor
import scala.util.Random
object SafeBot extends TelegramBot with Polling with Commands {

  val conf: Config = ConfigFactory.load
  lazy val token: String = scala.util.Properties.envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromInputStream(getClass.getResourceAsStream("/bot.token")).getLines().mkString)
  implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
  val botResponses: Array[BotResponse] = loadResponses()
  val ignoredWords:Seq[String] = Seq("/start","/credentials")
  override val logger = Logger(LoggerFactory.getLogger(SafeBot.getClass))
  onCommand('start) { implicit msg => reply("Bienvenido, Mi nombre es Luky y soy un Bot de soporte tecnico, En que puedo ayudarte?!!!") }
  onCommand("credentials") {implicit msg=>reply(OauthFactory.name())}
  onMessage({implicit msg =>{
    MDC.put("UUID",UUID.randomUUID().toString)
    val name = msg.from.get.firstName
    val msgText:String = msg.text.mkString
    if(null!=msg.text && !ignoredWords.contains(msgText) && !msgText.equals("")) {
      logger.info(s"Get intent for: $msgText")
      val witResponse = WitAiProcessor.getIntents(msgText)
      logger.debug(s"$witResponse")
      val intent = witResponse.entities.get("intent")
      if(intent!=null && intent.isDefined && intent.get!=null){
        intent.get.foreach(w=>{
          BotResponseEngine.determineBotResponse(MessageResponse(w.value, witResponse.entities.filterKeys(!_.equals("intent"))),msg.chat.id.toString)
          val answers = botResponses.filter(p=>p.tag==w.value)
          if(answers.length>0){
            val message:String = getRandomElement(answers(0).responses, new Random(System.currentTimeMillis())).replace("{name}",name)
            reply(message)
          }else{
            reply("No training for your request")
          }
        })
      }else reply("Lo lamento no puedo entenderte")
    }
    MDC.remove("UUID")
    }
  })

  def loadResponses():Array[BotResponse] = {
    val stream = getClass.getResourceAsStream("/intents.json")
    val jsonString:String = Source.fromInputStream(stream).getLines
        .mkString
    parse(jsonString).extract[Array[BotResponse]]
  }

  def getRandomElement(list: Seq[String], random: Random): String = list(random.nextInt(list.length))

}
