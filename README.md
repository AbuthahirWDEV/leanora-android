# Leanora Android

Leanora is a native Android nutrition and wellness tracker built with Kotlin and Jetpack Compose.

## Features

- Onboarding profile (age, sex, height, weight, goal and activity) with safety checks: under-18 users get a maintenance target only, targets below a healthy BMI are rejected, and calorie goals have a sex-specific floor
- Estimated daily calorie target, plus calorie and macro targets with progress on the dashboard
- Food diary with date navigation and local persistence; the Today tab always shows the current day
- 45-item Indian and everyday food catalog, recents, favorites, custom food entry, edit, copy and delete
- Food logging by voice (English and Tamil) and food photo estimates, both reviewed and editable before they are saved
- In-app CameraX barcode scanning (UPC/EAN) with Open Food Facts lookup and an amount-in-grams prompt; manual barcode entry as a fallback
- Water tracking with a configurable daily goal, saved by day
- Weight tracking, trend chart, 7-day calorie chart, averages and logging streak
- AI nutrition coach with conversation memory, English or Tamil replies and text-to-speech
- Dark mode, AI consent switch, backend URL and access token settings, and one-tap deletion of all local data

Nutrition and AI estimates are approximate. Barcode information may be incomplete or unavailable. Leanora is a wellness aid, not a medical device or substitute for professional advice. See [PRIVACY.md](PRIVACY.md) for what stays on the device and what is sent to AI services.

## Build the Android app

Requirements:
- Android Studio / Android SDK 35
- JDK 17

From the repository root (the Gradle wrapper pins Gradle 8.9):

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions runs the backend tests, the Android unit tests and lint, and builds the debug APK on pushes and pull requests to `main`. Download the artifact named `leanora-debug-apk` from a successful run.

### Release build

`./gradlew assembleRelease` produces a minified, resource-shrunk APK. To sign it, set `LEANORA_KEYSTORE`, `LEANORA_KEYSTORE_PASSWORD`, `LEANORA_KEY_ALIAS` and `LEANORA_KEY_PASSWORD` in the environment; otherwise the output is unsigned. Never commit keystores.

## Barcode scanner

The in-app camera analyzes UPC/EAN retail barcodes only; it does not open Google Lens or scan QR codes. If scanning is unavailable, use **Enter barcode manually**. Product nutrition is looked up through Open Food Facts and can be missing for some products.

## Backend: AI Coach and food estimates

1. Deploy the `backend/` directory using the Render blueprint in `render.yaml`.
2. Configure `GEMINI_API_KEY` as a secret environment variable in Render. Never place this key in the Android app or commit it.
3. Recommended: also set `LEANORA_API_TOKEN` to a long random string, then enter the same value in the app under Settings → AI backend → Access token. Without it, anyone who knows your URL can use your Gemini quota. Requests are also rate limited per client (`RATE_LIMIT_PER_MINUTE`, default 30).
4. Confirm the service is healthy at `/health`.
5. Set the service HTTPS URL in Leanora → Settings → AI backend.

Render's free plan sleeps when idle; the app pings `/health` at launch to wake it, but the first AI request after a long idle period can still take up to a minute.

Endpoints:
- `GET /health`
- `POST /v1/coach` (JSON; optional daily context and up to 8 earlier chat turns)
- `POST /v1/food-estimate` (multipart image upload; JPEG/PNG/WEBP verified by content, max 8 MB)
- `POST /v1/food-text-estimate` (JSON food description in English or Tamil)

Gemini responses use JSON schema output and are validated and clamped server-side. AI endpoints require a valid server-side Gemini API key; the app keeps working for local logging and tracking without them.

Run the backend tests:

```bash
cd backend
pip install -r requirements-dev.txt
python -m pytest
```
