package repository.model

object JdbcConnector {
  val db = slick.jdbc.JdbcBackend.Database.forConfig("db.config")
}
