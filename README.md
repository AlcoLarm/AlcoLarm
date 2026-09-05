# AlcoLarm

Android recovery-support app to help avoid alcohol relapse.

**Stack:** Kotlin · Jetpack Compose · Hilt · multi-module Gradle (Kotlin DSL)  
**Design:** Quiet Companion (cream/sage) + large soft pause banners + warm amber dial  
**Version:** `0.5.6-mvp` (versionCode 14)

## MVP flow

1. **Splash** → waits for DataStore profile, then onboarding or home
2. **Onboarding** — quit-reason chips (health optional text; loved ones optional text + photo picker)
3. **Risk places** — chips (bar, liquor store, supermarket, …)
4. **Emergency contact** — name + phone; test dial via `ACTION_DIAL`
5. **Home** — summary + **live risk watch** (Fused Location + OpenStreetMap Overpass); large soft sage **PAUSE** banner when near risk
6. **Alert** — loved-ones photo fills most of the screen + call-style ringtone/vibrate; large tappable **PAUSE** banner (silences → optional reflection); round warm amber **Call** button places a direct call via `ACTION_CALL` (`CALL_PHONE`; falls back to `ACTION_DIAL` if denied). No auto-dialer on enter.
7. **Dial return** — soft choice *I reached them* / *They didn’t answer* → praise+Home or affirmation + **mandatory** reflection
8. **Reflection** (`:feature:reflection`) — two fill-in questions the user must write (turn around vs drink again); skippable from Pause (“Not now”); mandatory after call no-answer (affirmation first)

Privacy: **live location only** (current sample + short in-memory ring for still/dwell — never written to DataStore). Backup/cloud extraction disabled. Loved-ones photos copied into app-internal storage.

## Live risk detection (v0.4.0+ background watch)

| Piece | Implementation |
|--------|----------------|
| Location | Play Services `FusedLocationProviderClient` — **always `PRIORITY_HIGH_ACCURACY`** while watching (foreground + background FGS); ~6 s / min 3 s foreground; ~8 s / min 5 s background; ~5 s / min 3 s when near |
| Places | **OpenStreetMap Overpass API** (`overpass-api.de`) — **supermarket/convenience 180 m**, other risks **120 m** — **free, no key** |
| Mapping | `BAR→amenity=bar\|pub\|biergarten`, `LIQUOR_STORE→shop=alcohol\|wine`, `SUPERMARKET→shop=supermarket\|convenience`, `PARTY→amenity=nightclub\|bar` (`HOME_ALONE` / `OTHER` skipped) |
| Still | `STOP_SPEED_MPS = 0.7` when speed is present; else displacement &lt; ~14 m over last ~8 s (in-memory ring only) |
| Dwell | Still **and** nearby continuously for `DWELL_REQUIRED_MS = 5_000` (~5 s); pass-by / leave radius resets the timer |
| Alert UX | Photo alert screen stays on screen (no auto-dialer). Round Call button uses **`ACTION_CALL`** after `CALL_PHONE` (else one `ACTION_DIAL` fallback). Default **ringtone** + call-like vibration until pause/call/dismiss; high-priority notification with `fullScreenIntent`; lock-screen title is anonymous (**Incoming call** / emergency contact name) — not alcohol/risk wording |
| Cooldown | 5 min after dismiss |
| Background | **`RiskWatchService`** foreground service (`foregroundServiceType=location`) hosts `RiskWatchEngine`. Home **ON_PAUSE does not stop** monitoring when background watch is active. Ongoing notification: **“AlcoLarm is active” / “Location on”** (never alcohol / relapse / risk / recovery) |

**No Google Places / Maps API key is required.** AlcoLarm stays free by querying OSM Overpass with User-Agent `AlcoLarm/0.5 (recovery support app)`.

### Permissions the user must grant

1. **Location (precise / while in use)** — `ACCESS_FINE_LOCATION` (and coarse) — required first  
2. **Background location (“Allow all the time”)** — `ACCESS_BACKGROUND_LOCATION` (Android 10+) — needed for alerts when the app is closed; Home shows a clear CTA if missing  
3. **Notifications** — `POST_NOTIFICATIONS` (Android 13+) — ongoing “Location on” notice + call-style alert  
4. App also declares `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` for the watch service  

If only while-in-use location is granted, monitoring stays **foreground-only** (pauses when leaving Home) and status reads: *Background location needed for alerts when app is closed*.

### OSM rate limits (please be polite)

Public Overpass instances ask for responsible use: identify the app (User-Agent), avoid tight polling. Local still/dwell evaluates about every **3 s**; Overpass runs only while the user is **still**, about every **10 s** (plus a ≥8 s client gap). High-accuracy GPS + frequent Overpass use more battery — intentional for aggressive MVP detection.

## Modules

| Module | Role |
|--------|------|
| `:app` | Application, Hilt, NavHost |
| `:core:model` | Domain models + display labels |
| `:core:data` | DataStore prefs (**no** location history) |
| `:core:designsystem` | Nordic calm theme + PauseBanner / chips / buttons / Dial |
| `:feature:onboarding` | Quit reasons (“Loved ones” chip) |
| `:feature:riskplaces` | Risk place chips |
| `:feature:emergency` | Emergency contact + dial |
| `:feature:location` | Home, Fused location, OSM Overpass, `RiskWatchService` |
| `:feature:alert` | Alert screen + call-style controller + dial-return tracker |
| `:feature:reflection` | Pause / no-answer fill-in reflection + call outcome |

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

`applicationId`: `com.alcolarm.app` · `versionName`: `0.5.6-mvp`

## Design notes (v0.5)

- **Everyday:** cool blue-gray surfaces, soft blue primary actions, calm private feel.
- **Pause / alert urgency:** Neo brutal amber banner — large top tappable area (not a tiny control).
- **Dial / call:** Clear Signal amber accent retained for dial and “I reached them”.
- **Launcher:** Logo 8 path-to-light adaptive icon.
