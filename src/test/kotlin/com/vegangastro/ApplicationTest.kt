package com.vegangastro

import com.vegangastro.plugins.configureTemplating
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
  @Test
  fun landingPageIsServed() = testApplication {
    application {
      configureTemplating()
    }
    val response = client.get("/")
    assertEquals(HttpStatusCode.OK, response.status)
    assertTrue(response.bodyAsText().contains("Let's ask all restaurants to offer vegan menus"))
  }
}
