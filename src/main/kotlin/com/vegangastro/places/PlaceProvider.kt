package com.vegangastro.places

import com.google.maps.GeoApiContext
import com.google.maps.PlacesApi
import com.google.maps.model.PlaceType
import com.google.maps.model.PlacesSearchResponse
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream

@Single
class PlaceProvider : KoinComponent {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val geoApiContext by inject<GeoApiContext>()
  private val placeRepository by inject<PlaceRepository>()

  fun getPlaces(country: String): Stream<Place> {
    logger.info("Fetching from Places API")
    val initialResponse = PlacesApi.textSearchQuery(geoApiContext, PlaceType.RESTAURANT)
      .region(country)
      .await()

    val lastQueryTime = AtomicReference(Instant.now())

    return Stream
      .iterate(
        initialResponse,
        { t -> t.results.isNotEmpty() },
        { t ->
          if (t.nextPageToken == null) PlacesSearchResponse().apply { results = emptyArray() }
          else {
            val sleepTime = Duration.between(Instant.now(), lastQueryTime.get().plusSeconds(2)).toMillis().coerceAtLeast(0)
            Thread.sleep(sleepTime)
            lastQueryTime.set(Instant.now())
            logger.info("Fetching next page from Places API")
            PlacesApi.textSearchNextPage(geoApiContext, t.nextPageToken).await()
          }
        },
      )
      .flatMap { Stream.of(*it.results) }
      .map { getPlaceDetails(it.placeId) }
  }

  private fun getPlaceDetails(placeId: String) = placeRepository.findByPlaceId(placeId)
}

data class GoogleApiProperties(
  val apiKey: String = System.getenv("GOOGLE_API_KEY") ?: "",
)
