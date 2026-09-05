# AlcoLarm

Android recovery-support app to help avoid alcohol relapse.

**Stack:** Kotlin · Jetpack Compose · Hilt · multi-module Gradle (Kotlin DSL)  
**Design:** Clear Signal — dark base, high contrast, amber accent, stress-ready dial  
**Version:** `0.3.0-mvp` (versionCode 4)

## MVP flow

1. **Splash** → waits for DataStore profile, then onboarding or home
2. **Onboarding** — quit-reason chips (health optional text; family optional text + photo picker)
3. **Risk places** — chips (bar, liquor store, supermarket, …)
4. **Emergency contact** — name + phone; test dial via `ACTION_DIAL`
5. **Home** — summary + **live risk watch** (Fused Location + OpenStreetMap Overpass)
6. **Alert** — AlarmStrip, reasons, family photo preview, large Dial button
7. **Simulate risk alert** — debug builds only (works without location)

Privacy: **live location only** (current sample in memory — never written to DataStore). Backup/cloud extraction disabled. Family photos copied into app-internal storage.

## Live risk detection (v0.3 — free, no API key)

| Piece | Implementation |
|--------|----------------|
| Location | Play Services `FusedLocationProviderClient` (high accuracy, foreground while Home is resumed) — free / on-device |
| Places | **OpenStreetMap Overpass API** (`overpass-api.de`) ~120 m radius — **free, no key** |
| Mapping | `BAR→amenity=bar\|pub\|biergarten`, `LIQUOR_STORE→shop=alcohol\|wine`, `SUPERMARKET→shop=supermarket\|convenience`, `PARTY→amenity=nightclub\|bar` (`HOME_ALONE` / `OTHER` skipped) |
| Debounce | ~2 consecutive hits + 5 min cooldown after alert |
| Background | **Not in MVP** — monitoring pauses when Home is paused; geofencing can come later |

**No Google Places / Maps API key is required.** Google account login does **not** grant free Places Nearby Search for end users — that API is billed. AlcoLarm stays free by querying OSM Overpass with User-Agent `AlcoLarm/0.2 (recovery support app)`.

### OSM rate limits (please be polite)

Public Overpass instances ask for responsible use: identify the app (User-Agent), avoid tight polling, and cache when possible. AlcoLarm checks about every **50 s** (or after ~40 m movement), with an extra **≥10 s** gap between Overpass calls. Do not lower these for production builds.

Google Places Nearby Search is **not** wired in this build. If it is ever re-enabled as an optional paid alternate, it would need a billed `MAPS_API_KEY` in `local.properties` — not the default path.

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

`applicationId`: `com.alcolarm.app` · `versionName`: `0.3.0-mvp`

### How to test live detection

1. Install a fresh debug build (no API key required).
2. Complete onboarding; select at least one detectable risk (e.g. **Bar**).
3. On Home, tap **Allow location** and grant fine/coarse location.
4. Status should show **Watching nearby risk places via open map data…** (foreground).
5. Stand near a mapped bar / liquor store / supermarket (~100 m) for two check cycles (~50 s each, or move ~40 m).
6. Alert opens when a matching OSM place is found. Dismiss → 5 min cooldown.
7. Or use **Simulate risk alert** in debug anytime.

## Design notes (Clear Signal)

- Near-black background (`#0B0D10`), amber primary (`#FFB020`), teal support accent
- Large dial on alert; calm pulse on AlarmStrip — not frantic
- Copy stays supportive (“Pause. You’ve got this.”)

## What’s next

- Background / geofencing while still avoiding a location history trail
- Notification channel for risk events when app is not foreground
- Release signing / Play Store packaging
