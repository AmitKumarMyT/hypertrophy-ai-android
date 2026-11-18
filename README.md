# Hypertrophy Log (native Android)

Kotlin, Jetpack Compose, Material 3 Expressive with wallpaper dynamic color.
compileSdk and targetSdk 37 (Android 17), minSdk 31.

## Build
1. Open this folder in the latest stable Android Studio.
2. Install the Android 17 (API 37) SDK platform when prompted.
3. Accept the Android Gradle Plugin upgrade assistant if it offers one, and bump the
   Compose BOM in app/build.gradle.kts to the newest release.
4. Run on a device or emulator, or Build > Generate Signed App Bundle / APK.

This source was written without a compiler available, so expect to fix a small number of
compile errors on the first build (most likely API names that moved between Compose versions).

## Where data lives
filesDir/tracker.json inside the app sandbox, covered by Android Auto Backup.
Progress > Export log as CSV shares a copy.

## Files
- Model.kt: muscles, exercises, tips, serializable state
- Engine.kt: planner, scoring, progression, persistence
- TrackerVM.kt: session logic (log, pause, restart, per-exercise time, rest timer)
- Anim.kt: the stick-figure exercise animations drawn on a Compose Canvas
- Screens.kt: Today and Progress screens
- Theme.kt: dynamic color plus MotionScheme.expressive()
