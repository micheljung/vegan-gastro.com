package com.vegangastro.places

import com.google.maps.GeoApiContext
import com.google.maps.PlaceDetailsRequest
import com.google.maps.PlacesApi
import com.google.maps.model.PlaceDetails
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Instant
import java.util.*

@Single
class PlaceRepository : KoinComponent {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val geoApiContext by inject<GeoApiContext>()

  fun findByPlaceId(placeId: String): Place = transaction {
    fetchPlaceFromDb(placeId)
      ?: save(mapToPlace(placeId, fetchDetailsFromApi(placeId)))
  }

  private fun fetchPlaceFromDb(placeId: String) =
    PlaceTable.select { PlaceTable.placeId eq placeId }.singleOrNull()
      ?.let { mapToPlace(it) }

  private fun mapToPlace(placeId: String, details: PlaceDetails) = Place(
    null,
    placeId,
    details.name,
    needsReview = true,
    ignore = false,
    address = details.formattedAddress,
    locale = Locale.getDefault(),
    website = details.website,
  )

  private fun fetchDetailsFromApi(placeId: String): PlaceDetails {
    logger.info("Fetching details for place $placeId from API")
    return PlacesApi.placeDetails(geoApiContext, placeId)
      .fields(
        PlaceDetailsRequest.FieldMask.NAME,
        PlaceDetailsRequest.FieldMask.WEBSITE,
        PlaceDetailsRequest.FieldMask.FORMATTED_ADDRESS,
      )
      .await()
  }

  private fun mapToPlace(it: ResultRow) = Place(
    it[PlaceTable.id].value,
    it[PlaceTable.placeId],
    it[PlaceTable.name],
    it[PlaceTable.needsReview],
    it[PlaceTable.ignore],
    it[PlaceTable.address],
    it[PlaceTable.language]?.let { lang -> Locale(lang, it[PlaceTable.country] ?: "") },
    it[PlaceTable.website]?.let { website -> URL(website) },
    it[PlaceTable.email],
    it[PlaceTable.sent],
  )

  fun save(place: Place): Place = transaction {
    if (place.id == null) {
      val id = PlaceTable.insertAndGetId {
        it[placeId] = place.placeId
        it[name] = place.name
        it[needsReview] = place.needsReview
        it[ignore] = place.ignore
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
        it[ignore] = place.ignore
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

  fun findAllByNeedsReview(needsReview: Boolean) = transaction {
    PlaceTable.select { PlaceTable.needsReview eq needsReview }
      .map { mapToPlace(it) }
  }
}

object PlaceTable : IntIdTable("place") {
  val placeId = varchar("placeId", 255).uniqueIndex()
  val name = varchar("name", 255)
  var needsReview = bool("needsReview")
  var ignore = bool("ignore")
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
  val ignore: Boolean = false,
  val address: String?,
  val locale: Locale?,
  val website: URL?,
  val email: String? = null,
  val sent: Instant? = null,
)
