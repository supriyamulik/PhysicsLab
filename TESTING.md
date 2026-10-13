# PhysicsLab — Manual Device Testing & QA Verification Checklist

This document provides a comprehensive, step-by-step verification checklist for validating **PhysicsLab** on physical Android devices and Android Emulators.

---

## 1. Test Environment Setup

- **Physical Device:** Android phone running Android 5.0 (API 21) or higher.
- **Android Emulator (Alternative):**
  - Android Virtual Device (AVD) running API 30–34 with Google APIs.
  - Open **Extended Controls** (`...` icon on emulator toolbar) → **Virtual Sensors**.
  - Use the sliders and controls for **Accelerometer**, **Magnetometer**, and **Ambient Light**.

---

## 2. Sensor Live Telemetry Verification

### 2.1 Magnetic Field (`Quantity.MAGNETIC_FIELD`)
- [ ] **Rest / Baseline:**
  - Place phone flat on a wooden/non-metallic table away from electronics.
  - Verify hero resultant magnitude $|B|$ reads within approximate Earth's field (**25 to 65 µT**).
  - Verify components ($B_x$, $B_y$, $B_z$) are displayed in µT with 1–2 decimal places.
  - Verify accuracy status badge is visible (e.g. `HIGH Accuracy` or `MEDIUM Accuracy`).
- [ ] **Disturbance / Magnet Nearby:**
  - Bring a small magnet, magnetic phone mount, or laptop speaker close to the phone.
  - Verify resultant reading immediately spikes (**>100–500+ µT**).
  - If accuracy drops to `LOW` or `UNRELIABLE`, verify calibration notice appears:
    *"Move away from magnets or metal objects and gently wave phone in a figure-8 to calibrate."*
- [ ] **Recalibration (Figure-8 Gesture):**
  - Remove magnet and wave device in a smooth figure-8 motion for 3–5 seconds.
  - Verify accuracy status returns to `HIGH` or `MEDIUM` and calibration notice disappears.
- [ ] **Physics Info Bottom Sheet:**
  - Tap info button (`ℹ`) in toolbar.
  - Verify formula $B = \sqrt{B_x^2 + B_y^2 + B_z^2}$, SI unit (Tesla / µT), Earth's range (25–65 µT), disturbance list, and device magnetometer hardware specs (name, vendor, max range, resolution) appear.

---

### 2.2 Acceleration (`Quantity.ACCELERATION`)
- [ ] **Rest / Gravity Invariance:**
  - Place phone flat on a table, screen facing up.
  - Verify hero resultant $|a|$ reads **~9.81 m/s²** (within 9.7–9.9 m/s²).
  - Verify $a_z \approx +9.8\ \text{m/s}^2$ while $a_x \approx 0.0\ \text{m/s}^2$ and $a_y \approx 0.0\ \text{m/s}^2$.
  - Verify subtext clearly states: *"Includes Earth's gravity (≈ 9.81 m/s² at rest)"*.
- [ ] **Device Motion & Gestures:**
  - Lift and gently shake phone or tilt vertically.
  - Verify resultant dynamically reacts (**12 to 25+ m/s²**).
  - In landscape / vertical orientations, verify the gravity vector shifts between $a_x$, $a_y$, and $a_z$.
- [ ] **Free-Fall Condition:**
  - *Emulator:* In Virtual Sensors, set Accelerometer $X=0, Y=0, Z=0$.
  - *Physical device (careful!):* Drop phone onto a soft mattress or pillow from 10–20 cm.
  - Verify during free fall all axes drop toward **~0 m/s²**.
- [ ] **Physics Info Bottom Sheet:**
  - Tap info button (`ℹ`).
  - Verify formula $a = \sqrt{a_x^2 + a_y^2 + a_z^2}$, gravity explanation ($\approx 9.81\ \text{m/s}^2$), and free fall behavior near 0 are clearly documented.

---

