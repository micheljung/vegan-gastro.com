package com.vegangastro.db

data class DbProperties(
  val url: String = System.getenv("DB_URL") ?: "jdbc:h2:./build/db",
  val user: String = System.getenv("DB_USER") ?: "sa",
  val password: String = System.getenv("DB_PASSWORD") ?: "",
  val driver: String = System.getenv("DB_DRIVER") ?: "org.h2.Driver",
)