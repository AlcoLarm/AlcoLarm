# AlcoLarm

Android recovery-support app to help avoid alcohol relapse.

**Stack:** Kotlin · Jetpack Compose · Hilt · multi-module Gradle (Kotlin DSL)  
**Design:** Clear Signal — dark base, high contrast, amber accent, stress-ready dial  
**Version:** `0.4.0-mvp` (versionCode 7)

## MVP flow

1. **Splash** → waits for DataStore profile, then onboarding or home
2. **Onboarding** — quit-reason chips (health optional text; family optional text + photo picker)
3. **Risk places** — chips (bar, liquor store, supermarket, …)
4. **Emergency contact** — name + phone; test dial via `ACTION_DIAL`
5. **Home** — summary + **live risk watch** (Fused Location + OpenStreetMap Overpass)
6. **Alert** — AlarmStrip, reasons, family photo preview, large Dial button
7. **Simulate risk alert** — debug builds only (works without location; immediate)

Privacy: **live location only** (current sample + short in-memory ring for still/dwell — never written to DataStore). Backup/cloud extraction disabled. Family photos copied into app-internal storage.

## Live risk detection (v0.4.0 — background watch)

| Piece | Implementation |
|--------|----------------|
| Location | Play Services `FusedLocationProviderClient` — high accuracy while Home is open; **balanced** (~30–40 s) in background FGS; bumps to ~8 s while near a candidate so a **15 s** dwell still works |
| Places | **OpenStreetMap Overpass API** (`overpass-api.de`) **SEARCH_RADIUS ≈ 120 m** — **free, no key** |
| Mapping | `BAR→amenity=bar\|pub\|biergarten`, `LIQUOR_STORE→shop=alcohol\|wine`, `SUPERMARKET→shop=supermarket\|convenience`, `PARTY→amenity=nightclub\|bar` (`HOME_ALONE` / `OTHER` skipped) |
| Still | `STOP_SPEED_MPS = 0.7` when speed is present; else displacement &lt; ~18 m over last ~30 s (in-memory ring only) |
| Dwell | Still **and** nearby continuously for `DWELL_REQUIRED_MS = 15_000` (~15 s); pass-by / leave radius resets the timer |
| Alert UX | Default **ringtone** + call-like vibration + high-priority notification with `fullScreenIntent`; lock-screen title is anonymous (**Incoming call** / emergency contact name) — not alcohol/risk wording |
| Cooldown | 5 min after dismiss |
| Background | **`RiskWatchService`** foreground service (`foregroundServiceType=location`) hosts `RiskWatchEngine`. Home **ON_PAUSE does not stop** monitoring when background watch is active. Ongoing notification: **“AlcoLarm is active” / “Location on”** (never alcohol / relapse / risk / recovery) |

**No Google Places / Maps API key is required.** AlcoLarm stays free by querying OSM Overpass with User-Agent `AlcoLarm/0.4 (recovery support app)`.

### Permissions the user must grant

1. **Location (precise / while in use)** — `ACCESS_FINE_LOCATION` (and coarse) — required first  
2. **Background location (“Allow all the time”)** — `ACCESS_BACKGROUND_LOCATION` (Android 10+) — needed for alerts when the app is closed; Home shows a clear CTA if missing  
3. **Notifications** — `POST_NOTIFICATIONS` (Android 13+) — ongoing “Location on” notice + call-style alert  
4. App also declares `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` for the watch service  

If only while-in-use location is granted, monitoring stays **foreground-only** (pauses when leaving Home) and status reads: *Background location needed for alerts when app is closed*.

### OSM rate limits (please be polite)

Public Overpass instances ask for responsible use: identify the app (User-Agent), avoid tight polling. Local still/dwell evaluates about every **5 s**; Overpass runs only while the user is **still**, about every **25 s** (plus a ≥10 s client gap). Do not lower these for production builds.

## Modules

| Module | Role |
|--------|------|
| `:app` | Application, Hilt, NavHost |
| `:core:model` | Domain models + display labels |
| `:core:data` | DataStore prefs (**no** location history) |
| `:core:designsystem` | Clear Signal theme + chips / buttons / AlarmStrip / Dial |
| `:feature:onboarding` | Quit reasons |
| `:feature:riskplaces` | Risk place chips |
| `:feature:emergency` | Emergency contact + dial |
| `:feature:location` | Home, Fused location, OSM Overpass, `RiskWatchService`, Simulate |
| `:feature:alert` | Alert screen + call-style controller |

## Build / run

```bash
./gradlew :app:assembleDebug
```

**Requirements:** JDK 17, Android SDK compile/target 35, minSdk 26.

No `MAPS_API_KEY` in `local.properties` is needed for live detection.

### Install a debug APK

```bash
adb install -r /path/to/AlcoLarm-debug.apk
```

`applicationId`: `com.alcolarm.app` · `versionName`: `0.4.0-mvp`

### How to test live detection

1. Install a fresh debug build (no API key required).
2. Complete onboarding; select at least one detectable risk (e.g. **Bar**).
3. On Home, tap **Allow location** and grant fine/coarse location.
4. Tap **Allow background location** and set **Allow all the time** in system settings (Android 10+).
5. Status should show **Monitoring: on (background)** and a persistent **“AlcoLarm is active” / “Location on”** notification. Leave Home / press Home — watch must **keep running**.
6. **Stand still** near a mapped bar / liquor store / supermarket (~120 m) for about **15 seconds**. Status becomes **Near — confirming you’ve stopped…** while dwell accumulates. Walking or driving past should **not** alert.
7. Alert opens with **ringtone + call-style vibration** (and an anonymous “Incoming call” notification). In-app Alert keeps supportive recovery content. Dial / I’m OK / leave Alert stops the ring. Dismiss → 5 min cooldown.
8. Grant **Notifications** (Android 13+) so the ongoing location notice and call-style notification / full-screen intent can appear.
9. **Simulate risk alert** (debug) still works from Home without location.
