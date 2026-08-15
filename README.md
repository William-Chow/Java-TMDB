# Java-TMDB

A small TMDB movie browser for Android — popular movies, search, and a detail
screen.

> **The name is historical.** This started as a Java app, but the source is now
> **100 % Kotlin** with a Jetpack Compose UI. Not a single `.java` file remains.

## Stack

| | |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (`compose-bom`) |
| Networking | Retrofit 3 + OkHttp logging interceptor |
| JSON | Gson (`@SerializedName`) |
| Images | Coil (`AsyncImage`) |
| Ads | AdMob banner (`play-services-ads`) |
| Build | AGP 8.9.1 / Gradle 9.4.1, version catalog in `gradle/libs.versions.toml` |
| SDK | `minSdk` 28, `compileSdk`/`targetSdk` 36 |

## Source layout

```
app/src/main/java/com/movie/tmdb/app/
  model/     Movie, Result           — Gson data classes
  network/   APIClient, APIInterface — Retrofit setup + endpoint definitions
  ui/        MainActivity, ViewActivity — Compose screens
  util/      Utils                   — connectivity check, image base URL
```

Model properties are camelCase; the TMDB wire format is mapped with
`@SerializedName`, so renaming a property never changes the parsed JSON.

## Screens

| Screen | Endpoint | Notes |
|---|---|---|
| **MainActivity** | `movie/popular`, `search/movie` | 2-column poster grid; the search field debounces 300 ms and falls back to the popular list below 2 characters. Double-tap back to exit. |
| **ViewActivity** | `movie/{movie_id}` | Title, overview, poster. Launched with the `movie_id` intent extra. |

Both screens show a snackbar on network failure and gate their initial request
on `Utils.getConnectionType`.

## Build

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

Or open the project in Android Studio and run the `app` configuration.

## Release

Release builds are minified with R8 and signed from `key.properties`
(git-ignored) in the project root:

```properties
storeFile=app/keystore/your-keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

If `key.properties` is absent, `app/build.gradle` leaves the `release` signing
config unpopulated — build a release variant on a fresh clone only after adding
it.

ProGuard keeps `com.movie.tmdb.app.{network,util,model}` — the model package
matters because Gson reflects over it.

## Notes

- The TMDB API key is currently **hardcoded** in `network/APIClient.kt` and is
  already in the git history. Move it to `local.properties` + `BuildConfig` if
  this repo ever matters; note that even then the key ships inside the APK, so
  a genuinely secret key needs a backend proxy.
- `glide`, `firebase-bom`, and `app-update` are declared in `app/build.gradle`
  but not referenced anywhere in the source — leftovers from the pre-Compose
  version, safe to drop.
- `res/layout/` and `res/menu/` are empty for the same reason.
- A Kotlin Multiplatform sibling of this app (Android + iOS, Compose
  Multiplatform, Ktor) lives in **TMDBProject**.