### 2.3 Ambient Light (`Quantity.LIGHT`)
- [ ] **Scalar Value & Unit:**
  - Verify single scalar value in **lux** (no $X/Y/Z$ components or resultant).
  - Verify number formatting: 2 decimals below 10 lux (e.g. `4.25 lux`), 1 decimal up to 1000 lux (e.g. `340.5 lux`), grouped integers above 1000 lux (e.g. `12,500 lux`).
- [ ] **Indoor / Covered / Bright Tests:**
  - Normal room lighting: verify reading reads **100–500 lux**.
  - Completely cover top bezel sensor with thumb: verify reading drops to **0.0–1.0 lux**.
  - Shine a flashlight or direct sunlight onto top sensor: verify reading spikes to **>5,000–50,000+ lux**.
- [ ] **Uncalibrated Sensor Notice:**
  - Verify note on screen: *"Phone light sensors are uncalibrated and indicative."*
- [ ] **Physics Info Bottom Sheet:**
  - Tap info button (`ℹ`).
  - Verify definition ($1\ \text{lux} = 1\ \text{lumen}/\text{m}^2$), everyday reference ranges (dim indoor, office, overcast, sunlight), and directional photodiode note appear.

---

### 2.4 Unsupported Sensor Graceful Degradation
- [ ] **Hardware Missing / Disabled:**
  - If testing on an emulator or specialized tablet missing a magnetometer or light sensor:
  - Verify app **does not crash**.
  - Verify attractive "Sensor Not Available" card is displayed explaining the hardware sensor is absent.
  - Verify "+ Save Reading" button is disabled (`isEnabled=false`, dimmed alpha).
  - On Dashboard: verify corresponding sensor card displays `"Not Supported"` badge in red (`@color/lab_danger`).

---

## 3. Save, Capture Window & Double-Save Protection

- [ ] **Save Reading Trigger:**
  - Tap `+ Save Reading` on any quantity screen.
  - Verify modal name dialog appears prefilled with automatic default name (e.g. `Magnetic Field Oct 14, 1:15 AM`).
- [ ] **Capture Window (~1 Second):**
  - Tap `Save` in dialog.
  - Verify circular progress dialog shows progress bar advancing from **0% to 100%** over ~1 second.
  - Verify samples are collected and averaged across the ~1-second window.
  - Verify Snackbar appears: `Saved: "Reading Name" (<Value> <Unit>)`.
- [ ] **Double-Save Race Protection:**
  - Rapidly tap `Save` repeatedly in dialog or tap `+ Save Reading` rapidly.
  - Verify progress window executes once; multiple duplicate records are **never** created.

---

## 4. History: Search, Rename, Delete & Undo

- [ ] **History List:**
  - Navigate to `History` tab.
  - Verify readings are ordered **newest first**.
  - Verify each item shows: Name, formatted date/time (device locale), and headline value with unit.
- [ ] **Search:**
  - Type a query into search field.
  - Verify results debounce by ~250 ms and filter case-insensitively.
  - Search for non-existent text: verify empty state shows `"No readings matching '<query>'"` (distinct from `"No readings saved yet"`).
- [ ] **Detail Bottom Sheet:**
  - Tap any reading item in the list.
  - Verify detail sheet displays: Name, full date & time, component values ($X, Y, Z$), resultant/illuminance, physical unit, sensor hardware model, sample count, accuracy level, and conservative physical interpretation line.
- [ ] **Rename:**
  - From history card or detail sheet, tap `Rename`.
  - Try entering an empty/whitespace name: verify validation error prevents submission.
  - Enter name > 60 characters: verify validation enforces 60 character limit.
  - Enter valid new name and confirm: verify name immediately updates in History list, detail view, and Statistics without restarting or manually refreshing.
- [ ] **Delete & Undo:**
  - Tap `Delete` on a reading item; confirm deletion dialog.
  - Verify reading disappears immediately from list and Statistics update instantly.
  - Verify Snackbar displays: `Deleted "<Reading Name>"` with an `Undo` button.
  - Tap `Undo`: verify reading is re-inserted and immediately reappears in History and Statistics.

