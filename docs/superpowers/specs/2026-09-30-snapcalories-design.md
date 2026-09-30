# SnapCalories — Design Spec

Date: 2026-09-30
Status: Approved in brainstorming, pending written-spec review

## 1. Purpose

A personal Android calorie tracker: photograph a meal, let an AI estimate its calories, confirm or correct the estimate, and see daily totals for the last 30 days.

The project is also a **self-learning exercise**. The developer writes all code themselves; Claude guides step by step (plain-English explanation → hints → developer codes → Claude reviews). Understanding each piece matters more than shipping speed.

### Success criteria

- Take a photo, receive an AI estimate (food name + kcal), edit it, save it.
- Today screen shows today's entries and running total, updating live.
- History screen lists past days (last 30) with totals; tapping a day shows its entries.
- Entries older than 30 days are deleted automatically.
- The developer can explain why each layer and each Hilt binding exists.

### Out of scope (YAGNI)

- Accounts, login, cloud sync, multiple users
- Daily calorie goal / progress bar
- Portion multiplier on the Review screen
- Storing photos or thumbnails after save
- Macros (protein/carbs/fat), barcode scanning
- Publishing to the Play Store (the API key is embedded in the APK — acceptable only for personal use)

## 2. Decisions

| Topic | Decision |
|---|---|
| Retention | Rolling 30 days per entry (entry deleted 30 days after `createdAt`) |
| After AI estimate | Review screen: editable name + kcal, Save / Retake. Manual entry always possible |
| Screens | Today, Review, History, Day detail |
| Camera | System camera via `ActivityResultContracts.TakePicture` + `FileProvider` (no CameraX, no CAMERA permission) |
| AI | Google Gemini (Flash-class model, free tier) called over REST with Retrofit + OkHttp + kotlinx.serialization |
| API key | `GEMINI_API_KEY` in `local.properties` → `BuildConfig` → OkHttp interceptor header `x-goog-api-key` |
| Architecture | MVI with Orbit, using a Contract / Event / Effect pattern |
| DI | Hilt with KSP |
| Persistence | Room |
| Background work | WorkManager with `@HiltWorker` |
| Modules | Single `app` module, layered packages |
| minSdk | Raised 24 → 26 (native `java.time`, no desugaring) |
| UI | Latest stable Jetpack Compose (BOM) + Material 3, Navigation Compose with type-safe `@Serializable` routes |

Exact library versions are chosen (latest stable) during implementation, not fixed here.

## 3. Package structure

```
com.example.snapcalories/
├── SnapCaloriesApp.kt        @HiltAndroidApp, WorkManager Configuration.Provider, schedules cleanup
├── MainActivity.kt           @AndroidEntryPoint, hosts NavHost
├── di/                       NetworkModule, DatabaseModule, RepositoryModule, CoreModule
├── data/
│   ├── local/                AppDatabase, FoodEntryEntity, FoodEntryDao, DayTotalRow
│   ├── remote/
│   │   ├── api/              GeminiApi (Retrofit)
│   │   └── dto/              request/, response/ (@Serializable)
│   └── repository/           FoodEntryRepositoryImpl, CalorieRepositoryImpl
├── domain/
│   ├── model/                FoodEntry, DailyTotal, CalorieEstimate, AnalysisError
│   ├── repository/           FoodEntryRepository, CalorieRepository (interfaces)
│   └── usecase/              ObserveEntriesForDay, ObserveDailyTotals, SaveFoodEntry,
│                             DeleteFoodEntry, PurgeOldEntries, AnalyzeFoodPhoto
├── ui/
│   ├── base/                 OrbitViewModel, UiState, UiEvent, UiSideEffect
│   ├── theme/
│   ├── navigation/           routes + NavHost
│   ├── today/                TodayContract, TodayViewModel, TodayScreen
│   ├── review/               ReviewContract, ReviewViewModel, ReviewScreen
│   ├── history/              HistoryContract, HistoryViewModel, HistoryScreen
│   └── daydetail/            DayDetailContract, DayDetailViewModel, DayDetailScreen
└── worker/                   PurgeOldEntriesWorker
```

## 4. UI layer (MVI)

### 4.1 Base class

SnapCalories defines a minimal Orbit base in `ui/base/`:

