# Natrium

[![Maven Central](https://img.shields.io/maven-central/v/schwarz.opensource.natrium/natrium-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/schwarz.opensource.natrium/natrium-core)

Natrium is a Kotlin Multiplatform library that wraps [Wire's Kalium SDK](https://github.com/wireapp/kalium) to provide a simplified API for secure citizen-to-government communication.

Natrium is **not** a rewrite of Kalium. It is a thin facade that exposes only what partners need to build secure messaging clients.

## Project Structure

```
natrium/
  natrium-core/     KMP library (the SDK) — see natrium-core/docs/ for SDK documentation
  natrium-cli/      JVM CLI app (Clikt), reference implementation
```

## Targets

- **JVM** (desktop / server)
- **Android** (minSdk 26)
- **iOS** (arm64 + simulator)

All shared code lives in `natrium-core/src/commonMain/` and must compile for all three targets.

## Building

Prerequisites: JDK 17+, Android SDK (compileSdk 36).

```bash
# Build all targets
./gradlew build
```

## Usage

Add `natrium-core` as a dependency from Maven Central:

```kotlin
// build.gradle.kts
dependencies {
    implementation("schwarz.opensource.natrium:natrium-core:<version>")
}
```

See [natrium-core/README.md](natrium-core/README.md) for API usage and code examples.

## Documentation

- [Getting Started](natrium-core/docs/getting-started.md) — install, initialize, first login
- [API Reference](natrium-core/docs/api-reference.md) — full public-type and method listing

See [`natrium-core/README.md`](natrium-core/README.md) for the complete documentation index and design overview.

## License

This project is licensed under the GNU General Public License v3.0.
See [LICENSE](LICENSE) for details.
