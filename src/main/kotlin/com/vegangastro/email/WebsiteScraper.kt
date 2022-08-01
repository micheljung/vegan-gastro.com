package com.vegangastro.email

import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.*

@Single
class WebsiteScraper : KoinComponent {

  private val logger = LoggerFactory.getLogger(WebsiteScraper::class.java)

  fun scrape(url: URL): WebsiteInfo {
    logger.info("Scraping $url")
    val body = read(url)
    val email = findEmailOnBody(body)
      ?: findContactLink(body, url, contactLinkRegex)?.let { findEmailOnBody(read(URL(it))) }
      ?: findContactLink(body, url, impressumLinkRegex)?.let { findEmailOnBody(read(URL(it))) }
    val locale = findLocaleOnBody(body)
    val country = countryFromUrl(url)
    return WebsiteInfo(
      url,
      email,
      locale?.let { Locale.Builder().setLocale(it).setRegion(country).build() },
    )
  }

  private fun countryFromUrl(website: URL): String? {
    val ccTld = website.host.substringAfterLast(".").uppercase()
    return if (Locale.getISOCountries().contains(ccTld)) ccTld else null
  }

  private fun findLocaleOnBody(body: String) =
    langRegex.find(body)?.let { Locale.Builder().setLanguageTag(it.groupValues[1]).build() }

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

  private fun findContactLink(body: String, website: URL, regex: Regex) = regex.find(body)?.let {
    val href = it.groupValues[1]
    if (href.startsWith("http")) {
      href
    } else {
      "${website.protocol}://${website.host}${if (website.port > 0) ":${website.port}" else ""}/${href.removePrefix("/")}"
    }
  }

  private fun findEmailOnBody(body: String): String? {
    return emailRegex.find(body)?.let { it.groupValues[0] }
  }

  companion object {
    private val htmlRegexOptions = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    val emailRegex = Regex("[a-zA-Z][a-zA-Z\\d_.+-]+@[a-zA-Z][a-zA-Z\\d_.-]+\\.[a-zA-Z_.-]{2,5}", htmlRegexOptions)
    val langRegex = Regex("<html[^>]+?lang=\"(.+?)\"", htmlRegexOptions)
    val contactLinkRegex = Regex("href=\"([^\"]+)\">.*?(?:Contact|Kontakt|Impressum)[^<]*</a>", htmlRegexOptions)
    val impressumLinkRegex = Regex("href=\"([^\"]+)\">.*?Impressum[^<]*</a>", htmlRegexOptions)
  }
}

data class WebsiteInfo(
  val url: URL,
  val email: String?,
  val locale: Locale?,
)
