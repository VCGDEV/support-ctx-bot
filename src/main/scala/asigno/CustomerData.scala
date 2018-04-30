package asigno

case class CustomerData (customerInfo:List[CustomerInfo],idExternalCustomer:Long) {
  override def toString: String =
    s"{ customerInfo = $customerInfo,idExternalCustomer =$idExternalCustomer }"
}
