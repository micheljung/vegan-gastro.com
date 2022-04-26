val ktorVersion = "2.0.0"
val kotlinVersion = "1.6.21"
val logbackVersion = "1.2.11"
val prometeusVersion = "1.8.5"
val kotlinxHtmlVersion = "0.7.5"

plugins {
  application
  kotlin("jvm") version "1.6.21"
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

dependencies {
  implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
  implementation("io.micrometer:micrometer-registry-prometheus:$prometeusVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
  implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
  implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
  implementation("ch.qos.logback:logback-classic:$logbackVersion")
  testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}