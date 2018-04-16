package kie

import org.kie.api.KieServices
import org.kie.api.runtime.{KieContainer, KieSession, StatelessKieSession}

/**
  *  Class to get access to the rules engine
  *  for full documentation go to: <strong>http://bleibinha.us/blog/2014/04/drools-with-scala</strong>
  * */
object Kie {
 private lazy val kieServices:KieServices = KieServices.Factory.get()
 private lazy val kieContainer:KieContainer = kieServices.getKieClasspathContainer

 def newStatelessSession:StatelessKieSession = kieContainer.newStatelessKieSession()

 def executeStateless(facts:List[Any]): Unit = newStatelessSession.execute(facts)

 def newSession: KieSession = kieContainer.newKieSession()

 def getStatelessSession(sessionId:String):StatelessKieSession = {
  kieContainer.newStatelessKieSession(sessionId)
 }
}
