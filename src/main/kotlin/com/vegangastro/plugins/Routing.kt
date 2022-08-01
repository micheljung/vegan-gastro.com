package com.vegangastro.plugins

import com.vegangastro.country.SupportedCountriesPublisher
import com.vegangastro.email.WebsiteScraper
import com.vegangastro.job.ContactPlace
import com.vegangastro.job.JobService
import com.vegangastro.job.JobWebSocketEventPublisher
import com.vegangastro.job.PlacesSearch
import com.vegangastro.locale.SupportedLocalesPublisher
import com.vegangastro.place.PlaceService
import com.vegangastro.place.PlaceWebSocketEventPublisher
import com.vegangastro.place.PlacesSummaryService
import com.vegangastro.serialization.Msg
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*

fun Application.configureRouting() {
  val logger = LoggerFactory.getLogger(javaClass)
  val jobEventPublisher: JobWebSocketEventPublisher by inject()
  val placeEventPublisher: PlaceWebSocketEventPublisher by inject()
  val placesSummaryService: PlacesSummaryService by inject()
  val supportedLocalesPublisher: SupportedLocalesPublisher by inject()
  val supportedCountriesPublisher: SupportedCountriesPublisher by inject()
  val websiteScraper: WebsiteScraper by inject()
  val jobService: JobService by inject()
  val placeService: PlaceService by inject()

  suspend fun onMessage(source: Connection, message: Msg) {
    when (message) {
      is PlacesSearch -> jobService.submit(message) { source.send(it) }
      is ContactPlace -> placeService.contact(message)
    }
  }

  routing {
    static("/tips/") {
      com.vegangastro.email.Template.values().forEach {
        static(it.locale.toString()) {
          staticBasePackage = "static.tips.${it.locale}"
          defaultResource("index.html")
        }
      }
    }
    get("/scrape/{url}") {
      val url = call.parameters["url"]
      val result = websiteScraper.scrape(URL(url))
      call.respondText { result.toString() }
    }
    get("/confirm/{placeId}") {
      val placeId = call.parameters["placeId"] ?: error("Missing placeId")
      val place = placeService.confirmRead(placeId) ?: error("No such place: $placeId")
      val locale = place.locale ?: Locale.ENGLISH

      val title = when (locale.language) {
        "de" -> "Besten Dank"
        else -> "Thank you"
      }

      call.respondHtmlTemplate(RootTemplate()) {
        header { +title }
      }
    }

    static("/") {
      get {
        call.respondRedirect("/app")
      }
    }

    singlePageApplication {
      applicationRoute = "/app"
      useResources = true
      react("webapp")
    }

    webSocket("/ws") {
      val connection = Connection(this)
//      jobEventPublisher.subscribe(connection)
//      placeEventPublisher.subscribe(connection)
      supportedLocalesPublisher.publishTo(connection)
      supportedCountriesPublisher.publishTo(connection)
      placesSummaryService.getSummary().let { connection.send(it) }

      fun onConnectionClosed(connection: Connection) {
        logger.info("Connection closed")
        jobEventPublisher.unsubscribe(connection)
        placeEventPublisher.unsubscribe(connection)
      }

      try {
        for (frame in incoming) {
          when (frame) {
            is Frame.Close -> onConnectionClosed(connection)
            is Frame.Text -> onMessage(connection, Json.decodeFromString(frame.readText()))
            else -> Unit
          }
        }
      } finally {
        onConnectionClosed(connection)
      }
    }
  }
}

data class Connection(
  private val session: DefaultWebSocketSession,
) {
  suspend fun send(msg: Msg) {
    session.send(Json.encodeToString(msg))
  }
}