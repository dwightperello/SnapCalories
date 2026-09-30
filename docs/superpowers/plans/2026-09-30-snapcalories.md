# SnapCalories Implementation Plan

> **Execution mode: guided self-learning.** The developer writes all product code. For every task Claude: (1) explains the concepts in plain English, (2) gives hints at the task's hint level, (3) waits for the developer to code it, (4) reviews the code, (5) runs the verification, (6) commits, (7) updates `CLAUDE.md`. Claude does not write product code unless asked. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Personal Android app: photograph food → Gemini estimates kcal → review/edit → save; Today, History, Day detail screens; entries auto-deleted 30 days after creation.

**Architecture:** Single `app` module, layered packages (`data` / `domain` / `ui` / `di` / `worker`). MVI with Orbit through a self-written `OrbitViewModel` base using a Contract / Event / Effect pattern. Hilt (KSP) wires everything; Room stores entries; Retrofit calls Gemini REST; WorkManager purges old rows.

**Tech Stack:** Kotlin (AGP 9 built-in Kotlin), Compose BOM + Material 3, Navigation Compose (type-safe), Orbit MVI, Hilt + KSP, Room, Retrofit + OkHttp + kotlinx.serialization, WorkManager, JUnit4, orbit-test, MockWebServer.

**Spec:** `docs/superpowers/specs/2026-09-30-snapcalories-design.md`

## Pacing

Each task is split into **sessions** (≈30–60 min each), so one evening = one session. Every session ends with a building app and a commit, so you can stop at any session boundary. To resume, say "continue SnapCalories" and Claude reads `CLAUDE.md` (which records the current task and session).

## Versions (checked 2026-09-30, latest stable)

| Catalog key | Version | Notes |
|---|---|---|
| `kotlin` | 2.4.20 | Compose-compiler + serialization plugins use this; overrides AGP 9.4.1's bundled KGP 2.2.10 |
| `ksp` | 2.3.12 | KSP2 versioning is independent of Kotlin |
| `composeBom` | 2026.09.00 | |
| `activityCompose` | 1.13.0 | |
| `lifecycle` | 2.11.0 | `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` |
| `navigationCompose` | 2.10.2 | |
| `hilt` | 2.60.1 | `hilt-android`, `hilt-compiler`, plugin `com.google.dagger.hilt.android` |
| `androidxHilt` | 1.4.0 | `hilt-navigation-compose`, `hilt-work`, `androidx.hilt:hilt-compiler` |
| `orbit` | 12.0.1 | `orbit-viewmodel`, `orbit-compose`, `orbit-test` |
| `room` | 2.8.5 | `room-runtime`, `room-ktx`, `room-compiler` (KSP), `room-testing` |
| `work` | 2.12.0 | `work-runtime-ktx` |
| `retrofit` | 3.0.0 | `retrofit`, `converter-kotlinx-serialization` |
| `okhttp` | 5.5.0 | `okhttp`, `logging-interceptor`, `mockwebserver` |
| `kotlinxSerialization` | 1.11.0 | `kotlinx-serialization-json` |
| `coroutines` | 1.11.0 | `kotlinx-coroutines-test` |

