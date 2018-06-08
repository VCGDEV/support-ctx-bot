package repository.model

trait JdbcConnector {
  val db = slick.jdbc.JdbcBackend.Database.forConfig("db.config")
}
