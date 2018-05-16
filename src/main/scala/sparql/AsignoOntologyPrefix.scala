package sparql


import org.w3.banana.{RDF, RDFOps}

object AsignoOntologyPrefix {
  def apply[Rdf <: RDF](implicit ops: RDFOps[Rdf]) = new AsignoOntologyPrefix(ops)
}

class AsignoOntologyPrefix[Rdf <: RDF](ops: RDFOps[Rdf]) extends PrefixBuilder("asn","http://www.apps.inbyte.mx/vcruz/ontologies/2018/4/asigno#")(ops){
  //object properties
  val companyAddress = apply("companyAddress")
  val hasAddress = apply("hasAddress")
  val hasPC = apply("hasPC")
  val hasPrinter = apply("hasPrinter")
  val hasServer = apply("hasServer")
  val hasSoftware = apply("hasSoftware")
  val isConnectedToPrinter = apply("isConnectedToPrinter")
  val isInCategory = apply("isInCategory")
  val isInCompany = apply("isInCompany")
  val isInGroup = apply("isInGroup")
  val personAddress = apply("personAddress")
  //data properties
  val adjacentStreet1 = apply("adjacent_street1")
  val adjacentStreet2 = apply("adjacent_street_2")
  val brand = apply("brand")
  val city = apply("city")
  val companyName = apply("company_name")
  val country = apply("country")
  val email = apply("email")
  val expirationDate = apply("expiration_date")
  val full_name = apply("full_name")
  val government = apply("government")
  val hasScanner = apply("has_scanner")
  val hdSize = apply("hd_size")
  val id = apply("id")
  val installedDate = apply("installed_date")
  val ipAddress = apply("ip_address")
  val localPhone = apply("local_phone")
  val mobilePhone = apply("mobile_phone")
  val model = apply("model")
  val name = apply("name")
  val numExt = apply("num_ext")
  val numInt = apply("num_int")
  val officePhone = apply("office_phone")
  val operatingSystem = apply("operating_system")
  val postalCode = apply("postal_code")
  val processor = apply("processor")
  val ramSize = apply("ram_size")
  val street = apply("street")
  val suburb = apply("suburb")
  //classes
  val Address = apply("Address")
  val Category = apply("Category")
  val Company = apply("company")
  val ComputerMachine = apply("ComputerMachine")
  val Customer = apply("Customer")
  val Desktop = apply("Desktop")
  val Group = apply("Group")
  val Hardware = apply("Hardware")
  val Laptop = apply("Laptop")
  val Local = apply("Local")
  val Network = apply("Network")
  val Pc = apply("Pc")
  val Person = apply("Person")
  val Printer = apply("Printer")
  val Server = apply("Server")
  val Software = apply("Software")
  val Technician = apply("Technician")
  val User = apply("User")
}


