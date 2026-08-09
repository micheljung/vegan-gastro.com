val ktorVersion = "3.5.1"
val kotlinVersion = "2.4.10"
val logbackVersion = "1.5.18"
val prometeusVersion = "1.15.7"
val kotlinxHtmlVersion = "0.11.0"

plugins {
  application
  kotlin("jvm") version "2.4.10"
}

group = "com.vegangastro"
version = "0.0.1"
application {
  mainClass.set("com.vegangastro.ApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
  jvmToolchain(21)
}

repositories {
  mavenCentral()
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
  testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
