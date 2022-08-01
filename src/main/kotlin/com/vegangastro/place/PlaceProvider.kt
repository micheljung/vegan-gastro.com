package com.vegangastro.place

import com.google.maps.GeoApiContext
import com.google.maps.PlacesApi
import com.google.maps.model.PlaceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Single
class PlaceProvider : KoinComponent {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val geoApiContext by inject<GeoApiContext>()
  private val placeRepository by inject<PlaceRepository>()

  suspend fun getPlaces(country: String, city: String): Flow<Place> {
    logger.info("Fetching from Places API")

    val initialResponse = withContext(Dispatchers.IO) {
      PlacesApi.textSearchQuery(geoApiContext, PlaceType.RESTAURANT)
        .region(country)
        .query(city)
        .await()
    }

    val lastQueryTime = AtomicReference(Instant.now())

    return flow {
      var response = initialResponse
      while (response.results.isNotEmpty()) {
        response.results.forEach { emit(getPlaceDetails(it.placeId)) }
        if (response.nextPageToken == null) {
          break
        }
        val sleepTime =
          Duration.between(Instant.now(), lastQueryTime.get().plusSeconds(3)).toMillis().coerceAtLeast(0)
        delay(sleepTime)
        lastQueryTime.set(Instant.now())
        logger.info("Fetching next page from Places API")
        withContext(Dispatchers.IO) {
          response = PlacesApi.textSearchNextPage(geoApiContext, response.nextPageToken).await()
        }
      }
    }
  }

  private suspend fun getPlaceDetails(placeId: String) =
    placeRepository.findByPlaceId(placeId) ?: error("Could not find place for placeId: $placeId")
}

data class GoogleApiProperties(
  val apiKey: String = System.getenv("GOOGLE_API_KEY") ?: "",
)
