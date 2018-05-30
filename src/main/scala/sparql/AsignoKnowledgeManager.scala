package sparql

import org.w3.banana._
import binder._
import java.net.URL

import org.w3.banana.jena.Jena

import scala.util.Try

class AsignoKnowledgeManager[Rdf <: RDF](implicit
                                ops: RDFOps[Rdf],
                                sparqlOps: SparqlOps[Rdf],
                                sparqlHttp: SparqlEngine[Rdf, Try, URL],
                                recordBinder: RecordBinder[Rdf]
                               ) {
  import ops._
  import recordBinder._
  import sparqlOps._
  import sparqlHttp.sparqlEngineSyntax._
  val SPARQLEndpoint = new URL("http://localhost:8890/sparql")
  val asigno = AsignoOntologyPrefix[Rdf]
  val defaultPrefixes =
    """
      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      PREFIX asn: <http://www.apps.inbyte.mx/vcruz/ontologies/2018/4/asigno#>
      PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    """

  case class User(name: String, email:String, id:String){
  }

  object User {
    val clazz = asigno.Person
    implicit val classUris = classUrisFor[User](clazz)
    val name = property[String](asigno("name"))
    val email = property[String](asigno("email"))
    val id = property[String](asigno("id"))
    implicit val binder = pgb[User](name, email, id)(User.apply, User.unapply)
  }
  //make querys to sparql endpoint
  def query(columns:String,where:String):List[User] = {
    val query = parseSelect(s"$defaultPrefixes SELECT $columns WHERE {$where}").get
    SPARQLEndpoint.executeSelect(query).get.iterator.map(row=>
      User(
        row("name").get.as[Rdf#Literal].get.lexicalForm,
        row("email").get.as[Rdf#Literal].get.lexicalForm,
        row("id").get.as[Rdf#Literal].get.lexicalForm
      )
    ).toList
  }


  //this has to be the only harcoded query, construct graph after this and navigate according to user issue
  def getUser(id:String):Option[User] = {
    val query = parseConstruct(s"$defaultPrefixes CONSTRUCT {" +
      "?individual ?p ?o " +
      "} WHERE {"  +
      "?type rdfs:subClassOf asn:User. "+
      "?individual rdf:type ?type. " +
      "?individual asn:id ?id." +
      "?individual ?p ?o .}").get
    val resultGraph = SPARQLEndpoint.executeConstruct(query).get
    //extrat data from graph using asigno IRI
    val users:List[User]=resultGraph.triples.collect{
      case Triple(user,rdf.`type`,asigno.Customer) =>
        val pg = PointedGraph(user, resultGraph)
        pg.as[User].toOption
      case Triple(user,rdf.typ,asigno.Technician)=>
        val pg = PointedGraph(user,resultGraph)
        pg.as[User].toOption
    }.flatten.toList
    users.find(u=>u.id.equals(id))
  }
}

object AsignoKnowledgeManagerImpl extends AsignoKnowledgeManager[Jena]
