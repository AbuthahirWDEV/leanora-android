# Leanora Android

Native Android nutrition and wellness tracker built with Kotlin + Jetpack Compose.

## Android features
- Daily calorie and macro dashboard
- Food diary with local persistence
- Food catalog with common foods
- Add food through Android speech recognition
- Camera food photo estimate (requires configured backend)
- Barcode scan with Google ML Kit Code Scanner and Open Food Facts lookup
- Weight updates and dated weight history
- Dark theme toggle
- Gemini AI nutrition coach via secure FastAPI backend
- Text-to-speech for coach responses

## Build APK
Open this repository in Android Studio, sync Gradle, then:
- Build > Build Bundle(s) / APK(s) > Build APK(s)
- APK: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions is configured to build and upload the debug APK on pushes to main.

## Backend (AI Coach and food-photo estimate)
1. Deploy the `backend/` directory to Render using the root `render.yaml` blueprint.
2. Add `GEMINI_API_KEY` as a secret environment variable in Render. Never put it in the Android app or commit it.
3. Wait for the service to become healthy at `/health`.
4. Copy the HTTPS service URL into Leanora > Settings > AI backend URL.

Endpoints:
- `GET /health`
- `POST /v1/coach`
- `POST /v1/food-estimate` (multipart image upload)

## Important
Food nutrition values and photo estimates are approximate. Barcode data from Open Food Facts may be incomplete. This app is a nutrition aid, not medical advice. AI features require a Gemini API key on the backend.
