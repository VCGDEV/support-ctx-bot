package asigno


case class CustomerView(idCustomer:Long,createdDate:Long,lastModifiedDate:Long,
                        customerType:String,name:String,paternalName:String,maternalName:String,
                        rfc:String,commercialName:String,customerStatus:String,
                        infoList:List[AdditionalInfoView],numberList:List[ContactNumberView],addressList:List[AddressView],
                        email:String)


case class AdditionalInfoView(idAdditionalInfo:Long,key:String,value:String,createdDate:Long,
                              modifiedDate:Long,additionalInfoStatus:String)

case class ContactNumberView(idCustomerContactNumber:Long,phone:String,phoneType:String,createdDate:Long,
                              modifiedDate:Long,contactNumberStatus:String)

case class AddressView(idCustomerAddress:Long,street:String,numExt:String,numInt:String,zipCode:Int,
                       country:String,city:String,suburb:String,street1:String,street2:String,addressStatus:String,
                       latitude:Option[Double],longitude:Option[Double])