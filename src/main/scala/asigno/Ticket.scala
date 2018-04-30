package asigno

case class Ticket (createdDate:Long,customerData: CustomerData,labels:List[LabelView],
                   ticketType: TicketType,ticketCategory: TicketCategory,
                   summary:String,description:String,
                   priority:String = "Baja",
                   channel:String = "Chat",extraInfoList:List[Any] = List()){
  override def toString: String = "{" +
      s"createdDate = $createdDate,customerData = $customerData,labels = $labels," +
      s"ticketType = $ticketType,ticketCategory = $ticketCategory," +
      s"summary = $summary,description = $description,priority = $priority,channel = $channel" +
    "}"
}
