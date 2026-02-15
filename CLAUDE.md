# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./build.sh build     # Build debug APK
./build.sh install   # Build + install on connected device
./build.sh run       # Build + install + launch on device
./build.sh clean     # Clean build artifacts
./gradlew assembleRelease  # Build release APK (signed with debug keystore)
```

Prerequisites: JDK 21+, Android SDK (auto-detected from ANDROID_HOME or standard paths). No Android Studio required.

Release APK is signed with the debug keystore (configured in `build.gradle.kts`). Output: `app/build/outputs/apk/release/app-release.apk`.

The Gemini API key is read from `local.properties` as `GEMINI_API_KEY` and injected via BuildConfig. There are no tests in this project.

## Architecture

Clean Architecture with 3 layers: **Domain → Data → Presentation**. Manual dependency injection in `SnapCalApp.kt` (Application class, service locator pattern).

### Domain Layer (`domain/`)
- **Models**: `FoodAnalysis` (with emoji, dishName, calories, ingredients, macros, notes), `MealEntry`, `UserProfile`, `NutritionGoal`, `DailyNutrition`, `WeightRecord`
- **Repository interfaces**: `FoodAnalysisRepository`, `MealRepository`, `UserProfileRepository`, `SettingsRepository`, `UsageRepository`
- **Use cases**: `AnalyzeFoodUseCase` (image + text), `CorrectAnalysisUseCase`, `SaveMealUseCase`, `GetDailyNutritionUseCase`, `GetNutritionHistoryUseCase`, `ComputeNutritionGoalUseCase`

### Data Layer (`data/`)
- **Remote**: `GeminiApiService` (Gemini 2.0 Flash via raw `HttpURLConnection`, retry with exponential backoff, JSON via `org.json`), `GoogleAuthManager`, `DriveBackupManager`, `DailyBackupWorker`
- **Local**: Room database v3 (`MealEntity`, `WeightEntity`) with migrations 1→2→3, `HealthConnectManager`
- **Repositories**: Implementations of domain interfaces, `FoodAnalysisMapper` converts Gemini JSON to domain models

### Presentation Layer (`presentation/`)
Jetpack Compose + Material 3 with MVVM pattern (ViewModels + StateFlow). ViewModels use `ViewModelProvider.Factory` for DI.

**Navigation** (`NavGraph.kt`): Bottom nav with 3 tabs (Dashboard/Scanner/Profile). Routes: `dashboard`, `home`, `result`, `profile`, `history`. Bottom bar hidden on `result` and `history`.

**Key ViewModels**:
- `FoodAnalysisViewModel`: Shared across home/result screens. Manages `AnalysisUiState` (Idle/Loading/Success/Error), image compression, quota tracking (1500/day free limit), `readOnly` mode for viewing saved meals
- `DashboardViewModel`: Daily nutrition + meals + goals
- `HistoryViewModel`: History + weight + chart data with configurable time ranges (1W/1M/3M/1Y)
- `ProfileViewModel`: Profile, goals, Health Connect sync, Google Sign-In, Drive backup/restore

## Key Patterns

- **No external HTTP/JSON libraries**: Uses `HttpURLConnection` + `org.json` throughout
- **Gemini API**: All prompts request JSON responses with emoji, dishName, totalCalories, ingredients, macros, notes. Language set from `Locale.getDefault().displayLanguage`
- **i18n**: English (`values/strings.xml`) + French (`values-fr/strings.xml`). All user-facing strings use string resources
- **Room migrations**: Always increment version in `AppDatabase.kt` and add migration object when changing entities
- **New dependencies wired in**: `SnapCalApp.kt` (create instance) → exposed as `lateinit var` → injected in `NavGraph.kt` via factory
- **Voice input**: Uses Android's `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (no extra dependencies)

## Tech Stack

Kotlin 2.0.0, AGP 8.7.3, Gradle 8.9, Compose BOM 2024.06.00, Room 2.6.1, compileSdk 35, minSdk 26, targetSdk 34, JVM target 21
