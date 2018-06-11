package sparql


import org.w3.banana.{RDF, RDFOps}

object AsignoOntologyPrefix {
  def apply[Rdf <: RDF](implicit ops: RDFOps[Rdf]) = new AsignoOntologyPrefix(ops)
}

class AsignoOntologyPrefix[Rdf <: RDF](ops: RDFOps[Rdf]) extends PrefixBuilder("asn","http://www.apps.inbyte.mx/vcruz/ontologies/2018/4/asigno#")(ops){
  //object properties
  val hasAddress = apply("hasAddress")
  val isInCategory = apply("isInCategory")
  val isInCompany = apply("isInCompany")
  val hasProperty = apply("hasProperty")
  //data properties
  val adjacentStreet1 = apply("adjacent_street1")
  val adjacentStreet2 = apply("adjacent_street_2")
  val city = apply("city")
  val country = apply("country")
  val email = apply("email")
  val expirationDate = apply("expiration_date")
  val full_name = apply("full_name")
  val government = apply("government")
  val id = apply("id")
  val installedDate = apply("installed_date")
  val localPhone = apply("local_phone")
  val mobilePhone = apply("mobile_phone")
  val name = apply("name")
  val numExt = apply("num_ext")
  val numInt = apply("num_int")
  val officePhone = apply("office_phone")
  val postalCode = apply("postal_code")
  val street = apply("street")
  val suburb = apply("suburb")
  //classes
  val Address = apply("Address")
  val Category = apply("Category")
  val Company = apply("company")
  val Employee = apply("Employee")
  val Person = apply("Person")
  val Software = apply("Software")
  val User = apply("User")
  val Property = apply("Property")
  val value = apply("value")
  val entityType = apply("type")
  val action = apply("action")
}

class IssuePrefix[Rdf <: RDF](ops: RDFOps[Rdf]) extends PrefixBuilder("iss","http://www.inbyte.semantic/lucy/ontologies/2018/5/issues#")(ops){
  val BusinessFlow = apply("BusinessFlow")
  val Category = apply("Category")
  val Issue = apply("Issue")
  val hasCategory = apply("hasCategory")
  val hasIssue = apply("hasIssue")
  val categoryId = apply("categoryId")
  val devCategoryId = apply("devCategoryId")
  val devSubcategoryId = apply("devSubcategoryId")
  val intent = apply("intent")
  val name = apply("name")
  val subcategoryId = apply("subcategoryId")
  val value = apply("value")
  val article = apply("article")
}

object IssueOntologyPrefix{
  def apply[Rdf <: RDF](implicit ops:RDFOps[Rdf]) = new IssuePrefix(ops)
}


class IntentPrefix[Rdf <: RDF](ops: RDFOps[Rdf]) extends PrefixBuilder("ipx","http://www.inbyte.semantic/lucy/ontologies/2018/5/knowledge#")(ops){
  val Answer = apply("Answer")
  val Intent = apply("Intent")
  val hasAnswer = apply("hasAnswer")
  val intentType = apply("type")
  val value = apply("value")
}

object IntentPrefix{
  def apply[Rdf <: RDF](implicit ops:RDFOps[Rdf]) = new IntentPrefix(ops)
}