If a version combination fails to sync in Task 1, fix it there (that's part of the lesson) and record the working set in `CLAUDE.md`.

## Global Constraints

- Package `com.example.snapcalories`; single module `app`; minSdk **26**, target/compileSdk 37, JVM target 11.
- All dependencies go through `gradle/libs.versions.toml`, never raw coordinates in `build.gradle.kts`.
- Retention: an entry is deleted once `createdAt < now − 30 days` (rolling, per entry).
- `epochDay` = `LocalDate` of `createdAt` in the injected `Clock`'s zone.
- Photos are never kept: the temp file lives in `cacheDir` and is deleted after save or when Review's ViewModel is cleared.
- API key: `GEMINI_API_KEY` in `local.properties` → `BuildConfig.GEMINI_API_KEY` → header `x-goog-api-key`. Never commit it.
- Every "now" and "today" comes from the injected `java.time.Clock`, never `System.currentTimeMillis()` / `LocalDate.now()` without a clock.
- Background work runs on the injected `@IoDispatcher`.
- ViewModels and use cases never touch `android.net.Uri`, `Context`, or `Bitmap`, so they stay testable on the plain JVM. Screens convert `File` → `Uri`.
- Screens receive `state` + `onEvent: (Event) -> Unit`. Stateless `XxxContent` composables are separate from the `XxxScreen` that holds the ViewModel, so they can be previewed.

## Review Focus

1. **Midnight / timezone:** an entry saved at 23:59 local belongs to that day, not the next. Covered by the `SaveFoodEntryTest` zone-boundary test (Task 4).
2. **Bad calorie input** (`""`, `"abc"`, `"0"`, `"-5"`, `"99999999999"`): Save stays disabled, no crash. Covered by `ReviewViewModelTest` (Task 6).
3. **Camera cancelled** (TakePicture returns `false`): stays on Today, no navigation, and the temp file is deleted. Covered by `TodayViewModelTest` (Task 6).
4. **Malformed AI reply** (empty `candidates`, non-JSON text, negative calories): `AnalysisError.Unknown`, fields stay editable. Covered by `CalorieRepositoryImplTest` (Task 7).
5. **Huge photos** (12–50 MP): no OutOfMemoryError. Decode with `inSampleSize` before scaling. Covered by `ImageEncoderTest` (Task 7).

---

## File map

```
app/src/main/java/com/example/snapcalories/
  SnapCaloriesApp.kt                     T2, T8
  MainActivity.kt                        T1, T3, T5
  di/CoreModule.kt                       T2
  di/DatabaseModule.kt                   T4
  di/RepositoryModule.kt                 T4, T7
  di/NetworkModule.kt                    T7
  di/IoDispatcher.kt                     T2
  ui/theme/*                             T1
  ui/base/OrbitViewModel.kt              T3   (+ UiState/UiEvent/UiSideEffect)
  ui/today/{TodayContract,TodayViewModel,TodayScreen}.kt          T3, T4, T6
  ui/navigation/{Routes,SnapCaloriesNavHost}.kt                   T5, T6
  ui/history/{HistoryContract,HistoryViewModel,HistoryScreen}.kt  T5
  ui/daydetail/{DayDetailContract,DayDetailViewModel,DayDetailScreen}.kt T5
  ui/review/{ReviewContract,ReviewViewModel,ReviewScreen}.kt      T6, T7
  ui/camera/PhotoFileFactory.kt          T6
  domain/model/{FoodEntry,DailyTotal,CalorieEstimate,AnalysisError}.kt  T4, T7
  domain/repository/{FoodEntryRepository,CalorieRepository}.kt    T4, T7
  domain/usecase/*.kt                    T4, T5, T7, T8
  data/local/{AppDatabase,FoodEntryEntity,FoodEntryDao,DayTotalRow}.kt  T4
  data/repository/{FoodEntryRepositoryImpl,CalorieRepositoryImpl}.kt    T4, T7
  data/remote/api/GeminiApi.kt           T7
  data/remote/dto/request/*, response/*  T7
  data/remote/ImageEncoder.kt            T7
  data/remote/ApiKeyInterceptor.kt       T7
  worker/PurgeOldEntriesWorker.kt        T8
app/src/main/res/xml/file_paths.xml      T6
app/src/test/.../                        tests per task (fakes in test/.../fake/)
app/src/androidTest/.../FoodEntryDaoTest.kt  T4
```

---

### Task 1: Gradle + Compose "Hello" 🟢 detailed hints

**Concepts to explain first:** what Gradle is; version catalog (`[versions]` / `[libraries]` / `[plugins]`); plugins vs libraries; the Compose BOM; the Compose compiler plugin; why AGP 9 has built-in Kotlin; why minSdk 26.

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts` (root), `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Create: `MainActivity.kt`, `ui/theme/{Color,Theme,Type}.kt`
- Delete: appcompat/material XML theme dependency where Compose replaces it (keep a minimal `Theme.SnapCalories` parent `android:Theme.Material.Light.NoActionBar`)

**Session 1a — catalog and plugins**
- [ ] Add the Kotlin, Compose-compiler, and serialization plugins plus the Compose BOM, activity-compose, material3, ui-tooling(-preview), and lifecycle entries to the catalog (versions table above).
- [ ] Root `build.gradle.kts`: declare the new plugins `apply false`. `app/build.gradle.kts`: apply `kotlin.compose`; set `buildFeatures { compose = true }`; minSdk 26.
- [ ] Verify: `./gradlew assembleDebug` → BUILD SUCCESSFUL.
- [ ] Commit `build: Add Compose and Kotlin plugins, raise minSdk to 26`.

**Session 1b — first screen**
- [ ] `MainActivity : ComponentActivity` calls `enableEdgeToEdge()` and `setContent { SnapCaloriesTheme { Text("SnapCalories") } }` inside a `Scaffold`; register it in the manifest as the launcher.
- [ ] `SnapCaloriesTheme` wraps `MaterialTheme` (dynamic color on API 31+).
- [ ] Verify: `./gradlew installDebug`, then the app shows "SnapCalories" on the device.
- [ ] Commit `feat: Add Compose MainActivity`. Update `CLAUDE.md` (Compose added, minSdk 26).

---

### Task 2: Hilt skeleton 🟢 detailed hints

**Concepts:** dependency injection in plain words; the Hilt component tree (Singleton → Activity → ViewModel); `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Module`/`@InstallIn`, `@Provides` vs `@Binds`, qualifiers; KSP as a code generator.

**Files:**
- Modify: catalog, root and app `build.gradle.kts` (plugins `ksp`, `hilt`), `AndroidManifest.xml` (`android:name=".SnapCaloriesApp"`)
- Create: `SnapCaloriesApp.kt`, `di/IoDispatcher.kt`, `di/CoreModule.kt`

**Interfaces — produces:**
- `@Qualifier annotation class IoDispatcher`
- `CoreModule` (`@InstallIn(SingletonComponent::class)`): `@Provides @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO` and `@Provides @Singleton fun provideClock(): Clock = Clock.systemDefaultZone()`

**Session 2a**
- [ ] Add Hilt + KSP to the catalog and plugins, plus the `hilt-android` / `hilt-compiler` (ksp) dependencies.
- [ ] Create `SnapCaloriesApp` (`@HiltAndroidApp`) and annotate `MainActivity` `@AndroidEntryPoint`.
- [ ] Create `IoDispatcher` + `CoreModule`.
- [ ] Proof it works: `@Inject lateinit var clock: Clock` in `MainActivity`; show `LocalDate.now(clock)` on screen. Remove it afterwards.
- [ ] Verify: `./gradlew assembleDebug` succeeds and today's date shows on the device.
- [ ] Commit `feat: Wire Hilt with Clock and IoDispatcher`. Update `CLAUDE.md`.

---

### Task 3: OrbitViewModel base + Today with hard-coded data 🟢 detailed hints

**Concepts:** MVI loop (Event → ViewModel → State/Effect → UI); State vs Effect (state = what's on screen, effect = do-once); Orbit's `container`, `intent`, `reduce`, `postSideEffect`; `collectAsState`/`collectSideEffect`; why `XxxContent` is stateless.

**Files:**
- Create: `ui/base/OrbitViewModel.kt`, `ui/today/TodayContract.kt`, `ui/today/TodayViewModel.kt`, `ui/today/TodayScreen.kt`, `domain/model/FoodEntry.kt`
- Test: `app/src/test/java/com/example/snapcalories/ui/today/TodayViewModelTest.kt`

**Interfaces — produces:**
- `interface UiState`, `interface UiEvent`, `interface UiSideEffect`
- `abstract class OrbitViewModel<S : UiState, E : UiEvent, F : UiSideEffect>(initialState: S) : ViewModel(), ContainerHost<S, F>` with `final override val container`, `abstract fun onEvent(event: E)`, `protected fun setState(reducer: S.() -> S)`, `protected fun postEffect(effect: F)`. Check the `ContainerHost` generic signature against Orbit 12 docs while doing this step.
- `data class FoodEntry(val id: Long, val foodName: String, val calories: Int, val createdAt: Instant, val epochDay: Long)`
- `TodayContract.State(entries: List<FoodEntry> = emptyList(), totalCalories: Int = 0, isLoading: Boolean = true)`
- `TodayContract.Event`: `CameraClicked`, `HistoryClicked`, `DeleteEntry(id: Long)`. `PhotoCaptured`/`PhotoCancelled` come in T6.
- `TodayContract.Effect`: `NavigateToHistory`. `LaunchCamera`/`NavigateToReview` come in T6.
- `@Composable fun TodayScreen(viewModel: TodayViewModel = hiltViewModel(), onNavigateToHistory: () -> Unit)` and `@Composable fun TodayContent(state: State, onEvent: (Event) -> Unit)`

**Session 3a — base class + contract + test**
- [ ] Add `orbit-viewmodel`, `orbit-compose`, `orbit-test`, `coroutines-test`, `lifecycle-viewmodel-compose`, `hilt-navigation-compose` to the catalog and app.
- [ ] Write `OrbitViewModel` + marker interfaces, then `TodayContract`.
- [ ] Write the failing test `TodayViewModelTest`:
  - `init loads hardcoded entries and total` → `expectState { copy(entries = <3 fake entries>, totalCalories = <sum>, isLoading = false) }`
  - `HistoryClicked posts NavigateToHistory` → `expectSideEffect(Effect.NavigateToHistory)`
  - `DeleteEntry removes entry and recalculates total`
- [ ] Run `./gradlew test --tests "*TodayViewModelTest"` → FAIL (no ViewModel yet).

**Session 3b — ViewModel + UI**
- [ ] `@HiltViewModel class TodayViewModel @Inject constructor() : OrbitViewModel<...>(State())`. `init` sets 3 hard-coded entries.
- [ ] Run tests → PASS.
- [ ] `TodayContent`: top bar "Today", total card, `LazyColumn` of entries (name + kcal + delete icon), FAB with camera icon, History action in the top bar. Add a `@Preview` using fake state.
- [ ] Verify on the device: the list shows, delete removes an item, and the total updates.
- [ ] Commit `feat: Add OrbitViewModel base and Today screen`. Update `CLAUDE.md` (MVI convention now established).

---

### Task 4: Room + repository + use cases → real data 🟡 medium hints

**Concepts:** Room entity/DAO/database; `Flow` from Room = live updates; entity ↔ domain mapping; interface + `@Binds`; use cases with `operator fun invoke`; fakes vs mocks; injected `Clock` for "today".

**Files:**
- Create: `data/local/{FoodEntryEntity,FoodEntryDao,DayTotalRow,AppDatabase}.kt`, `domain/model/DailyTotal.kt`, `domain/repository/FoodEntryRepository.kt`, `data/repository/FoodEntryRepositoryImpl.kt`, `domain/usecase/{ObserveEntriesForDay,SaveFoodEntry,DeleteFoodEntry}.kt`, `di/DatabaseModule.kt`, `di/RepositoryModule.kt`
- Modify: `TodayViewModel.kt`, `TodayViewModelTest.kt`
- Test: `test/.../fake/FakeFoodEntryRepository.kt`, `test/.../domain/usecase/SaveFoodEntryTest.kt`, `androidTest/.../data/local/FoodEntryDaoTest.kt`

**Interfaces — produces:**
- `FoodEntryEntity(@PrimaryKey(autoGenerate = true) id: Long = 0, foodName: String, calories: Int, createdAt: Long, epochDay: Long)`, table `food_entries`, index on `epochDay`
- `FoodEntryDao`: `observeByDay(epochDay: Long): Flow<List<FoodEntryEntity>>` (createdAt DESC); `observeDailyTotals(sinceMillis: Long): Flow<List<DayTotalRow>>`; `suspend insert(entity): Long`; `suspend deleteById(id: Long)`; `suspend deleteOlderThan(cutoffMillis: Long): Int`
- `DayTotalRow(epochDay: Long, totalCalories: Int)`; `DailyTotal(date: LocalDate, totalCalories: Int)`
- `interface FoodEntryRepository`: `observeEntriesForDay(date: LocalDate): Flow<List<FoodEntry>>`, `observeDailyTotals(since: Instant): Flow<List<DailyTotal>>`, `suspend save(foodName: String, calories: Int, createdAt: Instant, epochDay: Long)`, `suspend delete(id: Long)`, `suspend deleteOlderThan(cutoff: Instant): Int`
- `ObserveEntriesForDay(repo, clock)`: `operator fun invoke(date: LocalDate = LocalDate.now(clock)): Flow<List<FoodEntry>>`
- `SaveFoodEntry(repo, clock)`: `suspend operator fun invoke(foodName: String, calories: Int)`. It computes `createdAt = clock.instant()` and `epochDay = LocalDate.ofInstant(createdAt, clock.zone).toEpochDay()`.
- `DeleteFoodEntry(repo)`: `suspend operator fun invoke(id: Long)`

**Session 4a — Room**
- [ ] Add Room (runtime, ktx, compiler via ksp, testing) to the catalog.
- [ ] Write entity, `DayTotalRow`, DAO (the SQL is in spec §5.1), and `AppDatabase` (version 1, `exportSchema = false`).
- [ ] Write `FoodEntryDaoTest` (in-memory DB): `observeByDay returns only that day newest first`; `observeDailyTotals sums per day and excludes rows before since`; `deleteOlderThan removes only older rows and returns count`.
- [ ] Verify: `./gradlew connectedAndroidTest` → PASS. Needs an emulator; if none is available, `assembleDebugAndroidTest` must at least compile.
- [ ] Commit `feat: Add Room database for food entries`.

**Session 4b — repository, use cases, DI**
- [ ] Domain interface + `FoodEntryRepositoryImpl` (mapping). `DatabaseModule` provides the DB and DAO; `RepositoryModule` `@Binds` the repository.
- [ ] Write `SaveFoodEntryTest` with `FakeFoodEntryRepository` and a fixed clock:
  - `saves with clock instant and local epochDay`
  - `entry at 23:59 local is that day not next` (clock at `2026-09-30T23:59` in `Asia/Manila` → epochDay of 2026-09-30)
- [ ] Implement the use cases → tests PASS.
- [ ] Commit `feat: Add FoodEntryRepository and use cases`.

**Session 4c — Today uses real data**
- [ ] `TodayViewModel` injects `ObserveEntriesForDay`, `DeleteFoodEntry`, and a temporary `SaveFoodEntry`. `init` collects the flow in `intent { repeatOnSubscription { ... } }` (or a plain collect) and sets state. A temporary debug event `AddTestEntry` saves "Test food"/250.
- [ ] Update `TodayViewModelTest` to use `FakeFoodEntryRepository`: `emits entries from repository`, `DeleteEntry calls delete`.
- [ ] Verify: add entries on the device, kill the app, reopen → entries are still there.
- [ ] Commit `feat: Today screen reads from Room`. Update `CLAUDE.md`.

---

### Task 5: Navigation + History + Day detail 🟡 medium hints

**Concepts:** NavHost/NavController; type-safe `@Serializable` routes; passing args; `SavedStateHandle.toRoute()`; why navigation is an Effect handled in the Screen, not the ViewModel.

**Files:**
- Create: `ui/navigation/{Routes,SnapCaloriesNavHost}.kt`, `ui/history/*`, `ui/daydetail/*`, `domain/usecase/ObserveDailyTotals.kt`
- Modify: `MainActivity.kt`, `TodayScreen.kt`
- Test: `HistoryViewModelTest.kt`, `DayDetailViewModelTest.kt`, `ObserveDailyTotalsTest.kt`

**Interfaces — produces:**
- `@Serializable object TodayRoute`, `@Serializable object HistoryRoute`, `@Serializable data class DayDetailRoute(val epochDay: Long)`
- `ObserveDailyTotals(repo, clock)`: `operator fun invoke(): Flow<List<DailyTotal>>` with `since = clock.instant() − 30 days`
- `HistoryContract`: State(`days: List<DailyTotal>`, `isLoading`), Event `DayClicked(epochDay: Long)`, Effect `NavigateToDay(epochDay: Long)`
- `DayDetailContract`: State(`date: LocalDate`, `entries`, `totalCalories`), Event `DeleteEntry(id)`, no effects (use an empty sealed interface)

**Session 5a — navigation + History**
- [ ] Add navigation-compose + serialization plugin/json. Create routes and a NavHost with Today + History.
- [ ] `ObserveDailyTotalsTest`: `since is exactly 30 days before now`.
- [ ] `HistoryViewModelTest`: `shows daily totals newest first`; `DayClicked posts NavigateToDay`.
- [ ] Implement → PASS. The History screen shows rows like "Sep 28 · 1,850 kcal".
- [ ] Commit `feat: Add navigation and History screen`.

**Session 5b — Day detail**
- [ ] `DayDetailViewModel` reads `DayDetailRoute` from `SavedStateHandle`. Test: `loads entries for route epochDay`, `DeleteEntry deletes`. In tests, build `SavedStateHandle` with the route's args.
- [ ] Implement the screen (reuse the entry row composable from Today by moving it to `ui/components/FoodEntryRow.kt`).
- [ ] Verify on the device: Today → History → tap a day → its entries show.
- [ ] Commit `feat: Add Day detail screen`. Update `CLAUDE.md`.

---

### Task 6: Camera + Review (manual entry) 🟡 medium hints

**Concepts:** Activity Result API (`rememberLauncherForActivityResult` + `TakePicture`); why FileProvider exists (sharing a file path safely with another app); effects that launch things; `onCleared` cleanup; input validation in state.

**Files:**
- Create: `ui/camera/PhotoFileFactory.kt`, `res/xml/file_paths.xml`, `ui/review/{ReviewContract,ReviewViewModel,ReviewScreen}.kt`
- Modify: `AndroidManifest.xml` (`<provider>` FileProvider, authority `${applicationId}.fileprovider`), `Routes.kt` (+ `@Serializable data class ReviewRoute(val photoPath: String)`), NavHost, `TodayContract`/`TodayViewModel`/`TodayScreen` (remove `AddTestEntry`)
- Test: `TodayViewModelTest.kt`, `ReviewViewModelTest.kt`

**Interfaces — produces:**
- `interface PhotoFileFactory { fun create(): File }` with `CachePhotoFileFactory @Inject constructor(@ApplicationContext context)` (creates in `cacheDir/photos/`), bound in `RepositoryModule`. Tests use a fake backed by a temp folder.
- `fun File.toContentUri(context: Context): Uri` (in `ui/camera/`) wraps `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)` and is called **only from Screens**.
- Today additions: Events `PhotoCaptured`, `PhotoCancelled`; Effects `LaunchCamera(file: File)`, `NavigateToReview(photoPath: String)`. The VM holds the pending file.
- `ReviewContract.State(photoPath: String, isAnalyzing: Boolean = false, foodName: String = "", caloriesText: String = "", error: AnalysisError? = null, isSaving: Boolean = false)` with the computed `val canSave: Boolean` (name not blank, `caloriesText.toIntOrNull()` > 0, not saving/analyzing)
- Review Events: `NameChanged(String)`, `CaloriesChanged(String)`, `SaveClicked`, `RetakeClicked`, `PhotoRetaken`, `RetakeCancelled`, `RetryClicked`. Effects: `NavigateBack`, `LaunchCamera(file: File)`.

**Session 6a — camera from Today**
- [ ] FileProvider + `file_paths.xml` + `PhotoFileFactory` / `CachePhotoFileFactory` + `toContentUri`.
- [ ] Tests in `TodayViewModelTest`: `CameraClicked posts LaunchCamera`; `PhotoCaptured posts NavigateToReview with pending path`; `PhotoCancelled deletes temp file and posts nothing` (Review Focus #3). Use a temp folder in the test.
- [ ] Implement; `TodayScreen` launches `TakePicture` on the effect and sends the result back as an event.
- [ ] Commit `feat: Launch camera from Today`.

**Session 6b — Review screen (manual)**
- [ ] `ReviewViewModelTest`:
  - `canSave false for blank name or invalid calories`, parameterized over `""`, `"abc"`, `"0"`, `"-5"`, `"99999999999"` (Review Focus #2)
  - `SaveClicked saves trimmed name and calories then posts NavigateBack and deletes photo`
  - `RetakeClicked posts LaunchCamera`
- [ ] Implement ViewModel + screen (photo preview via `BitmapFactory` decoded small or `AsyncImage` if you add Coil; fields; Save/Retake buttons). Analysis is stubbed for now: fields start empty.
- [ ] Verify on the device: snap → Review → type "Adobo"/650 → Save → it appears on Today.
- [ ] Commit `feat: Add Review screen with manual entry`. Update `CLAUDE.md`.

---

### Task 7: Gemini integration 🟠 light hints

**Concepts:** REST/HTTP basics; Retrofit interface; DTOs vs domain models; kotlinx.serialization and `@SerialName`; OkHttp interceptors; BuildConfig fields; Base64; structured output (`responseSchema`); mapping HTTP errors to domain errors; MockWebServer.

**First, Claude verifies the current Gemini Flash model name and free-tier limits in Google's docs, and the developer creates a key in Google AI Studio.**

**Files:**
- Create: `data/remote/api/GeminiApi.kt`, `data/remote/dto/request/{GenerateContentRequest,Content,Part,InlineData,GenerationConfig}.kt`, `data/remote/dto/response/{GenerateContentResponse,Candidate}.kt`, `data/remote/dto/EstimateDto.kt`, `data/remote/ApiKeyInterceptor.kt`, `data/remote/ImageEncoder.kt`, `domain/model/{CalorieEstimate,AnalysisError}.kt`, `domain/repository/CalorieRepository.kt`, `data/repository/CalorieRepositoryImpl.kt`, `domain/usecase/AnalyzeFoodPhoto.kt`, `di/NetworkModule.kt`
- Modify: `app/build.gradle.kts` (`buildFeatures.buildConfig = true`, read `GEMINI_API_KEY` from `local.properties`), `RepositoryModule`, `ReviewViewModel`, `ReviewScreen`
- Test: `CalorieRepositoryImplTest.kt` (MockWebServer), `ImageEncoderTest.kt`, `ReviewViewModelTest.kt`

**Interfaces — produces:**
- `GeminiApi`: `@POST("v1beta/models/{model}:generateContent") suspend fun generateContent(@Path("model") model: String, @Body body: GenerateContentRequest): GenerateContentResponse`
- `EstimateDto(foodName: String, calories: Int, isFood: Boolean)`
- `data class CalorieEstimate(val foodName: String, val calories: Int)`
- `sealed interface AnalysisError { Network; QuotaExceeded; InvalidApiKey; NotFood; Unknown }` (data objects)
- `interface CalorieRepository { suspend fun analyze(photoPath: String): Result<CalorieEstimate> }`. The failure carries an `AnalysisException(val error: AnalysisError)`.
- `class ImageEncoder @Inject constructor()`: `fun encodeJpegBase64(path: String, maxSide: Int = 1024, quality: Int = 80): String`
- `AnalyzeFoodPhoto(repo)`: `suspend operator fun invoke(photoPath: String): Result<CalorieEstimate>`

**Session 7a — DTOs, API, network module**
- [ ] Add Retrofit, the converter, OkHttp, logging, and MockWebServer to the catalog. Add the BuildConfig key field.
- [ ] DTOs (request shape in spec §6, snake_case via `@SerialName("inline_data")`, `"mime_type"`, `"response_mime_type"`, `"response_schema"`), `GeminiApi`, `ApiKeyInterceptor`, `NetworkModule` (base URL `https://generativelanguage.googleapis.com/`, `Json { ignoreUnknownKeys = true }`, logging `BODY` only in debug and with the key header redacted).
- [ ] Verify: `./gradlew assembleDebug` succeeds.
- [ ] Commit `feat: Add Gemini Retrofit API and network module`.

**Session 7b — repository + error mapping (TDD)**
- [ ] `CalorieRepositoryImplTest` with MockWebServer and a fake encoder:
  - `maps valid reply to CalorieEstimate` (reply text `{"foodName":"Adobo","calories":650,"isFood":true}`)
  - `isFood false maps to NotFood`
  - `HTTP 429 maps to QuotaExceeded`; `HTTP 403 maps to InvalidApiKey`
  - `socket timeout maps to Network` (`SocketPolicy`/no response)
  - `empty candidates, non-JSON text, or negative calories map to Unknown` (Review Focus #4)
- [ ] Implement `CalorieRepositoryImpl` (the prompt text is in spec §6) → PASS.
- [ ] `ImageEncoderTest` (Robolectric not used; test the pure `calculateInSampleSize(width, height, maxSide): Int` helper): `4000x3000 with max 1024 gives 2`; `8000x6000 gives 4`; `800x600 gives 1` (Review Focus #5).
- [ ] Commit `feat: Add CalorieRepository with error mapping`.

**Session 7c — Review auto-fills**
- [ ] `ReviewViewModelTest`: `init analyzes and fills fields`; `failure sets error and leaves fields editable`; `RetryClicked re-runs analysis`; `PhotoRetaken re-runs analysis`.
- [ ] Wire `AnalyzeFoodPhoto` into the ViewModel, and show a loading overlay plus the error messages from spec §6.
- [ ] Verify on the device: real food → estimate appears; airplane mode → "Couldn't reach the AI" + Retry.
- [ ] Commit `feat: Auto-fill Review with Gemini estimate`. Update `CLAUDE.md`.

---

### Task 8: 30-day cleanup worker 🟠 light hints

**Concepts:** WorkManager (why not a coroutine or AlarmManager); periodic vs one-time; unique work; `@HiltWorker` + `@AssistedInject`; custom WorkManager configuration and disabling the default initializer.

**Files:**
- Create: `domain/usecase/PurgeOldEntries.kt`, `worker/PurgeOldEntriesWorker.kt`
- Modify: `SnapCaloriesApp.kt` (`Configuration.Provider`, injected `HiltWorkerFactory`, scheduling), `AndroidManifest.xml` (remove `androidx.work.WorkManagerInitializer` via `tools:node="remove"` on the startup provider meta-data), catalog (`work-runtime-ktx`, `hilt-work`, `androidx.hilt:hilt-compiler`)
- Test: `PurgeOldEntriesTest.kt`

**Interfaces — produces:**
- `PurgeOldEntries(repo, clock)`: `suspend operator fun invoke(): Int` with `cutoff = clock.instant() − 30 days`
- `@HiltWorker class PurgeOldEntriesWorker @AssistedInject constructor(@Assisted context, @Assisted params, purge: PurgeOldEntries) : CoroutineWorker`
- Unique work names `"purge-old-entries-daily"` (periodic, 24 h, `KEEP`) and `"purge-old-entries-startup"` (one-time, `REPLACE`)

**Session 8a**
- [ ] `PurgeOldEntriesTest`: `deletes entries older than 30 days`; `keeps entry exactly 30 days minus 1 minute old`.
- [ ] Implement the use case → PASS. Then the worker, the app configuration, the manifest change, and scheduling.
- [ ] Verify: run the app and check with `adb shell dumpsys jobscheduler | grep snapcalories`, or App Inspection → Background Task Inspector, that both jobs are enqueued and the startup job succeeded.
- [ ] Commit `feat: Add 30-day purge worker`. Update `CLAUDE.md`.

---

### Task 9: Polish 🔴 goal + checklist only

**Goal:** the app feels finished for daily personal use.

- [ ] Empty states: Today ("No food logged yet — tap 📷"), History, Day detail.
- [ ] Loading indicators where `isLoading`/`isAnalyzing`.
- [ ] Delete confirmation dialog (a new Event + State flag, no Effect needed; explain why).
- [ ] Number formatting ("1,850 kcal") and date formatting ("Mon, Sep 28").
- [ ] `./gradlew build` passes (lint + all tests).
- [ ] Commit `feat: Polish empty states, loading, delete confirmation`. Final `CLAUDE.md` update.
