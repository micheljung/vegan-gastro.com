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

@Single
class PlaceRepository : KoinComponent {
  private val geoApiContext by inject<GeoApiContext>()

  fun findByPlaceId(placeId: String): Place = transaction {
    PlaceTable.select { PlaceTable.placeId eq placeId }.singleOrNull()?.let {
      Place(
        it[PlaceTable.id].value,
        it[PlaceTable.placeId],
        it[PlaceTable.name],
        it[PlaceTable.website]?.let { website -> URL(website) },
        it[PlaceTable.email],
        it[PlaceTable.sent],
      )
    }
      ?: PlacesApi.placeDetails(geoApiContext, placeId)
        .fields(PlaceDetailsRequest.FieldMask.NAME, PlaceDetailsRequest.FieldMask.WEBSITE)
        .await().let { details ->
          Place(null, placeId, details.name, details.website)
        }
        .let { save(it) }
  }

  fun save(place: Place): Place {
    transaction {
      if (place.id == null) {
        place.id = PlaceTable.insertAndGetId {
          it[placeId] = place.placeId
          it[name] = place.name
          it[website] = place.website?.toExternalForm()
          it[email] = place.email
          it[sent] = place.sent
        }.value
      } else {
        PlaceTable.update({ PlaceTable.id eq place.id }) {
          it[placeId] = place.placeId
          it[name] = place.name
          it[website] = place.website?.toExternalForm()
          it[email] = place.email
          it[sent] = place.sent
        }
      }
    }
    return place
  }
}

object PlaceTable : IntIdTable("place") {
  val placeId = varchar("placeId", 255).uniqueIndex()
  val name = varchar("name", 255)
  val website = varchar("website", 255).nullable()
  val email = varchar("email", 255).nullable()
  var sent = timestamp("sent").nullable()
}

data class Place(
  var id: Int?,
  val placeId: String,
  val name: String,
  var website: URL?,
  var email: String? = null,
  var sent: Instant? = null,
)
