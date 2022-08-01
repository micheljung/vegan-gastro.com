package com.vegangastro.place

import com.vegangastro.email.EmailProperties
import com.vegangastro.email.EmailService
import com.vegangastro.email.Template
import com.vegangastro.job.ContactPlace
import io.ktor.http.*
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

@Single
class PlaceService : KoinComponent {

  private val placeRepository: PlaceRepository by inject()
  private val emailService: EmailService by inject()
  private val emailProperties: EmailProperties by inject()

  suspend fun findByPlaceId(placeId: String) = placeRepository.findByPlaceId(placeId)

  suspend fun contact(message: ContactPlace) {
    validate("name", message.place.name)
    validate("email", message.place.email)

    val place = placeRepository.findByPlaceId(message.place.placeId)!!.copy(
      name = message.place.name,
      email = message.place.email,
      locale = message.place.locale,
      needsReview = false,
    )
    placeRepository.save(place)

    val template = Template.forLocale(place.locale!!)
    emailService.send(
      template.subject,
      template, place.email!!,
      mapOf(
        "restaurantName" to place.name,
        "readConfirmationUrl" to readConfirmationUrl(place).toString(),
        "tipsUrl" to tipsUrl(place).toString(),
      ),
    )

    placeRepository.save(place.copy(sent = Instant.now()))
  }

  private fun readConfirmationUrl(place: Place) = URLBuilder(emailProperties.baseUrl).apply {
    appendPathSegments("confirm", place.placeId)
  }.build()

  private fun tipsUrl(place: Place) = URLBuilder(emailProperties.baseUrl).apply {
    appendPathSegments("tips", place.locale.toString())
  }.build()

  private fun validate(name: String, value: String?) {
    check(!value.isNullOrBlank()) { "$name must not be empty or null" }
  }

  suspend fun confirmRead(placeId: String): Place? {
    return placeRepository.findByPlaceId(placeId)
      ?.apply { placeRepository.save(copy(readConfirmed = true)) }
  }
}