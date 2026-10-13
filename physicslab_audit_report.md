# PhysicsLab — Full Audit Report
> Generated: 2026-10-13 | Status: Read-only audit. No files modified.

---

## 1. Platform Identity

| Item | Value |
|---|---|
| **applicationId / package** | `com.physicslab.app` |
| **Language** | Kotlin (100%) |
| **UI Toolkit** | Android Views — XML layouts + View Binding (`buildFeatures { viewBinding true }`) |
| **minSdk** | 21 (Android 5.0 Lollipop) |
| **targetSdk / compileSdk** | 34 (Android 14) |
| **AGP** | 8.2.2 |
| **Gradle wrapper** | 8.4 |
| **Kotlin** | 1.9.22 (via `ext.kotlin_version` in root `build.gradle`) |
| **Kotlin plugin style** | Classic `buildscript {}` block — NOT Kotlin DSL, NOT version catalogs |
| **JVM target** | 1.8 (`compileOptions` + `kotlinOptions`) |

---

## 2. Architecture, Navigation & DI

### Architecture
**None formally applied.** All business logic lives directly in Activity subclasses. There are no ViewModels, no UseCases, no Repositories beyond the Room DAO. Each Activity is self-contained.

### Navigation
Manual `startActivity(Intent(...))` from `MainActivity`. No Navigation Component. The back button on every sensor screen is a plain `TextView` wired to `finish()`.

### Dependency Injection
None. `AppDatabase.getInstance(context)` uses a hand-rolled double-checked-locking singleton. `ThemeManager` and `ReadingLogger` are Kotlin `object` singletons.

### State Handling
- Sensor screens: raw Activity fields (no `savedInstanceState` restoration).
- `ReactionTimeActivity`: `enum State { IDLE, WAITING, READY }` — not saved across rotation.
- `HistoryActivity`: in-memory `allReadings` + `currentFilterMs` field — loses filter on rotation.
- `AccelerationActivity`: `includeGravity: Boolean` field — loses toggle on rotation.
- Audio screens (`Decibel`, `SoundFrequency`): `listening` flag + `Job` — audio stops on rotation (via `onPause`).

---

## 3. Screens Inventory

| Screen | Class | Sensor/Input | What Works |
|---|---|---|---|
| **Dashboard** | `MainActivity` | — | Card navigation to all 11 sensor screens + History + Settings |
| **Magnetic Field** | `MagneticFieldActivity` | `TYPE_MAGNETIC_FIELD` | Live X/Y/Z + resultant (µT); sensor-unavailable banner; throttled DB logging |
| **Acceleration** | `AccelerationActivity` | `TYPE_ACCELEROMETER` / `TYPE_LINEAR_ACCELERATION` | Live X/Y/Z + resultant; gravity toggle switch; explanation text; DB logging |
| **Light** | `LightActivity` | `TYPE_LIGHT` | Live lux + qualitative condition label; DB logging |
| **Gyroscope** | `GyroscopeActivity` | `TYPE_GYROSCOPE` | Live X/Y/Z + resultant (rad/s); DB logging |
| **Compass** | `CompassActivity` | `TYPE_ACCELEROMETER` + `TYPE_MAGNETIC_FIELD` | Heading (°), cardinal label, needle image rotation; DB logging |
| **Barometer** | `BarometerActivity` | `TYPE_PRESSURE` | Live hPa + derived altitude via barometric formula; DB logging |
| **Step Counter** | `StepCounterActivity` | `TYPE_STEP_COUNTER` | Session-relative step count; ACTIVITY_RECOGNITION permission on API >= Q; DB logging |
| **Sound Frequency** | `SoundFrequencyActivity` | Mic / `AudioRecord` | Goertzel dominant frequency 60-2000 Hz; toggle button; DB logging |
| **Decibel Meter** | `DecibelActivity` | Mic / `AudioRecord` | Relative SPL in dB from RMS; toggle button; DB logging |
| **Digital Ruler** | `RulerActivity` | Camera (CameraX) + `RulerOverlayView` | Live camera preview; tap-two-points pixel measurement; credit-card calibration; DB logging |
| **Reaction Time** | `ReactionTimeActivity` | Touch / `Handler` | IDLE->WAIT->READY state machine; false-start detection; last + best time display; DB logging |
| **History** | `HistoryActivity` | Room DB | Full reading log; 1d/1w/1m/3m/all filters; TrendGraphView; CSV export via FileProvider |
| **Settings** | `SettingsActivity` | SharedPreferences | Light/Dark/System theme toggle; ruler calibration display + reset |

