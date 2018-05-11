package bot
import java.security.cert.X509Certificate

import config.oauth.OauthFactory
import javax.net.ssl.{HostnameVerifier, SSLSession, X509TrustManager}
import org.slf4j.LoggerFactory
import telegram.SafeBot

/**
  * @author Victor de la Cruz
  * @version 1.0.0
  * Main class
  * */
object ContextTelegramBot extends AppStart with App {
  val logger = LoggerFactory.getLogger(ContextTelegramBot.getClass)
  logger.info(start_msg)
  //System.setProperty("javax.net.ssl.trustStore","/usr/lib/jvm/jdk1.8.0_161/jre/lib/security/ca-certs")
  OauthFactory.credentials()
  SafeBot.run()
}

trait AppStart {
  lazy val start_msg: String = "Support Ctx Bot 1.0.0"
}
object TrustAll extends X509TrustManager {
  val getAcceptedIssuers = null

  def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) = {}

  def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) = {}
}

// Verifies all host names by simply returning true.
object VerifiesAllHostNames extends HostnameVerifier {
  def verify(s: String, sslSession: SSLSession) = true
}