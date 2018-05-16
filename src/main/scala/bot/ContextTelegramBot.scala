package bot
import config.oauth.OauthFactory
import org.slf4j.LoggerFactory
import org.w3.banana.{RDF, RDFModule, RDFOpsModule, SparqlHttpModule, SparqlOpsModule}
import sparql.{AsignoKnowledgeManagerImpl}
import telegram.SafeBot
/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Main class
  * */
object ContextTelegramBot extends AppStart with App {
  val logger = LoggerFactory.getLogger(ContextTelegramBot.getClass)
  logger.info(start_msg)
  OauthFactory.credentials()
  SafeBot.run()
  AsignoKnowledgeManagerImpl.selectUsers().foreach(println)
}

trait AppStart {
  lazy val start_msg: String = "Support Ctx Bot 1.0.0"
}

trait SPARQLExampleDependencies
  extends RDFModule
    with RDFOpsModule
    with SparqlOpsModule
    with SparqlHttpModule