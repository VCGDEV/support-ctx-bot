package bot
import com.typesafe.scalalogging.Logger
import config.oauth.OauthFactory
import org.slf4j.LoggerFactory
import telegram.SafeBot


object ContextTelegramBot extends AppStart with App {
  val logger = Logger(LoggerFactory.getLogger(ContextTelegramBot.getClass))
  logger.info(start_msg)
  //OauthFactory.credentials()
  SafeBot.run()
}

trait AppStart {
  lazy val start_msg: String = "App started 1.0.0"
}
