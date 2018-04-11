package bot
import config.oauth.OauthFactory
import telegram.SafeBot


object ContextTelegramBot extends AppStart with App {
  println(start_msg)
  println("Getting credentials")
  OauthFactory.credentials()
  SafeBot.run();
}

trait AppStart {
  lazy val start_msg: String = "App started 1.0.0"
}