---

## 5. Statistics & Canvas Chart

- [ ] **0 Readings (Empty State):**
  - Delete all readings for a quantity (or fresh install).
  - Navigate to `Statistics` tab.
  - Verify empty state is displayed: *"No saved readings yet — Capture readings in the Live tab to view statistics."* (No broken or empty chart drawn).
- [ ] **1 Reading:**
  - Save exactly 1 reading.
  - Navigate to `Statistics` tab.
  - Verify Min, Max, Average, and Latest all equal the single reading's value.
  - Verify timeline chart renders a single centered point with horizontal guidelines — **no broken or infinite lines**.
- [ ] **2+ Readings (Multi-point Line Chart):**
  - Save 3–5 readings at different levels.
  - Verify chart draws smooth line connecting points with filled gradient area.
  - Touch/drag finger across the chart: verify closest point highlights with a circular halo and vertical guideline.
  - Verify floating tooltip displays reading name, formatted value with unit, and date/time.
- [ ] **50+ Readings Cap:**
  - Verify header indicator states: `"Latest 50 of <N> readings"`.
  - Chart stays fast, readable, and responsive without downsampling lag.

---

## 6. Dashboard Integration

- [ ] **Real Persisted Metrics:**
  - Navigate to `Dashboard` tab.
  - Verify 3 sensor cards (Magnetic Field, Acceleration, Light).
  - Verify each card shows: icon, quantity name, unit, sensor availability badge (`Supported` / `Not Supported`), saved-reading count, latest saved value + date.
  - Verify **no fake or placeholder numbers** appear.
- [ ] **Global Empty State Banner:**
  - When total saved readings across all 3 sensors is 0: verify welcome banner is displayed.
  - When at least 1 reading is saved: verify welcome banner is hidden (`GONE`).
- [ ] **Card Tap Targets:**
  - Tap Magnetic Field card → switches to Magnetic Field tab.
  - Tap Acceleration card → switches to Acceleration tab.
  - Tap Light card → switches to Light tab.
- [ ] **Physics Reference Guide Section:**
  - Scroll down Dashboard to "Physics Reference Guide".
  - Tap any of the 3 summary guide cards: verify corresponding `PhysicsInfoBottomSheet` opens.

---

## 7. Device Orientation & Lifecycle Resilience

- [ ] **Screen Rotation:**
  - On Live tab with active sensor stream, rotate device from Portrait to Landscape and back.
  - Verify sensor continues streaming without crash or freezing.
  - Verify circular gauge and sparkline re-layout cleanly.
  - Verify 1-second grace period (`SharingStarted.WhileSubscribed(1000L)`) prevents sensor listener thrashing.
- [ ] **Bottom Tab Switching:**
  - Switch rapidly between Dashboard, Magnetic, Acceleration, and Light.
  - Verify state is preserved (ViewPager2 `offscreenPageLimit = 3`).
- [ ] **System Back Button:**
  - From History/Statistics tab, press back button: returns to Live tab.
  - From Live tab on any sensor screen, press back button: returns to Dashboard.
  - From Dashboard, press back button: exits app cleanly.
- [ ] **Process Death & Cold Restart:**
  - Save several readings across sensors.
  - Open device App Switcher (Recents) and swipe PhysicsLab away (force close).
  - Relaunch PhysicsLab from home screen.
  - Verify Room SQLite database persisted all records; dashboard shows exact saved counts and latest readings.

---

## 8. Performance Stress Test (1,000 Readings)

- [ ] **Stress Test Data Ingestion:**
  - Populate 1,000 readings into Room `sensor_readings` table.
- [ ] **Verification:**
  - Navigate to History tab: verify RecyclerView scrolls with buttery 60/120 fps smoothness (ViewBinding ViewHolder recycling).
  - Search query filtering responds with debounced speed without dropping frames.
  - Statistics tab loads immediately: `ChartScaleHelper` caps timeline chart to latest 50 readings; memory and CPU remain low.
