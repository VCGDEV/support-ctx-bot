package bot
import com.typesafe.scalalogging.Logger
import config.oauth.OauthFactory
import telegram.SafeBot
/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Main class
  * */
object ContextTelegramBot extends AppStart with App {
  val logger = Logger(ContextTelegramBot.getClass)
  logger.info(start_msg)
  OauthFactory.credentials()
  SafeBot.run()
}

trait AppStart {
  lazy val start_msg: String = "Luky Bot 0.1.0 Created By VCG"
}