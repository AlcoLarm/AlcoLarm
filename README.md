# AlcoLarm

Android recovery-support app to help avoid alcohol relapse.

**Stack:** Kotlin · Jetpack Compose · Hilt · multi-module Gradle (Kotlin DSL)  
**Design:** Clear Signal — dark base, high contrast, amber accent, stress-ready dial  
**Version:** `0.3.2-mvp` (versionCode 6)

## MVP flow

1. **Splash** → waits for DataStore profile, then onboarding or home
2. **Onboarding** — quit-reason chips (health optional text; family optional text + photo picker)
3. **Risk places** — chips (bar, liquor store, supermarket, …)
4. **Emergency contact** — name + phone; test dial via `ACTION_DIAL`
5. **Home** — summary + **live risk watch** (Fused Location + OpenStreetMap Overpass)
6. **Alert** — AlarmStrip, reasons, family photo preview, large Dial button
7. **Simulate risk alert** — debug builds only (works without location; immediate)

Privacy: **live location only** (current sample + short in-memory ring for still/dwell — never written to DataStore). Backup/cloud extraction disabled. Family photos copied into app-internal storage.

## Live risk detection (v0.3.2 — stop + dwell + call-style alert)

| Piece | Implementation |
|--------|----------------|
| Location | Play Services `FusedLocationProviderClient` (high accuracy, foreground while Home is resumed) — includes **speed** when available |
| Places | **OpenStreetMap Overpass API** (`overpass-api.de`) **SEARCH_RADIUS ≈ 120 m** — **free, no key** |
| Mapping | `BAR→amenity=bar\|pub\|biergarten`, `LIQUOR_STORE→shop=alcohol\|wine`, `SUPERMARKET→shop=supermarket\|convenience`, `PARTY→amenity=nightclub\|bar` (`HOME_ALONE` / `OTHER` skipped) |
| Still | `STOP_SPEED_MPS = 0.7` when speed is present; else displacement &lt; ~18 m over last ~30 s (in-memory ring only) |
| Dwell | Still **and** nearby continuously for `DWELL_REQUIRED_MS = 15_000` (~15 s); pass-by / leave radius resets the timer |
| Alert UX | Default **ringtone** + call-like vibration + high-priority notification with `fullScreenIntent`; lock-screen title is anonymous (**Incoming call** / emergency contact name) — not alcohol/risk wording |
| Cooldown | 5 min after dismiss |
| Background | **Not in MVP** — monitoring pauses when Home is paused; geofencing can come later |

**No Google Places / Maps API key is required.** AlcoLarm stays free by querying OSM Overpass with User-Agent `AlcoLarm/0.3 (recovery support app)`.

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
| `:feature:location` | Home, Fused location, OSM Overpass detector, Simulate |
| `:feature:alert` | Alert screen |

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

`applicationId`: `com.alcolarm.app` · `versionName`: `0.3.2-mvp`

### How to test live detection

1. Install a fresh debug build (no API key required).
2. Complete onboarding; select at least one detectable risk (e.g. **Bar**).
3. On Home, tap **Allow location** and grant fine/coarse location.
4. Status should show **Watching nearby risk places via open map data…** (foreground).
5. **Stand still** near a mapped bar / liquor store / supermarket (~120 m) for about **15 seconds**. Status becomes **Near — confirming you’ve stopped…** while dwell accumulates. Walking or driving past should **not** alert.
6. Alert opens with **ringtone + call-style vibration** (and an anonymous “Incoming call” notification). In-app Alert keeps supportive recovery content. Dial / I’m OK / leave Alert stops the ring. Dismiss → 5 min cooldown.
7. Grant **Notifications** (Android 13+) so the call-style notification / full-screen intent can appear.
8. Or use **Simulate risk alert** in debug anytime (immediate; unchanged).

## Design notes (Clear Signal)

- Near-black background (`#0B0D10`), amber primary (`#FFB020`), teal support accent
- Large dial on alert; calm pulse on AlarmStrip — not frantic
- Copy stays supportive (“Pause. You’ve got this.”)

## What’s next

- Background / geofencing while still avoiding a location history trail
- Notification channel for risk events when app is not foreground
- Release signing / Play Store packaging
