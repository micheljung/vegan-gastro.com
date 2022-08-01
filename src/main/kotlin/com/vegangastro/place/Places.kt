package com.vegangastro.place

import com.vegangastro.plugins.Connection
import com.vegangastro.serialization.Msg
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.util.*


@Single
class PlaceWebSocketEventPublisher : KoinComponent {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())
  private val placeRepository by inject<PlaceRepository>()

  suspend fun subscribe(connection: Connection) {
    connections += connection
    logger.debug("Connection subscribed: $connection")
//    placeRepository.findAll().forEach { sendToConnection(it, connection) }
  }

  suspend fun publish(place: Place) {
    connections.forEach { sendToConnection(place, it) }
  }

  private suspend fun sendToConnection(place: Place, it: Connection) {
    it.send(place)
  }

  fun unsubscribe(connection: Connection) {
    connections -= connection
    logger.debug("Connection unsubscribed: $connection")
  }
}

@Single
class PlacesSummaryService : KoinComponent {
  private val placeRepository by inject<PlaceRepository>()
  fun send(connection: Connection) {

  }

  suspend fun getSummary() = newSuspendedTransaction {
    PlacesSummary(
      PlaceTable.slice(PlaceTable.id).select { PlaceTable.sent neq null }.count().toInt(),
      PlaceTable.slice(PlaceTable.id).select { PlaceTable.readConfirmed eq true }.count().toInt(),
      PlaceTable.slice(PlaceTable.id).select { PlaceTable.needsReview eq true }.count().toInt(),
      PlaceTable.slice(intLiteral(1)).select { PlaceTable.sent neq null }.groupBy(PlaceTable.country).count().toInt(),
    )
  }
}

@Serializable
data class PlacesSummary(
  val contacted: Int,
  val reacted: Int,
  val needReview: Int,
  val numCountries: Int,
) : Msg() {
  override val type_: String
    get() = MSG_TYPE

  companion object {
    const val MSG_TYPE = "summary"
  }
}