# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

A Ktor 3 (Kotlin/JVM) web server for vegan-gastro.com. The site lets visitors submit a
restaurant's e-mail address and language so the app can send that restaurant a templated
request to offer a vegan menu. Each address is meant to be contacted only once. The app is
early-stage: the landing page and a `/submit` handler exist, but the actual e-mail sending
and the "already contacted" deduplication are not yet implemented (`/submit` currently just
echoes the address back).

Stack: Ktor 3.5.x, Kotlin 2.4.x, Gradle 9.5.x (wrapper), micrometer + Prometheus, logback.
Requires JDK 21 (set via `kotlin { jvmToolchain(21) }` in `build.gradle.kts`).

## Commands

```bash
./gradlew build          # compile + test
./gradlew test           # run tests (no tests exist yet; test deps are wired up)
./gradlew test --tests "com.vegangastro.ApplicationTest"   # run a single test class
./gradlew run            # start the server on http://0.0.0.0:8080
```

Tests live in `src/test/kotlin/`. The test stack is `ktor-server-test-host` +
`kotlin-test-junit` — use `testApplication { }` for endpoint tests (see `ApplicationTest`,
which installs a `configure*` module and asserts against a `client.get(...)` response).

## Architecture

Standard Ktor plugin-module layout. `Application.kt` starts an embedded Netty server and
installs feature modules via `configure*` extension functions on `Application`, each living
in its own file under `plugins/`:

- `Routing.kt` — static assets served from `resources/static` at `/static`
- `Templating.kt` — HTML pages built with the `kotlinx.html` DSL (no template files); holds
  the `GET /` landing page and `POST /submit` form handler
- `Monitoring.kt` — Micrometer + Prometheus metrics exposed at `/metrics-micrometer`

To add a page or endpoint, add a `routing { }` block inside the relevant `configure*`
function (or create a new plugin module and call it from `Application.kt`). HTML is written
in Kotlin via `call.respondHtml { }`, not in separate template files.
