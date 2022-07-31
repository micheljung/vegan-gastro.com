package com.vegangastro.places

import com.google.maps.GeoApiContext
import com.google.maps.PlacesApi
import com.google.maps.model.PlaceType
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.stream.Stream

@Single
class PlaceProvider : KoinComponent {
  private val geoApiContext by inject<GeoApiContext>()
  private val placeRepository by inject<PlaceRepository>()

  fun getPlaces(country: String): Stream<Place> {
    val initialResponse = PlacesApi.textSearchQuery(geoApiContext, PlaceType.RESTAURANT)
      .region(country)
      .await()

    return Stream
      .iterate(
        initialResponse,
        { t -> t.nextPageToken != null },
        { t -> Thread.sleep(2000); PlacesApi.textSearchNextPage(geoApiContext, t.nextPageToken).await() },
      )
      .flatMap { Stream.of(*it.results) }
      .map { getPlaceDetails(it.placeId) }
  }

  private fun getPlaceDetails(placeId: String) = placeRepository.findByPlaceId(placeId)
}

data class GoogleApiProperties(
  val apiKey: String = System.getenv("GOOGLE_API_KEY") ?: "",
)
