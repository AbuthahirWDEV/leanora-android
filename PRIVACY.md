# Leanora privacy notice

Leanora is a wellness aid. It is not a medical device.

## What stays on your device
Your profile (name, age, sex, height, weight), food diary, water log, weight history, favorites and settings are stored only in the app's private storage on your phone. They are excluded from Android cloud backup. Use **Settings → Privacy & app → Delete all my data** to erase everything.

## What leaves your device
Only when you use an AI feature **and have allowed AI features**:

| Feature | Data sent to the Leanora server (and on to Google Gemini) |
|---|---|
| Voice food logging | The text recognized from your speech |
| Food photo estimate | The photo you capture (not saved to your gallery) |
| AI coach | Your question, recent chat turns, today's calorie/macro totals, calorie goal, fitness goal, current and target weight, and names of recently logged foods |

Barcode lookups send the barcode number to Open Food Facts. Speech recognition is performed by your device's speech service (usually Google).

You can turn AI features off at any time in Settings. The app keeps working for manual logging, barcode lookup and tracking without them.

## Server handling
The backend (see `backend/`) forwards requests to Google Gemini and does not store them by design; Render and Google may keep operational logs under their own policies. If you self-host, you control the server and its logs.

## Age and safety
Leanora is intended for people 13 and over. Under-18 users only receive a maintenance calorie target. The app will not set targets that imply an underweight BMI, and it does not replace advice from a doctor or dietitian.
