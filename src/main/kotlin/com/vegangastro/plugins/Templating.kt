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
            span { +"0" }
            +" restaurants have been contacted"
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
                value = "de"
                label = "Deutsch"
              }
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
          germanOutreachEmail()
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

/**
 * The message sent to a restaurant. Placeholders in double braces are personalised per
 * recipient before sending; the {{Upload-URL}} link is valid only for that restaurant.
 */
private fun FlowContent.germanOutreachEmail() {
  section {
    h2 { +"Diese E-Mail wird versendet" }

    p {
      strong { +"Betreff: " }
      +"Eine Idee für die Speisekarte vom {{Restaurantname}}"
    }

    p { +"Hallo Team vom {{Restaurantname}}," }

    p {
      +"wir sind eine kleine Gruppe von Freiwilligen und schreiben Ihnen aus einem "
      +"einfachen Grund: Wir möchten, dass pflanzliche Gerichte überall ein Stück "
      +"selbstverständlicher werden – und Gastronomien wie Ihre spielen dabei die Hauptrolle."
    }

    p {
      +"Vorab, damit es keine Missverständnisse gibt: "
      strong { +"Wir verkaufen nichts." }
      +" Hinter dieser Nachricht steht kein Unternehmen, kein Produkt und keine Rechnung – "
      +"nur ein ehrenamtliches Projekt. Diese Mail ist unsere einzige Anfrage an Sie; wir "
      +"melden uns nicht erneut."
    }

    p {
      +"Immer mehr Gäste suchen gezielt nach pflanzlichen Gerichten – aus ganz "
      +"unterschiedlichen Gründen: "
      strong {
        +"der Umwelt zuliebe, wegen einer Laktose- oder anderen Unverträglichkeit "
        +"gegenüber tierischen Produkten, aus gesundheitlichen Überlegungen oder einfach "
        +"aus Neugier."
      }
      +" Das ist längst keine kleine Randgruppe mehr."
    }

    p {
      +"Und oft gibt genau diese Person den Ausschlag: Eine Gruppe von vier bis sechs "
      +"Personen sucht ein Lokal, und "
      strong { +"wer weitgehend auf tierische Produkte verzichtet, entscheidet mit, wohin alle gehen." }
      +" Fehlt ein passendes Gericht, zieht die ganze Runde weiter – schade um einen Tisch, "
      +"der eigentlich zu Ihnen gehört hätte."
    }

    p {
      +"Das Schöne daran: Es braucht keine neue Küche und keine zweite Speisekarte. Oft "
      +"genügt "
      strong { +"ein einziges, gut gemachtes pflanzliches Gericht" }
      +"."
    }

    p {
      strong { +"Ein Punkt ist dabei entscheidend:" }
      +" Es reicht nicht, ein solches Gericht anzubieten – es muss auf der Karte "
      strong { +"klar gekennzeichnet" }
      +" sein. Ohne deutliche Markierung wird es von genau den Gästen übersehen, für die es "
      +"gedacht ist. Erst die sichtbare Kennzeichnung macht daraus einen Grund, bei Ihnen zu "
      +"reservieren."
    }

    p {
      +"Damit das mühelos gelingt, stellen wir Ihnen "
      strong { +"kostenlose, einheitliche Symbole" }
      +" zum Kennzeichnen pflanzlicher und vegetarischer Gerichte bereit – frei zum "
      +"Herunterladen und Verwenden. Zusammen mit einfachen, risikoarmen Ideen für den "
      +"ersten Schritt finden Sie alles hier:"
    }
    p { strong { a(href = "{{Website-URL}}") { +"→ {{Website-URL}}" } } }

    p { +"Kostenlos, ohne Anmeldung. Ob Sie etwas davon übernehmen, entscheiden natürlich ganz allein Sie." }

    p {
      +"Und noch etwas: Wir führen ein öffentliches Verzeichnis von Restaurants, die "
      +"pflanzliche Gerichte "
      strong { +"klar gekennzeichnet" }
      +" anbieten – als Orientierung für Gäste, die gezielt danach suchen: "
      a(href = "{{Verzeichnis-URL}}") { +"{{Verzeichnis-URL}}" }
      +". Wenn {{Restaurantname}} dort erscheinen möchte, laden Sie einfach ein Foto Ihrer "
      +"gekennzeichneten Speisekarte als Nachweis hoch – über Ihren persönlichen Link, der "
      +"nur für Ihr Haus gültig ist:"
    }
    p { strong { a(href = "{{Upload-URL}}") { +"→ {{Upload-URL}}" } } }

    p {
      +"Herzliche Grüsse"
      br
      +"{{Absendername}}"
      br
      +"im Namen der Initiative {{Initiativenname}}"
      br
      a(href = "{{Website-URL}}") { +"{{Website-URL}}" }
    }
  }
}
