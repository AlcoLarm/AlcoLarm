# AlcoLarm

Android recovery-support app to help avoid alcohol relapse.

**Stack:** Kotlin · Jetpack Compose · Hilt · multi-module Gradle (Kotlin DSL)  
**Design:** Clear Signal — dark base, high contrast, amber accent, stress-ready dial  
**Version:** `0.2.0-mvp` (versionCode 3)

## MVP flow

1. **Splash** → waits for DataStore profile, then onboarding or home
2. **Onboarding** — quit-reason chips (health optional text; family optional text + photo picker)
3. **Risk places** — chips (bar, liquor store, supermarket, …)
4. **Emergency contact** — name + phone; test dial via `ACTION_DIAL`
5. **Home** — summary + **live risk watch** (Fused Location + Google Places Nearby Search)
6. **Alert** — AlarmStrip, reasons, family photo preview, large Dial button
7. **Simulate risk alert** — debug builds only (works without location / API key)

Privacy: **live location only** (current sample in memory — never written to DataStore). Backup/cloud extraction disabled. Family photos copied into app-internal storage.

## Live risk detection (v0.2)

| Piece | Implementation |
|--------|----------------|
| Location | Play Services `FusedLocationProviderClient` (high accuracy, foreground while Home is resumed) |
| Places | Google Places **Nearby Search** REST (`maps.googleapis.com`) ~120 m radius |
| Mapping | `BAR→bar`, `LIQUOR_STORE→liquor_store`, `SUPERMARKET→supermarket/convenience_store`, `PARTY→night_club` (`HOME_ALONE` / `OTHER` skipped) |
| Debounce | ~2 consecutive hits + 5 min cooldown after alert |
| Background | **Not in MVP** — monitoring pauses when Home is paused; geofencing can come later |

### Maps API key setup (required for live Places)

1. Create a key in [Google Cloud Console](https://console.cloud.google.com/) with **Places API** (and Maps SDK if desired) enabled.
2. Add to **`local.properties`** (gitignored — never commit the key):

```properties
sdk.dir=/path/to/Android/sdk
MAPS_API_KEY=your_key_here
```

3. Rebuild. The key is injected into `BuildConfig.MAPS_API_KEY` and the `com.google.android.geo.API_KEY` manifest meta-data.

Without a key the app still runs: Home shows **“Add MAPS_API_KEY…”** and **Simulate** works in debug. It does not crash.

## Modules

| Module | Role |
|--------|------|
| `:app` | Application, Hilt, NavHost, API key manifest meta-data |
| `:core:model` | Domain models + display labels |
| `:core:data` | DataStore prefs (**no** location history) |
| `:core:designsystem` | Clear Signal theme + chips / buttons / AlarmStrip / Dial |
| `:feature:onboarding` | Quit reasons |
| `:feature:riskplaces` | Risk place chips |
| `:feature:emergency` | Emergency contact + dial |
| `:feature:location` | Home, Fused location, Places detector, Simulate |
| `:feature:alert` | Alert screen |

## Build / run

```bash
./gradlew :app:assembleDebug
```

**Requirements:** JDK 17, Android SDK compile/target 35, minSdk 26.

### Install a debug APK

```bash
adb install -r /path/to/AlcoLarm-debug.apk
```

`applicationId`: `com.alcolarm.app` · `versionName`: `0.2.0-mvp`

### How to test live detection

1. Put `MAPS_API_KEY` in `local.properties` and install a fresh debug build.
2. Complete onboarding; select at least one detectable risk (e.g. **Bar**).
3. On Home, tap **Allow location** and grant fine/coarse location.
4. Status should show **Watching nearby…** (foreground).
5. Stand near a mapped bar / liquor store / supermarket (~100 m) for two check cycles (~50 s each, or move ~40 m).
6. Alert opens when a matching Place is found. Dismiss → 5 min cooldown.
7. Or use **Simulate risk alert** in debug anytime.

## Design notes (Clear Signal)

- Near-black background (`#0B0D10`), amber primary (`#FFB020`), teal support accent
- Large dial on alert; calm pulse on AlarmStrip — not frantic
- Copy stays supportive (“Pause. You’ve got this.”)

## What’s next

- Background / geofencing while still avoiding a location history trail
- Notification channel for risk events when app is not foreground
- Release signing / Play Store packaging
