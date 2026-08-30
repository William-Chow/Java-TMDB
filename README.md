# Java-TMDB

A small TMDB movie browser for Android — four browse lists, search, favorites
that survive going offline, and a detail screen with cast and trailers.

> **The name is historical.** This started as a Java app, but the source is now
> **100 % Kotlin** with a Jetpack Compose UI. Not a single `.java` file remains.

## Stack

| | |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (`compose-bom`) |
| Networking | Retrofit 3 (suspend) + OkHttp logging interceptor |
| JSON | Gson (`@SerializedName`) |
| Storage | Room 2.8 (KSP) — list cache + favorites |
| Images | Coil (`AsyncImage`) |
| Ads | AdMob banner (`play-services-ads`) |
| Build | AGP 8.9.1 / Gradle 9.4.1, Java 17, version catalog in `gradle/libs.versions.toml` |
| SDK | `minSdk` 28, `compileSdk`/`targetSdk` 36 |

## Source layout

```
app/src/main/java/com/movie/tmdb/app/
  model/     Movie, Result, Genre, Credits, Videos — Gson data classes
             MovieCategory                          — the four list endpoints
  network/   APIClient, APIInterface — Retrofit setup + suspend endpoint definitions
  data/      TmdbDatabase, Daos, Entities — Room
             MovieRepository              — network + cache, the ViewModels' only dependency
             ConnectivityChecker          — keeps Context out of the ViewModels
  ui/        MainActivity, ViewActivity           — Compose screens (thin; state lives in the ViewModels)
             MovieListViewModel, MovieDetailViewModel
             CommonUi, AdMobBanner                — shared composables
             theme/Theme.kt                       — Compose colour scheme (dynamic colour on API 31+)
  util/      Utils                   — connectivity check, image base URL
  TmdbApplication.kt                 — hand-rolled DI container
```

Model properties are camelCase; the TMDB wire format is mapped with
`@SerializedName`, so renaming a property never changes the parsed JSON.

## Screens

| Screen | Endpoint | Notes |
|---|---|---|
| **MainActivity** | `movie/{popular,now_playing,top_rated,upcoming}`, `search/movie` | Tabs for the four lists plus **Favorites**. 2-column poster grid, paged — the next page loads as you approach the end. Pull to refresh. Tap the heart on any poster to save it. The search field debounces 300 ms and falls back to the current list below 2 characters. Double-tap back to exit. |
| **ViewActivity** | `movie/{movie_id}` | Poster, title, tagline, release date, runtime, rating, genre chips, overview, cast row, trailer button and similar movies — all from one request via `append_to_response=credits,videos,similar`. Similar movies are tappable. Launched with the `movie_id` intent extra. |

Both screens hold their state in a `ViewModel`, so rotating the device does not
re-fetch or clear the list.

The four browse lists are **Room-backed**: rows paint from the cache first and
are refreshed from the network behind them, so the app opens with content while
offline. Search results are transient and never cached; favorites live in Room
and need no network at all.

Failures are reported two ways: with nothing on screen, a full-screen message
and a **Retry** button; with results already visible (a stale cache, or a failed
*next* page), a snackbar. Error text is carried through the state as a string
resource id, which is what keeps the ViewModels free of a `Context`.

Searches are cancellable — starting a new query cancels the in-flight call, so a
slow response for `ab` can never overwrite the results already shown for `abc`.

## Build

The TMDB API key is read from `local.properties` (git-ignored), so add this
before the first build:

```properties
tmdb.apiKey=your-tmdb-api-key
```

It reaches the code as `BuildConfig.TMDB_API_KEY`. Without it the build still
succeeds and every request comes back 401.

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

Or open the project in Android Studio and run the `app` configuration.

## Tests

`src/test` covers the ViewModels against fake DAOs and a fake `APIInterface`
(no Robolectric, no device): search debouncing and cancellation, paging, the
offline-with-and-without-cache split, favorites, tab switching, and Gson
mapping of the list and `append_to_response` payloads.

`src/androidTest` needs a device or emulator and covers the Room DAOs:

```bash
./gradlew :app:connectedDebugAndroidTest
```

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

- The TMDB API key now comes from `local.properties`, but the old hardcoded one
  is still in the git history — rotate it if this repo ever matters. Either way
  the key ships inside the APK, so a genuinely secret key needs a backend proxy.
- HTTP body logging is on in debug only; response bodies carry the api_key-bearing
  URL and have no business in a release logcat.
- A Kotlin Multiplatform sibling of this app (Android + iOS, Compose
  Multiplatform, Ktor) lives in **TMDBProject**.
