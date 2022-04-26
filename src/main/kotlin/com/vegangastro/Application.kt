package com.vegangastro

import com.vegangastro.plugins.configureMonitoring
import com.vegangastro.plugins.configureRouting
import com.vegangastro.plugins.configureTemplating
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0", watchPaths = listOf("classes")) {
    configureRouting()
    configureMonitoring()
    configureTemplating()
  }.start(wait = true)
}
