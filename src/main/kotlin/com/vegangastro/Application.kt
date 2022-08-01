package com.vegangastro

import com.google.maps.GeoApiContext
import com.vegangastro.db.DbProperties
import com.vegangastro.email.EmailProperties
import com.vegangastro.email.MjmlProperties
import com.vegangastro.places.GoogleApiProperties
import com.vegangastro.places.PlaceTable
import com.vegangastro.plugins.configureMonitoring
import com.vegangastro.plugins.configureRouting
import com.vegangastro.plugins.configureTemplating
import io.camassia.mjml.MJMLClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.webjars.*
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinApplication
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0", watchPaths = listOf("classes", "resources")) {
    install(Webjars)
    val koin = startKoin {
      modules(AppModule().module)
      // Needed for auto reload to work
      properties(mapOf("application" to this))
    }
    initDatabase(koin)
    configureRouting()
    configureMonitoring()
    configureTemplating()
  }.start(wait = true)
}

private fun initDatabase(koin: KoinApplication) {
  val properties = koin.koin.get<DbProperties>()
  Database.connect(
    url = properties.url, driver = properties.driver, user = properties.user, password = properties.password,
  )

  transaction {
//    addLogger(StdOutSqlLogger)
    SchemaUtils.create(PlaceTable)
  }
}

@Module
@ComponentScan("com.vegangastro")
class AppModule {

  @Single
  fun emailProperties() = EmailProperties("localhost", "user", "password")

  @Single
  fun mjmlProperties() = MjmlProperties()

  @Single
  fun googleApiProperties() = GoogleApiProperties()

  @Single
  fun dbProperties() = DbProperties()

  @Single
  fun okHttpClient() = OkHttpClient()

//  @Single
//  fun mjmlClient(mjmlProperties: MjmlProperties): MJMLClient = MJMLClient.newClient()
//    .withConfiguration(object : MJMLDefaultConfiguration("", "") {
//      override fun host() = "http://${mjmlProperties.host}:${mjmlProperties.port}"
//    })

  @Single
  fun mjmlClient(properties: MjmlProperties): MJMLClient = MJMLClient.newDefaultClient()
    .withApplicationID(properties.applicationId)
    .withApplicationKey(properties.applicationKey)

  @Single
  fun geoApiContext(properties: GoogleApiProperties): GeoApiContext = GeoApiContext.Builder()
    .apiKey(properties.apiKey)
    .build()
}
