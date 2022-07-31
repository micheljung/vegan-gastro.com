val bootstrapVersion = "5.1.3"
val commonsEmailVersion = "1.5"
val exposedVersion = "0.38.2"
val googleMapsVersion = "2.1.0"
val h2databaseVersion = "2.1.214"
val koinKspVersion = "1.0.1"
val koinVersion = "3.2.0"
val kotlinVersion = "1.6.21"
val kotlinxHtmlVersion = "0.7.5"
val kotlinWrappersVersion = "1.0.0-pre.360"
val kotlinCssVersion = "1.0.0-pre.332-kotlin-1.6.21"
val ktorVersion = "2.0.2"
val logbackVersion = "1.2.11"
val prometheusVersion = "1.9.0"
val mjml4jClientVersion = "1.0.0"

plugins {
  application
  kotlin("jvm") version "1.6.21"
  id("com.google.devtools.ksp") version "1.6.21-1.0.5"
}

group = "com.vegangastro"
version = "0.0.1"
application {
  mainClass.set("ch.micheljung.ApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
  mavenCentral()
  maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

sourceSets.main {
  java.srcDirs("build/generated/ksp/main/kotlin")
}

dependencies {
  implementation("com.google.maps:google-maps-services:$googleMapsVersion")
  implementation("com.h2database:h2:$h2databaseVersion")
  implementation("ch.qos.logback:logback-classic:$logbackVersion")
  implementation("io.camassia:mjml4j-client:$mjml4jClientVersion")
  implementation("io.insert-koin:koin-annotations:$koinKspVersion")
  implementation("io.insert-koin:koin-ktor:$koinVersion")
  implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
  ksp("io.insert-koin:koin-ksp-compiler:$koinKspVersion")
  implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-webjars:$ktorVersion")
  implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
  implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
  implementation("io.micrometer:micrometer-registry-prometheus:$prometheusVersion")
  implementation("org.apache.commons:commons-email:$commonsEmailVersion")
  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$kotlinCssVersion")
  implementation("org.webjars:bootstrap:$bootstrapVersion")

  // Test dependencies
  testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}