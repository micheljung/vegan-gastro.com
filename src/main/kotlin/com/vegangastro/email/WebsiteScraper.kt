package com.vegangastro.email

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*

@Single
class WebsiteScraper : KoinComponent {

    private val logger = LoggerFactory.getLogger(WebsiteScraper::class.java)

    suspend fun scrape(url: URL): WebsiteInfo {
        logger.info("Scraping $url")
        var email: String? = null
        var country: String? = null
        var locale: Locale? = null
        try {
            val body = read(url)
            email = findEmailInBody(body)
                ?: findContactLink(body, url, contactLinkRegex)?.let { findEmailInBody(read(URL(it))) }
                        ?: findContactLink(body, url, impressumLinkRegex)?.let { findEmailInBody(read(URL(it))) }
            locale = findLocaleOnBody(body)
            country = countryFromUrl(url)
        } catch (e: Exception) {
            logger.warn("Could not scrape $url", e)
        }
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

    private suspend fun read(website: URL): String {
        return HttpClient().get(website) {
            headers {
                append("Accept", "text/html")
                append(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
                )
            }
        }.bodyAsText()
    }

    private fun findContactLink(body: String, website: URL, regex: Regex) = regex.find(body)?.let {
        val href = it.groupValues[1]
        if (href.startsWith("http")) {
            href
        } else {
            "${website.protocol}://${website.host}${if (website.port > 0) ":${website.port}" else ""}/${
                href.removePrefix(
                    "/"
                )
            }"
        }
    }

    private fun findEmailInBody(body: String): String? {
        val allAddresses = emailRegex.findAll(body)
            .map { it.groupValues[0] }
            .filter { !emailDomainBlacklist.contains(it.substringAfterLast("@")) }
            .toList()
        return allAddresses.find { it.startsWith("info@") } ?: allAddresses.firstOrNull()
    }

    companion object {
        private val htmlRegexOptions = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
        private val emailDomainBlacklist = listOf(
            "sentry-next.wixpress.com",
            "sentry-viewer.wixpress.com",
            "sentry.wixpress.com",
            "sentry.io",
        )
        val emailRegex = Regex("[a-zA-Z][a-zA-Z\\d_.+-]*@[a-zA-Z][a-zA-Z\\d_.-]*\\.[a-zA-Z]{2,24}", htmlRegexOptions)
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
