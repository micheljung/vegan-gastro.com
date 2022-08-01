package com.vegangastro.places

import com.google.maps.GeoApiContext
import com.google.maps.PlaceDetailsRequest
import com.google.maps.PlacesApi
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URL
import java.time.Instant
import java.util.*

@Single
class PlaceRepository : KoinComponent {
  private val geoApiContext by inject<GeoApiContext>()

  fun findByPlaceId(placeId: String): Place = transaction {
    PlaceTable.select { PlaceTable.placeId eq placeId }.singleOrNull()?.let {
      Place(
        it[PlaceTable.id].value,
        it[PlaceTable.placeId],
        it[PlaceTable.name],
        it[PlaceTable.needsReview],
        it[PlaceTable.address],
        it[PlaceTable.language]?.let { lang -> Locale(lang, it[PlaceTable.country] ?: "") },
        it[PlaceTable.website]?.let { website -> URL(website) },
        it[PlaceTable.email],
        it[PlaceTable.sent],
      )
    }
      ?: PlacesApi.placeDetails(geoApiContext, placeId)
        .fields(
          PlaceDetailsRequest.FieldMask.NAME,
          PlaceDetailsRequest.FieldMask.WEBSITE,
          PlaceDetailsRequest.FieldMask.FORMATTED_ADDRESS,
        )
        .await().let { details ->
          Place(
            null,
            placeId,
            details.name,
            true,
            details.formattedAddress,
            Locale.getDefault(),
            details.website,
          )
        }
        .let { save(it) }
  }

  fun save(place: Place): Place = transaction {
    if (place.id == null) {
      val id = PlaceTable.insertAndGetId {
        it[placeId] = place.placeId
        it[name] = place.name
        it[needsReview] = place.needsReview
        it[address] = place.address
        it[language] = place.locale?.language
        it[country] = place.locale?.country
        it[website] = place.website?.toExternalForm()
        it[email] = place.email
        it[sent] = place.sent
      }.value
      place.copy(id = id)
    } else {
      PlaceTable.update({ PlaceTable.id eq place.id }) {
        it[placeId] = place.placeId
        it[name] = place.name
        it[needsReview] = place.needsReview
        it[address] = place.address
        it[language] = place.locale?.language
        it[country] = place.locale?.country
        it[website] = place.website?.toExternalForm()
        it[email] = place.email
        it[sent] = place.sent
      }
      place
    }
  }
}

object PlaceTable : IntIdTable("place") {
  val placeId = varchar("placeId", 255).uniqueIndex()
  val name = varchar("name", 255)
  var needsReview = bool("needsReview")
  var address = varchar("address", 255).nullable()
  var language = varchar("language", 2).nullable()
  var country = varchar("country", 2).nullable()
  val website = varchar("website", 255).nullable()
  val email = varchar("email", 255).nullable()
  var sent = timestamp("sent").nullable()
}

data class Place(
  val id: Int?,
  val placeId: String,
  val name: String,
  val needsReview: Boolean = false,
  val address: String?,
  val locale: Locale?,
  val website: URL?,
  val email: String? = null,
  val sent: Instant? = null,
)