---

## 4. Sensor Code Analysis

### Registration Pattern (all hardware sensors)
```
onResume()  -> sensorManager.registerListener(this, sensor, SENSOR_DELAY_UI)
onPause()   -> sensorManager.unregisterListener(this)
```
**Correct** — no leak risk. `SENSOR_DELAY_UI` (~60 ms) is appropriate for display.

### Screen-Specific Notes

| Screen | Finding |
|---|---|
| `AccelerationActivity` | `registerActiveSensor()` called in both `onResume()` and the switch toggle. The `unregisterListener` at the top of `registerActiveSensor()` prevents double-registration. **Safe.** |
| `CompassActivity` | Uses `haveGravity` + `haveGeomagnetic` guard flags before `getRotationMatrix`. **Correct.** |
| `StepCounterActivity` | `baseline` field resets on process kill — expected behavior for `TYPE_STEP_COUNTER`. |
| `SoundFrequencyActivity` | Minor race: if `startListening()` throws after `listening = true` is set but before the coroutine launches, the button shows "Stop" while audio is not recording. Low risk. |
| `DecibelActivity` | Same minor race as SoundFrequency. |
| `RulerActivity` | CameraX lifecycle bound to `this` Activity — **correct**. No image analysis pipeline (preview only). |

### Threading
- Sensor callbacks: Main thread (correct).
- Audio loop: `Dispatchers.Default` coroutine (correct).
- DB writes: `Dispatchers.IO` via `ReadingLogger` (correct).
- DB reads in `HistoryActivity`: `lifecycleScope.launch {}` collecting a `Flow` on Main (correct).

### Potential Leaks
- `ReactionTimeActivity.handler`: `onDestroy()` calls `removeCallbacks()` — **correctly cleaned up**.
- No `SensorManager` leaks found.
- `AppDatabase` singleton holds `applicationContext` — safe.

---

## 5. Persistence

### Library
Room 2.6.1

### Schema (version 1)
```sql
CREATE TABLE readings (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    sensorType TEXT NOT NULL,   -- e.g. "GYROSCOPE"
    label      TEXT NOT NULL,   -- e.g. "Angular velocity"
    value      REAL NOT NULL,   -- numeric reading
    unit       TEXT NOT NULL,   -- e.g. "rad/s"
    timestamp  INTEGER NOT NULL -- System.currentTimeMillis()
);
```

### Critical: `exportSchema = false`
No migration scripts or golden schema files are generated. Any schema change without a manual `Migration` object will either crash at runtime or silently wipe the DB. **This is the single biggest data-safety risk.**

### DAO Methods
| Method | Used in UI? |
|---|---|
| `insert(reading)` | Yes — via `ReadingLogger` |
| `getAll()` — Flow | Yes — `HistoryActivity` |
| `getSince(since)` — Flow | **NO** — dead code |
| `getBySensor(sensorType)` — Flow | **NO** — dead code |
| `clearAll()` — suspend | Yes — `HistoryActivity` |

---

## 6. Dependencies

### Runtime

| Library | In Use | Flag |
|---|---|---|
| `core-ktx` | 1.12.0 | Minor update available |
| `appcompat` | 1.6.1 | Minor update available |
| `material` | 1.11.0 | Minor update available |
| `constraintlayout` | 2.1.4 | Minor update available |
| `cardview` | 1.0.0 | Current |
| `recyclerview` | 1.3.2 | Current |
| `lifecycle-runtime-ktx` | 2.7.0 | Minor update available |
| `room-runtime` | 2.6.1 | Current |
| `room-ktx` | 2.6.1 | Current |
| `room-compiler` (kapt) | 2.6.1 | Current |
| `camera-core/camera2/lifecycle/view` | 1.3.1 | Minor update available (1.4.x) |

