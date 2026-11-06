# Leanora Android

Leanora is a native Android nutrition and wellness tracker built with Kotlin and Jetpack Compose.

## Features

- Onboarding profile (age, sex, height, weight, goal and activity)
- Estimated daily calorie target and calorie/macronutrient dashboard
- Food diary with date navigation and local persistence
- Food catalog search, custom food entry, edit and delete
- Food logging by voice (English and Tamil), with AI portion and macro estimation from spoken food descriptions
- Food photo calorie estimate using the backend AI service
- Barcode scanning with Google ML Kit Code Scanner and Open Food Facts nutrition lookup
- Manual barcode entry as a fallback
- Water tracking with a configurable daily hydration goal, saved by diary date
- Weight tracking and progress trend/history
- AI nutrition coach with backend Gemini integration and text-to-speech
- Dark mode and settings for goals, profile, backend URL and hydration target
- Branded launcher icon and CI-generated debug APK

Nutrition and AI estimates are approximate. Barcode information may be incomplete or unavailable. Leanora is a wellness aid, not a medical device or substitute for professional advice.

## Build the Android app

Requirements:
- Android Studio / Android SDK 35
- JDK 17
- Gradle 8.9 (CI uses Gradle 8.9)

From the repository root:

```bash
gradle assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions builds the debug APK on pushes and pull requests to `main`. Download the artifact named `leanora-debug-apk` from the successful workflow run.

## Barcode scanner

The in-app ML Kit scanner uses Google Play services. On first use, Google Play services may download the scanner UI module. If scanning is unavailable, use **Enter barcode manually** as a fallback. Product nutrition is looked up through Open Food Facts and can be missing for some products.

## Backend: AI Coach and food photo estimate

1. Deploy the `backend/` directory using the Render blueprint in `render.yaml`.
2. Configure `GEMINI_API_KEY` as a secret environment variable in Render. Never place this key in the Android app or commit it.
3. Confirm the service is healthy at `/health`.
4. Set the service HTTPS URL in Leanora → Settings → AI backend URL.

Endpoints:
- `GET /health`
- `POST /v1/coach`
- `POST /v1/food-estimate` (multipart image upload; JPEG/PNG/WEBP, max 8 MB)\n- `POST /v1/food-text-estimate` (JSON food description in English or Tamil; estimated calories, macros, portion, and meal category)

AI endpoints require a valid server-side Gemini API key. The app can continue to use local food logging and tracking without AI service availability.
