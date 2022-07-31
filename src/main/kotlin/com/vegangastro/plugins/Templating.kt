package com.vegangastro.plugins

import com.vegangastro.email.EmailScraper
import com.vegangastro.email.EmailService
import com.vegangastro.email.Template
import com.vegangastro.places.PlaceProvider
import com.vegangastro.places.PlaceRepository
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.koin.ktor.ext.inject
import java.time.Instant

fun Application.configureTemplating() {

  val emailService: EmailService by inject()
  val placeProvider: PlaceProvider by inject()
  val emailScraper: EmailScraper by inject()
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

      emailService.send("Test", Template.STANDARD_ENGLISH, emailAddress)

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
              it.email = emailScraper.scrape(it.website!!)
              if (it.email != null) {
                placeRepository.save(it)
              }
            }
          }
        }
        .filter { it.email != null }
        .filter { it.sent == null }
        .forEach {
          emailService.send("Your menu", Template.STANDARD_ENGLISH, it.email!!)
          it.sent = Instant.now()
          placeRepository.save(it)
        }

      call.respondHtmlTemplate(RootTemplate()) {
        header { +"Sent!" }
      }
    }
  }
}
