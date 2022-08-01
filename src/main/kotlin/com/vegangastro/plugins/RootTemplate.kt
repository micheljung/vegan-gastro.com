package com.vegangastro.plugins

import io.ktor.server.html.*
import kotlinx.html.*


class RootTemplate : Template<HTML> {
    val header = Placeholder<FlowContent>()
    val content = Placeholder<FlowContent>()

    override fun HTML.apply() {
        head {
            styleLink("webjars/bootstrap/bootstrap.css")
        }
        body {
            h1 {
                insert(header)
            }
            insert(content)
        }
    }
}