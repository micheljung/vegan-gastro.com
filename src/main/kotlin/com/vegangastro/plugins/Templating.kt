package com.vegangastro.plugins

import com.vegangastro.email.EmailService
import com.vegangastro.email.Template
import com.vegangastro.email.WebsiteInfo
import com.vegangastro.email.WebsiteScraper
import com.vegangastro.places.PlaceProvider
import com.vegangastro.places.PlaceRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.koin.ktor.ext.inject
import java.time.Instant

fun Application.configureTemplating() {

  val emailService: EmailService by inject()
  val placeProvider: PlaceProvider by inject()
  val websiteScraper: WebsiteScraper by inject()
  val placeRepository: PlaceRepository by inject()

  routing {
    get("/email") {
      call.respondHtmlTemplate(RootTemplate()) {
        header {
          +"Let's ask all restaurants to offer vegan menus"
        }
        content {
          p {
            span { +"0" }
            +"restaurants have been contacted"
          }
          h2 { +"How it works" }
          ol {
            li { +"Enter the e-mail address of your target restaurant" }
            li { +"Choose the desired language" }
            li { +"We'll send them the e-mail shown below" }
          }
          p { +"Each e-mail address will only be contacted once" }
          form(
            action = "/email/submit",
            method = FormMethod.post,
            encType = FormEncType.applicationXWwwFormUrlEncoded,
          ) {
            input(InputType.text, name = "", classes = "form-control") {
              name = "emailAddress"
              required = true
              placeholder = "info@restaurant.com"
            }
            select {
              option {
                value = "en"
                label = "English"
              }
            }
            button(
              type = ButtonType.submit,
            ) {
              +"Submit"
            }
          }
        }
      }
    }
    post("/email/submit") {
      val params = call.receiveParameters()
      val emailAddress = params["emailAddress"].toString()

      emailService.send("Test", Template.STANDARD_DE_CH, emailAddress, emptyMap())

      call.respondHtmlTemplate(RootTemplate()) {
        header {
          +"Great!"
        }
        content {
          p { +"An e-mail has been sent to $emailAddress" }
        }
      }
    }
    get("/country") {
      call.respondHtmlTemplate(RootTemplate()) {
        header {
          +"Let's ask all restaurants to offer vegan menus"
        }
        content {
          p {
            span { +"0" }
            +"restaurants have been contacted"
          }
          h2 { +"How it works" }
          ol {
            li { +"Enter the target country" }
            li { +"Choose the desired language" }
            li { +"We'll send them the e-mail shown below" }
          }
          p { +"Each e-mail address will only be contacted once" }
          form(
            action = "/country/submit",
            method = FormMethod.post,
            encType = FormEncType.applicationXWwwFormUrlEncoded,
          ) {
            input(InputType.text, name = "", classes = "form-control") {
              name = "country"
              required = true
              placeholder = "Switzerland"
            }
            select {
              option {
                value = "en"
                label = "English"
              }
            }
            button(
              type = ButtonType.submit,
            ) {
              +"Submit"
            }
          }
        }
      }
    }
    post("/country/submit") {
      val params = call.receiveParameters()
      val country = params["country"].toString()

      placeProvider.getPlaces(country)
        .map {
          it.apply {
            if (it.email == null && it.website != null) {
              websiteScraper.scrape(it.website).let { info ->
                return@map placeRepository.save(it.copy(email = info.email, locale = info.locale))
              }
            }
          }
        }
        .filter { it.email != null }
        .filter { it.sent == null }
        .forEach {
          emailService.send(
            Template.STANDARD_DE_CH.subject, Template.STANDARD_DE_CH, it.email!!,
            mapOf(
              "restaurantName" to it.name,
            ),
          )
          placeRepository.save(it.copy(sent = Instant.now()))
        }

      call.respondHtmlTemplate(RootTemplate()) {
        header { +"Sent!" }
      }
    }
    get("/tips/{placeId}") {
      call.respondRedirect {

        this.path()
      }
    }
  }
}

fun needsReview(info: WebsiteInfo) = info.email != "info@${info.url.host}"