**No INTERNET permission. No network libraries. No analytics. No downloadable fonts.**

### Missing (for planned features)
- `lifecycle-viewmodel-ktx` — needed to add ViewModels
- `viewpager2` — if tabs are chosen as the tab UI mechanism

### Build Plugins

| Plugin | Version | Note |
|---|---|---|
| AGP | 8.2.2 | 8.4+ current; minor gap |
| kotlin-android + kapt | 1.9.22 | KSP would be faster; requires plugin addition |

---

## 7. AndroidManifest

### Permissions
| Permission | Purpose |
|---|---|
| `RECORD_AUDIO` | Microphone (Decibel + SoundFrequency) |
| `CAMERA` | CameraX (Digital Ruler) |
| `ACTIVITY_RECOGNITION` | Step counter on API >= Q |

**No INTERNET permission.**

### `<uses-feature>` Declarations
All 8 hardware features declared with `required="false"`. Each Activity checks availability at runtime. **Correct approach.**

### Theme & Night Mode
- Base: `Theme.MaterialComponents.DayNight.NoActionBar`
- `ThemeManager` applies saved mode in `PhysicsLabApp.onCreate()` — correct.
- `values-night/` directory exists for dark mode overrides.
- `android:windowBackground` and `android:statusBarColor` set to `@color/lab_bg` — works but the status bar stays light-colored in dark mode unless overridden in `values-night/themes.xml`.

### FileProvider & Shortcuts
- FileProvider declared for CSV export — correct.
- Static shortcuts for Magnetic Field, Acceleration, Decibel, Reaction Time — all point to valid Activities.

---

## 8. Existing Tests

**None.** No `src/test/` or `src/androidTest/` directory exists.

> Running `./gradlew testDebugUnitTest` will succeed vacuously — zero tests, zero failures, zero confidence.

---

## 9. Baseline Build Results

### `./gradlew assembleDebug` — RESULT: BUILD FAILED

```
BUILD FAILED in 47s

* What went wrong:
Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> SDK location not found. Define a valid SDK location with an
  ANDROID_HOME environment variable or by setting the sdk.dir path
  in your project's local.properties file.
```

> [!WARNING]
> **Root cause: `local.properties` contains a hardcoded SDK path from a different machine.**
>
> Line 8 of `local.properties`:
> ```
> sdk.dir=C\:\\Users\\Ritesh\\AppData\\Local\\Android\\Sdk
> ```
> This points to user "Ritesh"'s machine. On this machine (user "Supriya") the SDK is
> not at that path. `local.properties` is intentionally git-ignored and machine-specific.
>
> **To fix before the next build:**
> 1. Open Android Studio → the IDE will prompt to update `local.properties` automatically, OR
> 2. Manually update `local.properties` to `sdk.dir=C\:\\Users\\Supriya\\AppData\\Local\\Android\\Sdk`
>    (or wherever the Android SDK is installed on this machine), OR
> 3. Set the `ANDROID_HOME` environment variable to the correct SDK path.
>
> **This is NOT a code bug** — it is a one-time machine setup issue.
> The Kotlin/Groovy source files contain no unresolved references or type errors
> (verified by full manual source review of all 23 `.kt` files).

### `./gradlew testDebugUnitTest` — NOT RUN
Blocked by the same SDK path issue. Once `local.properties` is corrected, expected result:
`BUILD SUCCESSFUL, 0 tests executed` (no test source sets exist).

---

## 10. Bugs, Bad Practices & Technical Debt

### HIGH Severity
| # | Location | Issue |
|---|---|---|
| C1 | `AppDatabase` | `exportSchema = false` + no migrations — any schema change silently destroys user data |
| C2 | `HistoryActivity` | `clearAll()` fires immediately with no confirmation dialog — one tap wipes all history |

