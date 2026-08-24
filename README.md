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

The Natrium SDK (the wrapper code in this repository) is licensed under the
**European Union Public Licence v. 1.2 only** (EUPL-1.2).
See [LICENSE](LICENSE) for the full licence text and [NOTICE](NOTICE) for
attribution.

### Kalium SDK — not licensed under the EUPL

Natrium depends on Wire’s [Kalium SDK](https://github.com/wireapp/kalium)
(currently the Maven artifact `schwarz.opensource.natrium:logic`). **No portion
of the Kalium SDK is licensed under the EUPL-1.2**, either as a standalone
component or as incorporated in Natrium.

The Kalium SDK may only be used under:

- the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html),
  as published by Wire; or
- a commercial licence obtained from Wire Swiss GmbH.

Linking or combining Natrium with the Kalium SDK does not cause any copyleft
effect of the EUPL-1.2 to extend to Kalium. Recipients who use Kalium without a
commercial licence from Wire remain subject to the GPL-3.0 and to the licences
of Kalium’s third-party dependencies.

Publication of Natrium under the EUPL-1.2 is authorised by a commercial licence
agreement between Schwarz Digits and Wire Swiss GmbH. That agreement does not
grant any Kalium rights to recipients of Natrium.
