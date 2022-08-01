package com.vegangastro.locale

import com.vegangastro.email.Template
import com.vegangastro.plugins.Connection
import com.vegangastro.serialization.LocaleSerializer
import com.vegangastro.serialization.Msg
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import java.util.*

@Single
class SupportedLocalesPublisher {
  suspend fun publishTo(connection: Connection) {
    val locales = SupportedLocales(Template.values().map { it.locale })
    connection.send(locales)
  }
}

@Serializable
data class SupportedLocales(
  val locales: List<@Serializable(with = LocaleSerializer::class) Locale?>,
) : Msg() {
  override val type_: String
    get() = MSG_TYPE

  companion object {
    const val MSG_TYPE = "supportedLocales"
  }
}
