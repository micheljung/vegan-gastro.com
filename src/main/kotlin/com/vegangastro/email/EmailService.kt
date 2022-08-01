package com.vegangastro.email

import io.ktor.http.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

@Single
class EmailService : KoinComponent {

  private val properties by inject<EmailProperties>()
  private val templateRenderer by inject<TemplateRenderer>()

  fun send(subject: String, template: Template, to: String, replacements: Map<String, String>) {
    SimpleEmail().apply {
      hostName = properties.smtpHost
      setSmtpPort(properties.smtpPort)
      sslSmtpPort = properties.smtpSslPort.toString()
      setAuthenticator(DefaultAuthenticator(properties.smtpUser, properties.smtpPassword))
      isSSLOnConnect = properties.smtpUseSsl
      setFrom(properties.from)
      this.subject = subject
      val content = templateRenderer.render(template).let {
        var newString = it
        replacements.entries.forEach { entry ->
          newString = newString.replace("{{ ${entry.key} }}", entry.value)
        }
        newString
      }
      setContent(content, ContentType.Text.Html.toString())
      addTo(to)
    }.send()
  }
}

enum class Template(
  val subject: String,
  val templateName: String,
  val locale: Locale,
) {
  STANDARD_DE_CH("Ihr Menü", "simple", Locale("de", "CH"));

  fun getString(): String {
    val resourceName = "/mjml/$templateName.$locale.mjml"
    val stream = javaClass.getResourceAsStream(resourceName) ?: error("Could not find $resourceName")
    return stream.use { BufferedReader(InputStreamReader(stream)).readText()
      .replace("ä", "&auml;")
      .replace("ö", "&ouml;")
      .replace("ü", "&uuml;")
      .replace("Ä", "&Auml;")
      .replace("Ö", "&Ouml;")
      .replace("Ü", "&Uuml;")
    }
  }
}

data class MjmlProperties(
  val applicationId: String = System.getenv("MJML_APPLICATION_ID") ?: "",
  val applicationKey: String = System.getenv("MJML_APPLICATION_KEY") ?: "",
)

data class EmailProperties(
  val smtpHost: String = System.getenv("EMAIL_SMTP_HOST") ?: "",
  val smtpUser: String = System.getenv("EMAIL_SMTP_USER") ?: "",
  val smtpPassword: String = System.getenv("EMAIL_SMTP_PASSWORD") ?: "",
  val from: String = System.getenv("EMAIL_FROM") ?: "info@vegan-gastro.com",
  val smtpPort: Int = System.getenv("EMAIL_SMTP_PORT")?.let { Integer.parseInt(it) } ?: 2500,
  val smtpSslPort: Int = System.getenv("EMAIL_SMTP_SSL_PORT")?.let { Integer.parseInt(it) } ?: 465,
  val smtpUseSsl: Boolean = System.getenv("EMAIL_SMTP_USE_SSL")?.let { it.lowercase() == "true" } ?: true,
)