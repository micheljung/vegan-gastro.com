package com.vegangastro.plugins

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Application.configureTemplating() {

  routing {
    get("/") {
      call.respondHtml {
        body {
          h1 { +"Let's ask all restaurants to offer vegan menus" }
          p {
            span { 0 }
            +"restaurants have been contacted"
          }
          h2 { +"How it works" }
          ol {
            li { +"Enter the e-mail address of your target restaurant" }
            li { +"Choose the desired language" }
            li { +"We'll send them the e-mail shown below" }
          }
          p { +"Each e-mail address will only be contacted once" }
          form(action = "/submit", method = FormMethod.post, encType = FormEncType.applicationXWwwFormUrlEncoded) {
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
              type = ButtonType.submit
            ) {
              +"Submit"
            }
          }
        }
      }
    }
    post("/submit") {
      val params = call.receiveParameters()
      val emailAddress = params["emailAddress"].toString()
      call.respondHtml {
        body {
          h1 { +"Great!" }
          p { +"An e-mail has been sent to $emailAddress" }
        }
      }
    }
  }
}
