package com.vegangastro.email

import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

@Single
class EmailScraper : KoinComponent {

  private val logger = LoggerFactory.getLogger(EmailScraper::class.java)

  fun scrape(website: URL): String? {
    logger.info("Scraping $website")
    val body = read(website)
    return findEmailOnBody(body)
      ?: findContactLink(body, website)?.let {
        findEmailOnBody(read(URL(it)))
      }
  }

  private fun read(website: URL): String {
    val request = HttpRequest.newBuilder(website.toURI())
      .header("Accept", "text/html")
      .header(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
      )
      .GET()
      .build()
    val response = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.ALWAYS)
      .version(HttpClient.Version.HTTP_1_1)
      .build()
      .send(request, BodyHandlers.ofString())

    return response.body()
  }

  private fun findContactLink(body: String, website: URL): String? {
    return contactLinkRegex.find(body)?.let {
      val href = it.groupValues[1]
      if (href.startsWith("http")) {
        href
      } else {
        "${website.protocol}://${website.host}${if (website.port > 0) ":${website.port}" else ""}/${href.removePrefix("/")}"
      }
    }
  }

  private fun findEmailOnBody(body: String): String? {
    return emailRegex.find(body)?.let { it.groupValues[0] }
  }

  companion object {
    val emailRegex = Regex("[a-zA-Z\\d_.-]+@[a-zA-Z\\d_.-]+\\.[a-zA-Z\\d_.-]{2,5}")
    val contactLinkRegex = Regex(
      "href=\"([^\"]+)\">.*?(?:Contact|Kontakt)[^<]*</a>",
      setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
    )
  }
}
