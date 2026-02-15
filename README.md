# Food Calories

A free, open-source alternative to [Foodvisor](https://www.foodvisor.io/). Take a photo of your meal and instantly get a detailed breakdown of ingredients, quantities, macronutrients, and calorie count — powered by Google's Gemini AI.

## Features

- **Snap or pick**: Take a photo with your camera or choose one from your gallery
- **AI-powered analysis**: Uses Gemini 2.0 Flash to identify ingredients and estimate nutritional values
- **Detailed breakdown**: Get per-ingredient calories, total calorie count, and macronutrients (proteins, carbs, fats, fiber)
- **Multilingual**: Supports English and French (adapts to your device language)
- **No account required**: Just bring your own Gemini API key — no sign-up, no subscription, no tracking
- **Fully offline-capable UI**: Only the analysis step requires an internet connection

## Architecture

Built with **Clean Architecture** principles:

```
domain/          Models, repository interfaces, use cases (no Android dependencies)
data/            Gemini REST API client, repository implementations, JSON mapper
presentation/    Jetpack Compose UI, ViewModel, Material 3 theme, navigation
```

**Tech stack**: Kotlin, Jetpack Compose, Material 3, CameraX contracts, Coil, Gemini REST API.

## Getting Started

### Prerequisites

- **JDK 21+**
- **Android SDK** (`ANDROID_HOME` must be set, or SDK installed at `~/Library/Android/sdk`)
- A **Google Gemini API key** — get one free at [Google AI Studio](https://aistudio.google.com/apikey)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/ghostwan/food-calories.git
   cd food-calories
   ```

2. Add your Gemini API key to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

3. Build and run (no Android Studio needed):
   ```bash
   ./build.sh build     # Build the debug APK
   ./build.sh install   # Build + install on connected device
   ./build.sh run       # Build + install + launch
   ./build.sh clean     # Clean build artifacts
   ```

The build script automatically downloads Gradle if needed and generates the wrapper.

## Why?

Apps like Foodvisor charge a subscription for food photo analysis. This project provides the same core functionality for free using the Gemini API generous free tier. Your data stays between you and the Gemini API — no intermediary server, no account, no analytics.

## License

MIT