- Marker interfaces `UiState`, `UiEvent`, `UiSideEffect`.
- `abstract class OrbitViewModel<S : UiState, E : UiEvent, F : UiSideEffect>(initialState: S) : ViewModel(), ContainerHost<S, F>`
  - `container = container(initialState)`
  - `abstract fun onEvent(event: E)`: the single entry point for screen actions
  - `protected fun setState(reducer: S.() -> S)`: wraps Orbit `intent { reduce { ... } }`
  - `protected fun postEffect(effect: F)`: wraps Orbit `postSideEffect`
- Screens collect with Orbit's Compose helpers (`collectAsState()`, `collectSideEffect { }`) and receive a single `onEvent: (Event) -> Unit` callback.

### 4.2 Contracts

Each screen has a `XxxContract` class containing `data class State : UiState`, `sealed interface Event : UiEvent`, `sealed interface Effect : UiSideEffect`.

| Screen | State | Events | Effects |
|---|---|---|---|
| Today | `entries: List<FoodEntry>`, `totalCalories: Int`, `isLoading` | `CameraClicked`, `PhotoCaptured(path)`, `PhotoCancelled`, `DeleteEntry(id)`, `HistoryClicked` | `LaunchCamera(path)`, `NavigateToReview(path)`, `NavigateToHistory` |
| Review | `photoPath`, `isAnalyzing`, `foodName`, `caloriesText`, `error: AnalysisError?`, `isSaving`, `canSave` | `NameChanged`, `CaloriesChanged`, `SaveClicked`, `RetakeClicked`, `PhotoRetaken`, `RetryClicked` | `NavigateBack`, `LaunchCamera(path)` |
| History | `days: List<DailyTotal>`, `isLoading` | `DayClicked(epochDay)` | `NavigateToDay(epochDay)` |
| Day detail | `date`, `entries`, `totalCalories` | `DeleteEntry(id)` | none |

`canSave` = name not blank and calories a positive integer. Review starts analysis in `init` using the `photoPath` nav argument. Retake launches the camera from Review itself (overwriting the same temp file), then `PhotoRetaken` restarts analysis — no round-trip through Today. The temp photo is deleted after save or when the ViewModel is cleared (`onCleared`).

### 4.3 Flow

```
Today ──CameraClicked──► (create temp file in cacheDir) ──LaunchCamera──► system camera
      ◄──PhotoCaptured──                                    
      ──NavigateToReview(path)──► Review ──SaveClicked──► insert ──NavigateBack──► Today (Flow updates list)
                                         ──RetakeClicked──► LaunchCamera ──PhotoRetaken──► re-analyze
Today ──HistoryClicked──► History ──DayClicked──► Day detail
```

### 4.4 Navigation

Navigation Compose with type-safe routes: `@Serializable object Today`, `@Serializable data class Review(val photoPath: String)`, `@Serializable object History`, `@Serializable data class DayDetail(val epochDay: Long)`. ViewModels read args from `SavedStateHandle.toRoute<...>()`.

## 5. Data layer

### 5.1 Room

Table `food_entries`:

| Column | Type | Notes |
|---|---|---|
| `id` | Long, PK autoGenerate | |
| `foodName` | String | |
| `calories` | Int | |
| `createdAt` | Long | epoch millis; drives the 30-day purge |
| `epochDay` | Long | `LocalDate` in device zone at save time; drives grouping; indexed |

DAO:
- `observeByDay(epochDay: Long): Flow<List<FoodEntryEntity>>`, ordered by `createdAt` desc
- `observeDailyTotals(sinceMillis: Long): Flow<List<DayTotalRow>>`: `SELECT epochDay, SUM(calories) ... WHERE createdAt >= :sinceMillis GROUP BY epochDay ORDER BY epochDay DESC`
- `insert(entity)`, `deleteById(id)`, `deleteOlderThan(cutoffMillis): Int`

### 5.2 Repositories

- `FoodEntryRepository`: observe day, observe daily totals (last 30 days), save, delete, purge older than. Maps entities ↔ domain models.
- `CalorieRepository.analyze(photoPath): Result<CalorieEstimate>`, where the failure is an `AnalysisError`.

### 5.3 Use cases

One class per action with `operator fun invoke`. They use an injected `java.time.Clock` for "today" and "30 days ago", so tests are deterministic.

## 6. AI integration (Gemini REST)

- Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent`. The model name lives in one constant. Current model name and free-tier limits are verified against Google's docs at implementation time.
- Image prep: decode → downscale longest side to ~1024 px → JPEG (~80%) → Base64. Done off the main thread on the IO dispatcher.
- Request: `contents[0].parts = [ {text: prompt}, {inline_data: {mime_type: "image/jpeg", data}} ]`, and `generationConfig` with `responseMimeType = "application/json"` plus a `responseSchema` for `{ foodName: string, calories: integer, isFood: boolean }`.
- Response: take `candidates[0].content.parts[0].text` and decode it as JSON into `EstimateDto`, then map to `CalorieEstimate`.
- Errors → `AnalysisError`:

| Cause | AnalysisError | UI message | Actions |
|---|---|---|---|
| IOException / timeout | `Network` | "Couldn't reach the AI" | Retry, manual |
| HTTP 429 | `QuotaExceeded` | "Daily AI limit reached" | Manual |
| HTTP 400/401/403 | `InvalidApiKey` | "API key problem — check local.properties" | Manual |
| `isFood = false` | `NotFood` | "That doesn't look like food" | Retake, manual |
| Parse failure / empty candidates / other | `Unknown` | "Couldn't read the AI's answer" | Retry, manual |

Name and calorie fields stay editable in every state.

## 7. Dependency injection (Hilt + KSP)

| Module | Provides |
|---|---|
| `NetworkModule` | `Json` (ignoreUnknownKeys), `OkHttpClient` (API-key interceptor, logging interceptor in debug, timeouts), `Retrofit` (kotlinx.serialization converter), `GeminiApi` |
| `DatabaseModule` | `AppDatabase` (singleton), `FoodEntryDao` |
| `RepositoryModule` | `@Binds` for `FoodEntryRepository` and `CalorieRepository` |
| `CoreModule` | `@IoDispatcher CoroutineDispatcher`, `Clock.systemDefaultZone()` |

Entry points: `@HiltAndroidApp SnapCaloriesApp`, `@AndroidEntryPoint MainActivity`, `@HiltViewModel` ViewModels, `@HiltWorker` worker.

## 8. 30-day cleanup

- `PurgeOldEntriesWorker` (`@HiltWorker`, `CoroutineWorker`) calls `PurgeOldEntries`, which deletes rows with `createdAt < now − 30 days`.
- Scheduled in `SnapCaloriesApp.onCreate()` as unique periodic work (24 h, `ExistingPeriodicWorkPolicy.KEEP`), plus a unique one-time run on each app start.
- Hilt-WorkManager wiring: the Application implements `Configuration.Provider` with an injected `HiltWorkerFactory`, and the manifest removes the default `WorkManagerInitializer` from `androidx.startup`.
- Safety net: `observeDailyTotals` filters to the last 30 days, so stale rows are never shown even if the worker is delayed.

## 9. Testing

| Target | Tooling |
|---|---|
| ViewModels | orbit-test + fake repositories (in-memory) + `kotlinx-coroutines-test` |
| Use cases (purge cutoff, today's epochDay) | JUnit + fixed `Clock` |
| `CalorieRepositoryImpl` (DTO parsing, error mapping) | OkHttp MockWebServer |
| `FoodEntryDao` (grouping query, purge) | One instrumented test with in-memory Room |

Each build step ends with its tests.

## 10. Build order (learning steps)

Hint depth decreases over time: 🟢 detailed, 🟡 medium, 🟠 light, 🔴 goal + checklist.

1. 🟢 Gradle: Compose BOM, Kotlin serialization, KSP, Hilt in version catalog; minSdk 26; "Hello" Compose `MainActivity`.
2. 🟢 Hilt skeleton: `SnapCaloriesApp`, `@AndroidEntryPoint`, first injected dependency.
3. 🟢 `OrbitViewModel` base + Today screen with hard-coded entries (full MVI loop).
4. 🟡 Room + repository + use cases; Today shows real data (temporary "add test entry").
5. 🟡 Navigation + History + Day detail.
6. 🟡 Camera + FileProvider; Review screen with manual entry and Save.
7. 🟠 Gemini: Retrofit, DTOs, interceptor, image prep, error mapping; Review auto-fills.
8. 🟠 Cleanup worker + app-start purge.
9. 🔴 Polish: empty states, loading indicators, delete confirmation.

Per-step rhythm: explain → hints → developer codes → Claude reviews → fixes → commit → update `CLAUDE.md`.
