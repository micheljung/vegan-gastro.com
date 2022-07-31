package com.vegangastro.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

  routing {
    static("/tips/") {
      static("en") {
        staticBasePackage = "static.tips.en"
        defaultResource("index.html")
      }
      static("de") {
        staticBasePackage = "static.tips.de"
        defaultResource("index.html")
      }
    }
  }
}
