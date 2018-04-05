package bot
import telegram.SafeBot


object ContextTelegramBot extends AppStart with App {
  println(start_msg)
  SafeBot.run();
}

trait AppStart {
  lazy val start_msg: String = "App started 1.0.0"
}
