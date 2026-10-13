# PhysicsLab 📱🔬

A modern, high-precision Android application that turns your mobile device into an interactive physics sensor laboratory. Built with **Kotlin**, **Coroutines**, **Flow**, **Room Database**, and modern **Material Design**, PhysicsLab enables students, educators, and science enthusiasts to measure, capture, visualize, and analyze real-world physical quantities.

---

## ✨ Features Overview

### 1. 🎛️ Modern Physics Hub (New Architecture)
The core suite features an intuitive bottom-navigation interface focusing on three fundamental quantities:
- **🧲 Magnetic Field**: 
  - Resultant field strength $|B| = \sqrt{B_x^2 + B_y^2 + B_z^2}$ in microteslas ($\mu\text{T}$).
  - Live $X, Y, Z$ component bars and hardware accuracy status badges.
  - Figure-8 recalibration guidance when geomagnetic sensor accuracy is compromised.
- **⚡ Acceleration**:
  - Resultant acceleration $|a| = \sqrt{a_x^2 + a_y^2 + a_z^2}$ in $\text{m/s}^2$.
  - Clear real-world physical context (e.g., standard gravitational constant $\approx 9.81\text{ m/s}^2$ at rest).
  - Tri-axial motion progress monitors.
- **💡 Ambient Light**:
  - Illuminance measurement in $\text{lux}$ across dynamic ranges ($0.0\text{x}$ to $100,000+\text{ lx}$).
  - Responsive logarithmic/range-adaptive formatting.
  - Indicative sensor notes informing users about uncalibrated hardware characteristics.

Each quantity includes three dedicated sub-sections:
- **Live Telemetry**: Real-time gauge dials, live trend canvas sparkline, session statistics (min, avg, max), and hardware metadata.
- **Capture Window**: ~1-second buffered sampling window with animated progress to compute accurate time-averaged mean readings and reduce hand jitter.
- **History**: Per-quantity isolated records, debounced name search, reading detail sheets with physical interpretations, in-place renaming with validation, and deletion with an interactive **Undo** snackbar.
- **Statistics & Native Canvas Chart**: Persisted historical analytics (count, min, max, average, latest) alongside a high-performance native Canvas timeline chart featuring touch tooltips, auto-scaling, and point downsampling.

---

### 2. 📊 Lab Dashboard
- **Telemetry Cards**: Live availability detection for all sensors, saved capture count, and latest timestamped value.
- **Direct Navigation**: Tap any card to navigate immediately to that sensor's workspace.
- **Physics Explanations Bottom Sheet**: In-app physics guide detailing scientific units, formulas, environmental disturbances, and everyday benchmarks.

---

### 3. 🛠️ Legacy Sensor Experiments Suite
Preserved and accessible under `LegacyMainActivity`:
- **Gyroscope**: 3-axis angular velocity in $\text{rad/s}$.
- **Digital Compass**: Directional heading needle with degrees.
- **Barometer**: Atmospheric pressure in $\text{hPa}$ and barometric altitude estimation.
- **Step Counter**: Hardware step detector log.
- **Acoustic Tools**: Sound frequency analysis (FFT) and ambient decibel ($\text{dB}$) sound meter.
- **Digital Ruler**: CameraX-assisted overlay for screen-based distance calibration.
- **Reaction Timer**: Visual stimulus reflex measurement tool.

---

## 🏛️ Architecture & Best Practices

- **MVVM Pattern**: Clean separation of concerns between UI, ViewModels, and Repository layers.
- **Lifecycle-Aware Coroutines**: Telemetry collection is strictly bound to `repeatOnLifecycle(Lifecycle.State.STARTED)` and `SharingStarted.WhileSubscribed(1000L)`. Sensors are automatically unregistered when screens go into the background, preventing battery drain.
- **State Preservation**: Uses `SavedStateHandle` and `onSaveInstanceState` to preserve active tab indices, capture states, and search queries across device rotation and process death.
- **Light Theme Only**:
  - Strictly follows clean, light-mode aesthetics (off-white backgrounds `#F1F5F9`, card surfaces `#FFFFFF`, and sky blue accents `#0284C7`).
  - Dark mode and system inversion are explicitly disabled (`forceDarkAllowed=false`, `MODE_NIGHT_NO`).
- **Offline & Private**: Zero cloud dependencies, zero analytics trackers, and zero `INTERNET` permissions. All data remains locally stored on device.

---

## 💾 Database & Migrations

PhysicsLab uses **Room** with schema export enabled:
- **Version 1 (`readings`)**: Stores legacy experiment records.
- **Version 2 (`sensor_readings`)**: Domain-specific schema supporting multi-axis components, capture windows, sample counts, and sensor accuracy.
- **Seamless Data Migration (`MIGRATION_1_2`)**: Compatible legacy rows are automatically copied into `sensor_readings` during upgrade, ensuring no historical user data is lost.
- Verified using AndroidX `MigrationTestHelper`.

---

## 🧪 Testing & Verification

The project includes unit test suites and instrumented test support:

| Suite | Focus Area |
| :--- | :--- |
| `VectorMathTest` | Resultant calculation, Pythagoras accuracy, and vector invariants |
| `CaptureWindowTest` | Multi-sample buffering, component averaging, and validation |
| `LikeEscaperTest` | SQL wildcard sanitization for SQLite queries |
| `NameValidatorTest` | Input trimming, length constraints, and blank rejection |
| `PhysicsExplanationsTest` | Factual descriptions, formulas, and baseline constants |
| `StatsResultTest` | Aggregations, min/max calculations, and empty-state safety |
| `ChartScaleHelperTest` | Native canvas scaling, nice numbers, and downsampling |
| `SensorSourceTest` / `Throttler` | Telemetry flow emissions, throttling, and state transitions |
| `QuantityViewModelTest` | StateFlow emissions, capture states, and history search |
| `DashboardViewModelTest` | Aggregations across multi-sensor repositories |
| `AppDatabaseMigrationTest` | SQLite migration 1 → 2 validation via `MigrationTestHelper` |

Run tests locally:
```bash
# Execute unit tests
./gradlew testDebugUnitTest

# Run code style & lint inspection
./gradlew lintDebug

# Assemble debug instrumented test APK
./gradlew assembleDebugAndroidTest
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog / Ladybug or newer
- Android SDK 34
- JDK 17 (recommended for Gradle 8+)
- Android device or emulator running Android 5.0+ (API 21+)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/supriyamulik/PhysicsLab.git
   cd PhysicsLab
   ```
2. Build and run the debug app:
   ```bash
   ./gradlew installDebug
   ```
3. Or build the minified release APK (signed with debug key for local testing):
   ```bash
   ./gradlew assembleRelease
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

---

## 📄 License
This project is open-source under the MIT License. See [LICENSE](LICENSE) for details.
