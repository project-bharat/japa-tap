# Japa Tap — Bhajan Japa on Tap

Standalone native Android app for mantra japa counting. This repository is a clean migration target; it does not alter the original Counter app.

## Current foundation
- Native Kotlin + Jetpack Compose UI
- Offline Room persistence for mantra records and tap history
- Per-mantra daily Mala target and lifetime Japa target
- 108-count Mala ring, tap counter, undo last tap
- Per-mantra milestone rank and badge gallery concept
- Placeholder visuals are intentionally replaceable

## Planned migration
1. Japa UI polish and mantra selection/custom mantra data
2. Voice counting, offline speech models, model download lifecycle, and language settings migrated from Counter app
3. Streak / practice-day achievements, richer badge gallery and progress summaries
4. User-provided branding assets and splash/launcher replacement

Build debug APK with Android Studio or `gradle assembleDebug` (Java 17, Android SDK 35).
