# Hypertrophy AI ⚡ Biomechanical 3D Android Coach

> **Autonomous Hypertrophy & Progressive Overload Engine powered by Google Gemini 4 Hydrogen (`gemini-4-hydrogen`) and Jetpack Compose 3D Canvas.**

[![Platform](https://img.shields.io/badge/Platform-Android%2014%20--%2017%20(API%2034--37)-brightgreen.svg)](https://developer.android.com)
[![Engine](https://img.shields.io/badge/Gemini%20API-Gemini%204%20Hydrogen-blue.svg)](https://ai.google.dev)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Room%20%2B%20Coroutines%20Flow-orange.svg)]()
[![Design](https://img.shields.io/badge/Design-Fitness%20Editorial%20AMOLED%20Volt%20Lime-yellow.svg)]()
[![Glance](https://img.shields.io/badge/Widget-Jetpack%20Glance%20AppWidget-blueviolet.svg)]()

---

## 🌟 Key Highlights

### 1. 🦾 Volumetric 3D Stylized Mannequin Rig
- **True Anatomical Depth:** Bilateral left/right limb separation ($Z \pm 22\text{ units}$), volumetric cylindrical limbs, 3D head sphere, and ribcage geometry — eliminating 1D/2D flat collapse during rotation.
- **Dynamic 360° Orbit & Pinch-to-Zoom:** Touch-drag camera orbit around the lifter with real-time perspective division.
- **Angle Preset Chips:** One-tap snap to **Side Profile (90°)**, **Front Profile (0°)**, **Top View (70°)**, and **3/4 Isometric Perspective (45°)**.
- **Simultaneous Dual Synchronized View:** Renders side-by-side or stacked viewports (e.g. Side + Front) driven by a single unified rep phase $k \in [0.0, 1.0]$.
- **⚡ X-Ray Equipment Ghosting:** Renders heavy equipment at 30% alpha with subtle rim lighting so lifter joint angles (elbow flexion, spinal alignment, knee path) remain visible through the bench or rack.

### 2. 🏋️ Procedural 3D Gym Equipment Suite
- **Olympic Barbell:** 2200mm steel bar cylinder, knurled grip markers, and 450mm diameter 3D circular bumper plates with rotating collar clamps (plate count dynamically scales with logged weight kg).
- **Hex Dumbbells:** Solid hexagonal weight heads with knurled steel handles.
- **Adjustable Workout Bench:** 3D padded backrest with dynamic incline angles ($0^\circ$ flat, $30^\circ/45^\circ$ incline, $85^\circ$ seated).
- **Power Cage / Squat Rack:** 4 vertical steel box columns, crossbeams, and J-hooks positioned at barbell height.
- **Gym Floor Grid:** Perspective ground grid with drop shadows.

### 3. ⏱️ Precision Scrubber & Speed Machine
- **Interactive Scrub Slider:** Drag to freeze-frame inspect critical inflection points (deep squat stretch, chest touch on bench press, lockout).
- **Speed Multipliers:** `0.25x Slo-Mo`, `0.5x Form Check`, `1.0x Normal`, `1.5x Fast`, `2.0x Tempo`.
- **Play / Pause Toggle:** Seamless frame pause and resumption.

### 4. 🧠 Gemini 4 Hydrogen AI Coach & Auto-Tuning
- **6-Step Biomechanical Onboarding Intake:** Assesses prior joint pain/injuries, target weak points, weekly frequency (3-6 days), session duration budget, available equipment, and an **Open Priority Free-Form Text Box** (e.g. *"Focus on upper chest thickness and biceps peak while keeping stress off lower back"*).
- **7-Day Dynamic Hypertrophy Generator:** Gemini 4 Hydrogen synthesizes an entire personalized weekly split with optimal stimulus-to-fatigue ratios (SFR), loaded directly into Room DB.
- **Intra-Workout Real-Time Coach:** Dynamically evaluates sets, reps, kg, and RIR to offer real-time fatigue warnings and rest recommendations.
- **Next-Day AI Auto-Tuning (`tuneNextDaySession`):** Analyzes historical sets, recovery readiness, and active soreness to dynamically configure tomorrow's session.
- **Biomechanical Exercise Tuning (`tuneExercise`):** Custom prompt-based fine-tuning of 3D joint trajectories and equipment configurations.

### 5. 📳 Biomechanical Audio-Tactile Suite (`FeedbackManager`)
- **Zero-Latency AudioTrack Synthesis:** Pure mathematical sine wave bursts (no external asset dependencies or audio decoding delay).
- **Cadence Metronome Ticks:** Audio-tactile click at rep inflection (eccentric-to-concentric transition).
- **Countdown Warning Beeps:** Dual-frequency pings at 3s, 2s, 1s rest remaining.
- **Victory Chime:** Polyphonic harmonic synth chord ($C_5 \to E_5 \to G_5$) + double-pulse confirm vibration upon rest timer completion.
- **Slider Notch Haptics:** Micro-mechanical clicks when scrubbing frames or switching speeds.

### 6. 📱 Android 17, Samsung Galaxy A55 5G & Nothing OS Optimization
- **Samsung Galaxy A55 5G (Exynos 1480 / AMD Xclipse 530):** Full edge-to-edge `WindowInsets.safeDrawing` protection preventing camera punch-hole cutout clipping, 1080×2340 19.5:9 display scaling, and 120Hz canvas frame budgeting.
- **Nothing Phone (4a) Pro (Android 17):** Native tactile haptics via `VibratorManager`.
- **4x2 Jetpack Glance Home Screen Widget:** Displays today's sets, active workout volume (kg), streak badge, and 1-tap launcher into the AI Coach.

---

## 📐 Technology Stack

- **UI & Animation:** Jetpack Compose 1.8+, Material 3 Expressive, Software 3D Vector Math on Canvas.
- **AI Integration:** Google Gemini Interactions & Content Generation API (`gemini-4-hydrogen`, `gemini-3.8-flash`, `gemini-3.5-flash-lite`) via OkHttp & Kotlinx Serialization.
- **Database & Persistence:** AndroidX Room 2.7 (KSP Compiler) + DataStore / SharedPreferences.
- **Widget:** Jetpack Glance AppWidget 1.1.1.
- **Haptics & Audio:** Android `VibratorManager`, `VibrationEffect`, `AudioTrack` PCM tone synthesis.
- **Build System:** Gradle 8.13, AGP 8.13, Kotlin 2.2.20, Java 21, Android SDK 37 (compileSdk 37, minSdk 31).

---

## 🚀 Getting Started

### Prerequisites
1. Android Studio Ladybug / Meerkat (or official Android CLI).
2. Android SDK Platforms 34 through 37.
3. JDK 21.
4. A Google Gemini API Key from [Google AI Studio](https://aistudio.google.com/).

### Building & Running
```bash
# Clone the repository
git clone https://github.com/AmitKumarMyT/hypertrophy-ai-android.git
cd hypertrophy-ai-android

# Build Debug APK
./gradlew assembleDebug

# Deploy to connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Configuring the Gemini API Key
1. Launch the app on your Android device.
2. Complete the initial 6-step Biomechanical Onboarding Survey.
3. Tap the **Settings** icon (top right) and paste your **Gemini API Key**.
4. Select your preferred engine (`Gemini 4 Hydrogen` recommended) and Thinking Level (`High`).

---

## 📄 License
This project is licensed under the Apache License 2.0.
