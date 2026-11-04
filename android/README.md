# Teamily AI — Android (Jetpack Compose)

Implements the design in [teamily-android-ui](https://github.com/KRFbrothers/teamily-android-ui)
(`DESIGN_SPEC.md`): day/night tokens, 14 screens, bottom nav, persisted theme mode (`teamily-theme-mode`).

## Run
Open this folder in **Android Studio (Koala+)**, let Gradle sync (it creates the wrapper), then Run on a device/emulator (API 26+).

## Backend
Start the FastAPI backend from `teamly-ai/backend` (`uvicorn app.main:app --host 0.0.0.0`, with `JWT_SECRET` and `OPENAI_API_KEY` set).
The app signs in via `/auth/*` and the Chat screen calls `/ai/assist`. `Api.BASE_URL` defaults to `http://10.0.2.2:8000` (emulator → host); set your machine's LAN IP for a real device. Debug builds allow cleartext HTTP; release builds need HTTPS.

## Status
- Agent/chat lists and profile data are still local sample data (`Data.kt`); only auth and chat replies are live.
- Next: server-side chats/agents, conversation history, streaming replies.
- Fonts: uses the system font. Add Lato **TTF** subsets (Android does not load the repo's woff2) in `res/font` and set `T.font` in `ui/Theme.kt`.
- Icons: Material core icons stand in for the spec's custom 46-glyph set.
- Not yet compiled in CI — the authoring environment had no Android SDK.
