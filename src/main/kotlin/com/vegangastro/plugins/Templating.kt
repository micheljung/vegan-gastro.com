package com.vegangastro.plugins

import com.vegangastro.email.EmailService
import com.vegangastro.email.Template
import com.vegangastro.email.WebsiteInfo
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.koin.ktor.ext.inject

fun Application.configureTemplating() {

    val emailService: EmailService by inject()

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
    }
}

fun needsReview(info: WebsiteInfo) = info.email == null ||
        !info.email.endsWith("@${info.url.host.removePrefix("www.")}")
        || !listOf("kontakt", "info", "restaurant").contains(info.email.substringBefore("@"))
