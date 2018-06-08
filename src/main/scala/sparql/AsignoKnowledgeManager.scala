package sparql

import org.w3.banana._
import binder._
import java.net.URL

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import org.w3.banana.jena.Jena
import sparql.entities._

import scala.util.{Failure, Random, Success, Try}

trait OntologyKnowledge{
  val defaultPrefixes ="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
  val asignoURI = "PREFIX asn: <http://www.apps.inbyte.mx/vcruz/ontologies/2018/4/asigno#>"
  val issueURI = "PREFIX  iss:  <http://www.inbyte.semantic/lucy/ontologies/2018/5/issues#>"
  val intentURI = "PREFIX ipx: <http://www.inbyte.semantic/lucy/ontologies/2018/5/knowledge#>"
  lazy val config = ConfigFactory.load
  val SPARQLEndpoint = new URL(config.getString("sparql.config.url"))//query to this endpoint

  /**
    * Take a random element from possible responses array
    *  @param list a sequence of posible arrays
    *  @return message response
    * */
  def getRandomElement(list: Set[RDF#URI]): RDF#URI= {
    val random = new Random(System.currentTimeMillis())
    val index = random.nextInt(list.size)
    list.toList(index)
  }
}

class AsignoKnowledgeManager[Rdf <: RDF](implicit
                                ops: RDFOps[Rdf],
                                sparqlOps: SparqlOps[Rdf],
                                sparqlHttp: SparqlEngine[Rdf, Try, URL],
                                recordBinder: RecordBinder[Rdf]
                               ) extends OntologyKnowledge {
  import ops._
  import recordBinder._
  import sparqlOps._
  import sparqlHttp.sparqlEngineSyntax._
  val logger = Logger(LoggerFactory.getLogger("AsignoKnowledgeManager"))
  val asigno = AsignoOntologyPrefix[Rdf]
  val issuePrefix = IssueOntologyPrefix[Rdf]
  val intentPrefix = IntentPrefix[Rdf]
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

  //TODO move this classes to another files ---->
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

  case class Intent(intentType:String,value:String,hasAnswer:Set[Rdf#URI]){
    def toObject():Intention = Intention(intentType,value,hasAnswer.toSet)
  }

  object Intent{
    val clazz = intentPrefix.Intent
    val intentType = property[String](intentPrefix.intentType)
    val value = property[String](intentPrefix.value)
    val hasAnswer = set[Rdf#URI](intentPrefix.hasAnswer)
    implicit val binder = pgb[Intent](intentType,value,hasAnswer)(Intent.apply,Intent.unapply)
  }

  case class Answer(value:String){
    def toObject():OntologyAnswer = OntologyAnswer(value)
  }

  object Answer{
    val clazz = intentPrefix.Answer
    val value = property[String](intentPrefix.value)
    implicit val binder = pgb[Answer](value)(Answer.apply,Answer.unapply)
  }

  case class UserDO(name:String, email:String, id:String, officePhone:Option[String],mobilePhone:Option[String],
                    hasAddress:Option[Rdf#URI],isInCompany:Option[Rdf#URI],hasProperty:Set[Rdf#URI]){
    def toObject:User = User(name,email,id,officePhone.getOrElse(""),mobilePhone.getOrElse(""),hasAddress,isInCompany,hasProperty.toSet)
  }
  object UserDO{
    implicit val classUris = classUrisFor[UserDO](asigno.User)
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

  case class CompanyDO(name:String,entityType:String,hasAddress:Option[Rdf#URI],hasProperty:Set[Rdf#URI])

  object CompanyDO{
    implicit  val classUris = classUrisFor[CompanyDO](asigno.Company)
    val name = property[String](asigno.name)
    val entityType = property[String](asigno.entityType)
    val hasAddress = optional[Rdf#URI](asigno.hasAddress)
    val hasProperty = set[Rdf#URI](asigno.hasProperty)
    implicit val binder = pgb[CompanyDO](name,entityType,hasAddress,hasProperty)(CompanyDO.apply,CompanyDO.unapply)
  }

  case class AddressDO(government:String,postalCode:String,country:String,city:String,numInt:Option[String],
                     adjacent1:Option[String],adjacent2:Option[String],
                     numExt:Option[String],street:String)

  object AddressDO{
    implicit val classUris = classUrisFor[AddressDO](asigno.Address)
    val government = property[String](asigno.government)
    val postalCode = property[String](asigno.postalCode)
    val country = property[String](asigno.country)
    val city = property[String](asigno.city)
    val numInt = optional[String](asigno.numInt)
    val adjacent1 = optional[String](asigno.adjacentStreet1)
    val adjacent2 = optional[String](asigno.adjacentStreet2)
    val numExt = optional[String](asigno.numExt)
    val street = property[String](asigno.street)
    implicit val binder = pgb[AddressDO](government,postalCode,country,city,numInt,adjacent1,adjacent2,numExt,street)(AddressDO.apply,AddressDO.unapply)
  }

  //generic entity
  case class PropertyDO(propertyType:String,value:String,action:Option[String],hasProperty:Set[Rdf#URI]){
    def toObject():NodeProperty = NodeProperty(propertyType,value,action,hasProperty.toSet)
  }

  object PropertyDO{
    implicit val classUris = classUrisFor[PropertyDO](asigno.Property)
    val propertyType = property[String](asigno.action)
    val value = property[String](asigno.value)
    val action = optional[String](asigno.action)
    val hasProperty = set[Rdf#URI](asigno.hasProperty)
    implicit val binder = pgb[PropertyDO](propertyType,value,action,hasProperty)(PropertyDO.apply,PropertyDO.unapply)
  }

  def getPropertyByUri(propertyURI:String):Option[entities.NodeProperty]= {
    val query = parseConstruct(s"$defaultPrefixes $asignoURI CONSTRUCT {" +
      s"<$propertyURI> ?predicate ?object " +
      s"} WHERE {<$propertyURI> ?predicate ?object}").get
    val graph = SPARQLEndpoint.executeConstruct(query).get
    val properties:Iterable[entities.NodeProperty] = graph.triples.collect{
      case Triple(answer,rdf.`type`,intentPrefix.Answer)=>
        PointedGraph(answer,graph).as[PropertyDO].get.toObject
    }
    properties.headOption
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
    }.headOption
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
        PointedGraph(intent,resultGrap).as[Intent].get.toObject
    }.headOption
  }

  def getAnswer(iri:String):Option[OntologyAnswer] = {
    val query = parseConstruct(s"$defaultPrefixes $intentURI CONSTRUCT {" +
      s"<$iri> ?predicate ?object " +
      s"} WHERE {<$iri> ?predicate ?object}").get
    val graph = SPARQLEndpoint.executeConstruct(query).get
    graph.triples.collect{
      case Triple(answer,rdf.`type`,intentPrefix.Answer)=>
        PointedGraph(answer,graph).as[Answer].get.toObject
    }.headOption
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
        PointedGraph(user, resultGraph).as[UserDO].get.toObject
      case Triple(user,rdf.`type`,asigno.Person)=>
        PointedGraph(user,resultGraph).as[UserDO].get.toObject
    }.headOption
  }
}

object AsignoKnowledgeManagerImpl extends AsignoKnowledgeManager[Jena]