### MEDIUM Severity
| # | Location | Issue |
|---|---|---|
| B1 | `DecibelActivity` | `REFERENCE = 1.0` (arbitrary) — readings cannot be compared to calibrated SPL. Not disclosed in UI. |
| B2 | `SoundFrequencyActivity` | Goertzel steps of 10 Hz may miss fundamentals that fall between bins; comment says 50 Hz min but code uses 60 Hz. |

### LOW Severity
| # | Location | Issue |
|---|---|---|
| B3 | `AccelerationActivity` | `setOnCheckedChangeListener` fires during `onCreate` when `isChecked = true` is set, calling `registerActiveSensor()` before `onResume`. Safe due to unregister guard, but fragile. |
| B4 | `CompassActivity` | `headingLabel()` array has 9 elements intentionally (trailing "N") — works but confusing. |
| B5 | `HistoryAdapter` | `notifyDataSetChanged()` instead of `DiffUtil.ItemCallback` — full rebind on every update. |
| L1 | All sensor Activities | No `savedInstanceState` — rotation loses all live display state and toggle positions. |
| L2 | `ReactionTimeActivity` | Rotation during WAITING loses the pending `postDelayed` — state silently resets. |
| L3 | `HistoryActivity` | `currentFilterMs` lost on rotation — filter resets to "All". |
| A1 | All Activities | Zero ViewModel usage — state in Activity fields. |
| A2 | `ThemeManager` | Mixed concerns: theme + calibration storage in same object. |
| A3 | `ReadingLogger` | Global `object` with a shared throttle `HashMap` — not scoped to individual sensor screens. |
| A4 | `HistoryActivity` | Fetches ALL readings then filters in memory — will degrade with large history. |
| A5 | `app/build.gradle` | No `lifecycle-viewmodel-ktx` dependency — cannot add ViewModels without it. |

---

## PROPOSAL (Read-Only — No Implementation)

### Target Screen Structure

#### Dashboard (modified `MainActivity`)
```
ScrollView or RecyclerView
  ├── App header ("PhysicsLab", subtitle)
  ├── [History] [Settings] buttons  (unchanged)
  ├── Featured sensor cards (Magnetic, Acceleration, Light) — FULL width
  │     Each shows: icon, name, LAST LOGGED VALUE from DB (live via Flow)
  └── Compact 2-column grid for remaining 8 sensors (unchanged layout)
```

#### Sensor Detail Screen (Magnetic Field / Acceleration / Light)
```
SensorDetailActivity
  ├── Toolbar: sensor name + back arrow
  └── ViewPager2 with TabLayout (3 tabs):
      |
      ├── [LIVE tab]
      │     ├── Large resultant value + unit
      │     ├── Axis breakdown (X/Y/Z) where applicable
      │     ├── Sensor hardware name chip
      │     ├── Accuracy indicator (SENSOR_STATUS_ACCURACY_HIGH/MED/LOW)
      │     └── "Save Reading" FAB (calls ReadingLogger.log immediately)
      |
      ├── [HISTORY tab]
      │     ├── Filter chips: 1h | 6h | 1d | 1w | All
      │     ├── RecyclerView (ListAdapter + DiffUtil)
      │     └── Export CSV button
      |
      └── [STATISTICS tab]
            ├── Filter chips (same as History tab — shared ViewModel state)
            ├── Summary row: Min / Max / Mean / Std Dev
            ├── Count of readings in window
            └── TrendGraphView (reused unchanged)
```

---

### Data Model & Migration Plan

#### Schema stays at version 1 (no changes needed for this feature set)

All statistics (min, max, mean, std dev) can be computed from existing `value` + `timestamp` columns.

#### New DAO queries needed (no schema change, no migration):
```kotlin
// Per-sensor filtered history
@Query("SELECT * FROM readings WHERE sensorType = :type AND timestamp >= :since ORDER BY timestamp DESC")
fun getBySensorSince(type: String, since: Long): Flow<List<ReadingEntity>>

// Per-sensor statistics
@Query("SELECT MIN(value) as min, MAX(value) as max, AVG(value) as avg, COUNT(*) as count FROM readings WHERE sensorType = :type AND timestamp >= :since")
suspend fun getStatsSince(type: String, since: Long): SensorStats

// Last reading per sensor (for dashboard cards)
@Query("SELECT * FROM readings WHERE sensorType = :type ORDER BY timestamp DESC LIMIT 1")
suspend fun getLatest(type: String): ReadingEntity?
```

