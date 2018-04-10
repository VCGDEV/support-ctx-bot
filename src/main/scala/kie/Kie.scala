package kie

import org.kie.api.KieServices
import org.kie.api.runtime.{KieContainer, KieSession, StatelessKieSession}

object Kie {
 private lazy val kieServices:KieServices = KieServices.Factory.get()
 private lazy val kieContainer:KieContainer = kieServices.getKieClasspathContainer

 def newStatelessSession:StatelessKieSession = kieContainer.newStatelessKieSession()

 def executeStateless(facts:List[Any]): Unit = newStatelessSession.execute(facts)

 def newSession: KieSession = kieContainer.newKieSession()
}
