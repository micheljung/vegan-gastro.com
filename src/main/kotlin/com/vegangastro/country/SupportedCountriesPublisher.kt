package com.vegangastro.country

import com.vegangastro.plugins.Connection
import com.vegangastro.serialization.CountrySerializer
import com.vegangastro.serialization.Msg
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Single
class SupportedCountriesPublisher {
  suspend fun publishTo(connection: Connection) {
    val locales = SupportedCountries(Country.values().toList())
    connection.send(locales)
  }
}

@Serializable
data class SupportedCountries(
  val countries: List<Country>,
) : Msg() {
  override val type_: String
    get() = MSG_TYPE

  companion object {
    const val MSG_TYPE = "supportedCountries"
  }
}

@Serializable(with = CountrySerializer::class)
enum class Country(val code: String) {
  SWITZERLAND("CH"),
  GERMANY("DE"),
}