#### If any future task requires a schema change:
1. Enable `exportSchema = true` (add `schemaLocation` to `build.gradle`).
2. Write an explicit `Migration(oldV, newV)` object.
3. Register it in `Room.databaseBuilder(...).addMigrations(...)`.
4. Never use `fallbackToDestructiveMigration()` in production.

---

### Keep vs Replace

| Component | Decision | Reason |
|---|---|---|
| Room + `ReadingEntity` + `ReadingDao` | **KEEP + extend** | Solid foundation; only needs new queries |
| `TrendGraphView` | **KEEP** | Self-contained; no external library needed |
| `RulerOverlayView` | **KEEP** | Correct and self-contained |
| `PhysicsLabApp` | **KEEP** | Correct |
| All sensor reading code (the `onSensorChanged` logic) | **KEEP** | Hardware reading is correct; move to ViewModel |
| `ThemeManager` | **KEEP theme part; SPLIT calibration** | Extract `CalibrationRepository` |
| `ReadingLogger` | **KEEP; make ViewModel-scoped** | Throttle logic is fine; remove global HashMap |
| `HistoryAdapter` | **REPLACE** with `ListAdapter<ReadingEntity, DiffCallback>` | DiffUtil for smooth updates |
| `HistoryActivity` in-memory filter | **REPLACE** with DAO query filter in ViewModel | Performance; correctness |
| Activity state fields | **REPLACE with ViewModel** | Rotation survival |
| Manual `startActivity()` navigation | **KEEP** | Navigation Component is overkill for this screen count |
| KAPT | **KEEP for now** | KSP upgrade is independent; do separately if Kotlin is bumped |

---

### New Dependencies Required (minimum)

| Library | Version | Justification |
|---|---|---|
| `lifecycle-viewmodel-ktx` | 2.7.0 (match existing lifecycle-runtime-ktx) | ViewModels for state survival |
| `viewpager2` | 1.0.0 | Only if tabs are the chosen UI for Live/History/Statistics — no alternative library is needed |

No new networking, analytics, or font libraries. Fully offline stays intact.

---

### Questions I Need Answered Before Coding

1. **Scope of upgrade**: Should ALL 11 sensor screens get Live/History/Statistics tabs, or only the three named (Magnetic Field, Acceleration, Light)? This determines how much shared abstraction (base ViewModel, base layout) to build.

2. **ViewModel scope**: Add `lifecycle-viewmodel-ktx` for the three named screens only, or all screens at once?

3. **Tab UI mechanism**: Which of the three options?
   - `ViewPager2` + `TabLayout` (swipeable tabs — standard Android)
   - `BottomNavigationView` within each sensor screen
   - Collapsible/expandable card sections on a single scroll view

4. **Dashboard live values**: Should each card on the home screen show the last-logged value from the DB (requires one `Flow` query per sensor type), or stay as static navigation tiles?

5. **Statistics time window**: Should the Statistics tab respect the same filter chips as the History tab (1h/6h/1d/1w/All), or always show whole-history stats?

6. **Schema / migration**: Do you want to bump to `exportSchema = true` and DB version 2 now (with a `notes` TEXT column or no column change at all), to establish the migration infrastructure? Or freeze at version 1 since no column change is needed yet?

7. **Dependency versions**: Should I upgrade all mildly-outdated libraries (core-ktx, appcompat, CameraX, etc.) in the same commit as the feature work, or in a separate preceding commit?

8. **`clearAll()` safety**: Add a confirmation dialog to "Clear History" as part of this work?

9. **AccuracyChip**: Should the LIVE tab show sensor accuracy (HIGH/MED/LOW/UNRELIABLE) — this requires the `onAccuracyChanged` callback to be wired up (currently ignored in all screens).

10. **Test coverage**: Should unit tests be added for the new ViewModel stats logic? (Zero tests currently exist.) If yes, I'll add `junit`, `mockk`/`turbine` to `testImplementation` only.
