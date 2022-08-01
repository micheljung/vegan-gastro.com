package com.vegangastro

import com.google.maps.GeoApiContext
import com.vegangastro.db.DbProperties
import com.vegangastro.email.EmailProperties
import com.vegangastro.email.MjmlProperties
import com.vegangastro.job.JobTable
import com.vegangastro.place.GoogleApiProperties
import com.vegangastro.place.PlaceTable
import com.vegangastro.plugins.configureMonitoring
import com.vegangastro.plugins.configureRouting
import com.vegangastro.plugins.configureTemplating
import io.camassia.mjml.MJMLClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.webjars.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.KoinApplication
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", watchPaths = listOf("classes", "resources")) {
        install(Webjars)
        install(WebSockets) {
            timeout = Duration.ofSeconds(15)
            pingPeriod = Duration.ofSeconds(5)
        }
        // TODO try this
//    install(Koin)
        val koin = startKoin {
            modules(AppModule().module)
            // Needed for auto reload to work
            properties(mapOf("application" to this))
        }
        runBlocking {
            initDatabase(koin)
        }
        configureRouting()
        configureMonitoring()
        configureTemplating()
    }.start(wait = true)
}

private suspend fun initDatabase(koin: KoinApplication) {
    val properties = koin.koin.get<DbProperties>()
    Database.connect(
        url = properties.url, driver = properties.driver, user = properties.user, password = properties.password,
    )

    newSuspendedTransaction {
//    addLogger(StdOutSqlLogger)
        SchemaUtils.create(PlaceTable, JobTable)
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
