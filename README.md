# Natrium

Natrium is a Kotlin Multiplatform library that wraps [Wire's Kalium SDK](https://github.com/wireapp/kalium) to provide a simplified API for secure citizen-to-government communication.

Natrium is **not** a rewrite of Kalium. It is a thin facade that exposes only what partners need to build secure messaging clients.

## Project Structure

```
natrium/
  natrium-core/     KMP library (the SDK)
  natrium-cli/      JVM CLI app (Clikt), reference implementation
  kalium/           Kalium Git submodule (do not edit)
  docs/             Architecture documentation
```

## Targets

- **JVM** (desktop / server)
- **Android** (minSdk 26)
- **iOS** (arm64 + simulator)

All shared code lives in `natrium-core/src/commonMain/` and must compile for all three targets.

## Building

Prerequisites: JDK 17+, Android SDK (compileSdk 36).

```bash
# Initialize the Kalium submodule
git submodule update --init --recursive

# Build all targets
./gradlew build

```

To Update the Submodule
```bash
git submodule update --remote kalium
```

Kalium is included as a composite build via `includeBuild("kalium")` in `settings.gradle.kts`.

## Documentation

- [API Reference](docs/api-reference.md) -- all public types and methods

## License

This project is licensed under the GNU General Public License v3.0.
See [LICENSE](LICENSE) for details.
