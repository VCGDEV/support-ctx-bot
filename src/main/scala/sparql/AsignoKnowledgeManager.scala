package sparql

import org.w3.banana._
import binder._
import java.net.URL

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import org.w3.banana.jena.Jena
import sparql.entities.{Intention, IssueCategory, OntologyAnswer, User}

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
  lazy val config = ConfigFactory.load
  val SPARQLEndpoint = new URL(config.getString("sparql.config.url"))//query to this endpoint
  val logger = Logger(LoggerFactory.getLogger("AsignoKnowledgeManager"))
  val asigno = AsignoOntologyPrefix[Rdf]
  val issuePrefix = IssueOntologyPrefix[Rdf]
  val intentPrefix = IntentPrefix[Rdf]
  val defaultPrefixes ="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
  val asignoURI = "PREFIX asn: <http://www.apps.inbyte.mx/vcruz/ontologies/2018/4/asigno#>"
  val issueURI = "PREFIX  iss:  <http://www.inbyte.semantic/lucy/ontologies/2018/5/issues#>"
  val intentURI = "PREFIX ipx: <http://www.inbyte.semantic/lucy/ontologies/2018/5/knowledge#>"

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

  case class UserDO(name:String, email:String, id:String, officePhone:Option[String],mobilePhone:Option[String],
                    hasAddress:Option[Rdf#URI],isInCompany:Option[Rdf#URI],hasProperty:Set[Rdf#URI])
  object UserDO{
    val clazz = asigno.User
    implicit val classUris = classUrisFor[UserDO](clazz)
    val name = property[String](asigno.name)
    val email = property[String](asigno.email)
    val id = property[String](asigno.id)
    val officePhone = optional[String](asigno.officePhone)
    val mobilePhone = optional[String](asigno.mobilePhone)
    val hasAddress = optional[Rdf#URI](asigno.hasAddress)
    val isInCompany = optional[Rdf#URI](asigno.isInCompany)
    val hasProperty = set[Rdf#URI](asigno.hasProperty)
    implicit val binder = pgb[UserDO](name, email, id,officePhone,mobilePhone,hasAddress,isInCompany,hasProperty)(UserDO.apply, UserDO.unapply)
  }

  def getCategory(intent:String):Option[IssueCategory] = {
    val query = parseConstruct(s"$defaultPrefixes $issueURI CONSTRUCT {" +
      s"?individual ?p ?o} WHERE {" +
      s"?individual rdf:type iss:Category." +
      s"?individual iss:intent ?intent." +
      s"FILTER(?intent='$intent'^^xsd:string)." +
      s"?individual ?p ?o" +
      s"}").get
    val resultGraph = SPARQLEndpoint.executeConstruct(query).get
    resultGraph.triples.collect{
      case Triple(category,rdf.`type`,issuePrefix.Category)=>
        val c = PointedGraph(category,resultGraph).as[Category].get
        IssueCategory(c.categoryId,c.devCategoryId,c.value,c.subcategoryId,
          c.intent,c.name,c.devSubcategoryId)
    }.find(c=>c.intent.equals(intent))
  }

  def searchIntent(intent: String):Option[Intention]={
    val query = parseConstruct(s"$defaultPrefixes $intentURI CONSTRUCT {" +
      s"?individual ?p ?o" +
      s"} WHERE {" +
      s"?individual rdf:type ipx:Intent." +
      s"?individual ipx:value ?value." +
      s"FILTER(?value='$intent'^^xsd:string)." +
      s"?individual ?p ?o" +
      s"}").get
    val resultGrap = SPARQLEndpoint.executeConstruct(query).get
    resultGrap.triples.collect{
      case Triple(intent,rdf.`type`,intentPrefix.Intent)=>
        val int_ = PointedGraph(intent,resultGrap).as[Intent].get
        Intention(int_.intentType,int_.value,int_.hasAnswer.toSet)
    }.find(p=>p.value.equals(intent))
  }

  def getAnswer(iri:String):Option[OntologyAnswer] = {
    val query = parseConstruct(s"$defaultPrefixes $intentURI CONSTRUCT {" +
      s"<$iri> ?predicate ?object " +
      s"} WHERE {<$iri> ?predicate ?object}").get
    val graph = SPARQLEndpoint.executeConstruct(query).get
    val answers:Iterable[OntologyAnswer] = graph.triples.collect{
      case Triple(answer,rdf.`type`,intentPrefix.Answer)=>
        val a_ = PointedGraph(answer,graph).as[Answer].get
        OntologyAnswer(a_.value)
    }
    if(answers.size>0)
      Some(answers.head)
    else
      None
  }

  /**
    * Take a random element from possible responses array
    *  @param list a sequence of posible arrays
    *  @return message response
    * */
  def getRandomElement(list: Set[RDF#URI]): RDF#URI= {
    val random = new Random(System.currentTimeMillis());
    val index = random.nextInt(list.size)
    list.toList(index)
  }

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
    resultGraph.triples.collect{
      case Triple(user,rdf.`type`,asigno.Employee) =>
        val pg = PointedGraph(user, resultGraph).as[UserDO].get
        User(pg.name,pg.email,pg.id,pg.officePhone.getOrElse(""),pg.mobilePhone.getOrElse(""),pg.hasAddress,pg.isInCompany,pg.hasProperty.toSet)
      case Triple(user,rdf.`type`,asigno.Person)=>
        val pg = PointedGraph(user,resultGraph).as[UserDO].get
        User(pg.name,pg.email,pg.id,pg.officePhone.getOrElse(""),pg.mobilePhone.getOrElse(""),pg.hasAddress,pg.isInCompany,pg.hasProperty.toSet)
    }.find(u=>u.id.equals(id))
  }
}

object AsignoKnowledgeManagerImpl extends AsignoKnowledgeManager[Jena]