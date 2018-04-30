package asigno

case class TicketCategory (ticketCategoryId:String,parentId:String) {
  override def toString: String =
    s"{ ticketCategoryId = $ticketCategoryId,parentId = $parentId }"
}
