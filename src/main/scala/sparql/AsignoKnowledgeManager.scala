package sparql

import org.w3.banana._
import binder._
import java.net.URL

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import org.w3.banana.jena.{Jena, JenaModule}

import scala.util.{Failure, Random, Success, Try}

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
  val logger = Logger(LoggerFactory.getLogger("AsignoKnowledgeManager"))
  val asigno = AsignoOntologyPrefix[Rdf]
  val issuePrefix = IssueOntologyPrefix[Rdf]
  val intentPrefix = IntentPrefix[Rdf]
  val defaultPrefixes =
    """
      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    """
  val asignoURI = "PREFIX asn: <http://www.apps.inbyte.mx/vcruz/ontologies/2018/4/asigno#>"
  val issueURI = "PREFIX  iss:  <http://www.inbyte.semantic/lucy/ontologies/2018/5/issues#>"
  val intentURI = "PREFIX ipx: <http://www.inbyte.semantic/lucy/ontologies/2018/5/knowledge#>"
  case class User(name: String, email:String, id:String,
                  hasPC:Option[Rdf#URI]) {
  }

  object LongBinder{
    implicit val LongToLiteral = new ToLiteral[Rdf,Long] {
      def toLiteral(t: Long): Rdf#Literal = Literal(t.toString,xsd.long)
    }

    implicit val LongFromLiteral = new FromLiteral[Rdf,Long] {
      def fromLiteral(literal: Rdf#Literal): Try[Long] = {
        val Literal(lexicalForm,datatype,_) = literal
        if(datatype == xsd.long){
          try{
            Success(lexicalForm.toLong)
          }catch {
            case _:IllegalArgumentException=>Failure(FailedConversion(s"${literal} is an xsd.long but is not an acceptable long"))
          }
        }else{
          Failure(FailedConversion(s"${literal} is not an xsd.long"))
        }
      }
    }
  }

  object User {
    val clazz = asigno.Person
    implicit val classUris = classUrisFor[User](clazz)
    val name = property[String](asigno("name"))
    val email = property[String](asigno("email"))
    val id = property[String](asigno("id"))
    val hasPC = optional[Rdf#URI](asigno("hasPC"))
    implicit val binder = pgb[User](name, email, id,hasPC)(User.apply, User.unapply)
  }

  case class PC(idAddress:String,hasSoftware:Set[Rdf#URI],hasPrinter:Set[Rdf#URI])
  object PC{
    val clazz = asigno.Pc
    val ip = property[String](asigno.ipAddress)
    val hasSoftware = set[Rdf#URI](asigno.hasSoftware)
    val hasPrinter = set[Rdf#URI](asigno.hasPrinter)
    implicit val binder = pgb[PC](ip,hasSoftware,hasPrinter)(PC.apply, PC.unapply)
  }

  case class Category(categoryId:Long,devCategoryId:Long,value:String,subcategoryId:Long,
                      intent:String,name:String,devSubcategoryId:Long)

  object Category{
    val clazz = issuePrefix.Category


    val value = property[String](issuePrefix.value)
    val intent = property[String](issuePrefix.intent)
    val name = property[String](issuePrefix.name)
    import LongBinder._
    val subcategoryId = property[Long](issuePrefix.subcategoryId)
    val devCategoryId = property[Long](issuePrefix.devCategoryId)
    val categoryId = property[Long](issuePrefix.categoryId)
    val devSubcategoryId = property[Long](issuePrefix.devSubcategoryId)
    implicit val binder = pgb[Category](categoryId,devCategoryId,value,
      subcategoryId,intent,name,devSubcategoryId)(Category.apply,Category.unapply)
  }

  case class Intent(intentType:String,value:String,hasAnswer:Set[Rdf#URI])

  object Intent{
    val clazz = intentPrefix.Intent
    val intentType = property[String](intentPrefix.intentType)
    val value = property[String](intentPrefix.value)
    val hasAnswer = set[Rdf#URI](intentPrefix.hasAnswer)
    implicit val bincer = pgb[Intent](intentType,value,hasAnswer)(Intent.apply,Intent.unapply)
  }

  case class Answer(value:String)

  object Answer{
    val clazz = intentPrefix.Answer
    val value = property[String](intentPrefix.value)
    implicit val binder = pgb[Answer](value)(Answer.apply,Answer.unapply)
  }


  def getGraph(iri:String,fType: Rdf#URI) ={
    logger.info("URI search: {}",fType)
    val query = parseConstruct(s"$defaultPrefixes $asignoURI CONSTRUCT {" +
      s"<${iri}> ?predicate ?object " +
      s"} WHERE {<${iri}> ?predicate ?object}").get
    val graph = SPARQLEndpoint.executeConstruct(query).get
    graph.triples.collect{
      case Triple(r,rdf.typ,s)=>
        PointedGraph(r,graph)
    }
    //logger.info("Selected: \n{}",node)
  }

  def getCategory(intent:String):Option[Category] = {
    val query = parseConstruct(s"$defaultPrefixes $issueURI CONSTRUCT {" +
      s"?individual ?p ?o} WHERE {" +
      s"?individual rdf:type iss:Category." +
      s"?individual iss:intent ?intent." +
      s"FILTER(?intent='$intent'^^xsd:string)." +
      s"?individual ?p ?o" +
      s"}").get
    val resultGraph = SPARQLEndpoint.executeConstruct(query).get
    val categories:List[Category] = resultGraph.triples.collect{
      case Triple(category,rdf.`type`,issuePrefix.Category)=>
        val pg = PointedGraph(category,resultGraph)
        pg.as[Category].toOption
    }.flatten.toList
    categories.find(c=>c.intent.equals(intent))
  }

  def searchIntent(intent: String):Option[Intent]={
    val query = parseConstruct(s"$defaultPrefixes $intentURI CONSTRUCT {" +
      s"?individual ?p ?o" +
      s"} WHERE {" +
      s"?individual rdf:type ipx:Intent." +
      s"?individual ipx:value ?value." +
      s"FILTER(?value='$intent'^^xsd:string)." +
      s"?individual ?p ?o" +
      s"}").get
    val resultGrap = SPARQLEndpoint.executeConstruct(query).get
    val intents = resultGrap.triples.collect{
      case Triple(intent,rdf.`type`,intentPrefix.Intent)=>
        val pg = PointedGraph(intent,resultGrap)
        logger.info(s"${pg}")
        pg.as[Intent].toOption
    }.flatten.toList
    intents.find(s=>s.value.equals(intent))
  }

  def getAnswer(iri:String):Option[Answer] = {
    val query = parseConstruct(s"$defaultPrefixes $intentPrefix CONSTRUCT {" +
      s"<$iri> ?predicate ?object " +
      s"} WHERE {<$iri> ?predicate ?object}").get
    val graph = SPARQLEndpoint.executeConstruct(query).get
    val answers:List[Answer] = graph.triples.collect{
      case Triple(answer,rdf.`type`,intentPrefix.Answer)=>
        PointedGraph(answer,graph).as[Answer].toOption
    }.flatten.toList
    if(answers.size>0)
      Some(answers.head)
    else
      None
  }

  /**
    * Take a random element from possible responses array
    *  @param list a sequence of posible arrays
    *  @param random Random object with configurations to obtain next position of array
    *  @return message response
    * */
  def getRandomElement(list: Set[Rdf#URI], random: Random)= list.take(random.nextInt(list.size))

  //this has to be the only harcoded query, construct graph after this and navigate according to user issue
  def getUser(id:String):Option[User] = {

    val query = parseConstruct(s"$defaultPrefixes $asignoURI CONSTRUCT {" +
      "?individual ?p ?o " +
      "} WHERE {"  +
      "?type rdfs:subClassOf asn:User. "+
      "?individual rdf:type ?type. " +
      "?individual asn:id ?id." +
      s"FILTER(?id='${id}'^^xsd:string)."+
      "?individual ?p ?o .}").get
    val resultGraph = SPARQLEndpoint.executeConstruct(query).get
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




