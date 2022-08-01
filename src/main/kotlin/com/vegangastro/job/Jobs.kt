package com.vegangastro.job

import com.vegangastro.email.WebsiteScraper
import com.vegangastro.place.Place
import com.vegangastro.place.PlaceProvider
import com.vegangastro.place.PlaceRepository
import com.vegangastro.plugins.Connection
import com.vegangastro.plugins.needsReview
import com.vegangastro.serialization.InstantSerializer
import com.vegangastro.serialization.Msg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

@Single
class JobService : KoinComponent {
  private val placeProvider: PlaceProvider by inject()
  private val websiteScraper: WebsiteScraper by inject()
  private val placeRepository: PlaceRepository by inject()
  private val jobRepository: JobRepository by inject()

  private val coroutineScope = CoroutineScope(Executors.newFixedThreadPool(60).asCoroutineDispatcher())

  suspend fun submit(
    search: PlacesSearch,
    listener: suspend (Msg) -> Unit,
  ) {
    var job = jobRepository.save(Job(country = search.country, city = search.city))
    placeProvider.getPlaces(job.country, job.city)
      .filter { !hasBeenContacted(it) }
      .map {
        coroutineScope.async {
          if (it.email != null || it.website == null) {
            it
          } else {
            listener(PlaceStatus(it, PlaceStatus.Status.SCRAPING))

            websiteScraper.scrape(it.website).let { info ->
              placeRepository.save(it.copy(email = info.email, locale = info.locale, needsReview = needsReview(info)))
            }
          }
        }
      }
      .toList()
      .awaitAll()
      .onEach {
        job.places += it
        job = jobRepository.save(job)
        if (it.sent != null)
          listener(PlaceStatus(it, PlaceStatus.Status.CONTACTED))
        else
          listener(PlaceStatus(it, PlaceStatus.Status.SCRAPED))
      }

    job = job.copy(finishedAt = Instant.now())
    jobRepository.save(job)
    listener(SearchDone())
  }

  private fun hasBeenContacted(it: Place) = it.sent != null
}

@Single
class JobRepository : KoinComponent {
  private val jobEventPublisher by inject<JobWebSocketEventPublisher>()

  suspend fun findAll() =
    newSuspendedTransaction {
      JobTable.selectAll().map { Job(it[JobTable.id].value, it[JobTable.country], it[JobTable.city]) }
    }

  suspend fun save(job: Job): Job {
    return newSuspendedTransaction {
      val id = if (job.id == null) {
        JobTable.insertAndGetId {
          it[processed] = job.places.size
          it[country] = job.country
          it[city] = job.city
        }.value
      } else {
        JobTable.update {
          it[processed] = job.places.size
          it[country] = job.country
          it[city] = job.city
        }
        job.id
      }

      val updatedJob = job.copy(id = id)
      jobEventPublisher.publish(updatedJob)
      updatedJob
    }
  }
}

@Single
class JobWebSocketEventPublisher : KoinComponent {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())
  private val jobRepository by inject<JobRepository>()

  suspend fun subscribe(connection: Connection) {
    connections += connection
    logger.debug("Connection subscribed: $connection")
    jobRepository.findAll().forEach { sendToConnection(it, connection) }
  }

  suspend fun publish(job: Job) {
//    connections.forEach { sendToConnection(job, it) }
  }

  private suspend fun sendToConnection(job: Job, it: Connection) {
    it.send(job)
  }

  fun unsubscribe(connection: Connection) {
    connections -= connection
    logger.debug("Connection unsubscribed: $connection")
  }
}

@Serializable
data class PlacesSearch(
  val country: String,
  val city: String,
) : Msg() {
  override val type_: String
    get() = MSG_TYPE

  companion object {
    const val MSG_TYPE = "search"
  }
}

@Serializable
data class ContactPlace(
  val place: Place,
) : Msg() {
  override val type_: String
    get() = MSG_TYPE

  companion object {
    const val MSG_TYPE = "contactPlace"
  }
}

@Serializable
data class PlaceStatus(
  val place: Place,
  val status: Status,
) : Msg() {
  override val type_: String
    get() = MSG_TYPE

  companion object {
    const val MSG_TYPE = "placeStatus"
  }

  enum class Status {
    SCRAPING, SCRAPED, CONTACTED
  }
}

@Serializable
data class SearchDone(
  val done: Boolean = true,
) : Msg() {
  override val type_: String
    get() = MSG_TYPE

  companion object {
    const val MSG_TYPE = "searchDone"
  }
}

@Serializable
data class Job(
  val id: Int? = null,
  val country: String,
  val city: String,
  val places: MutableList<Place> = mutableListOf(),
  @Serializable(with = InstantSerializer::class) val finishedAt: Instant? = null,
) : Msg() {
  override val type_: String
    get() = MSG_TYPE

  companion object {
    const val MSG_TYPE = "job"
  }
}

object JobTable : IntIdTable("job") {
  val processed = integer("processed").default(0)
  val country = varchar("country", 255)
  val city = varchar("city", 255)
}