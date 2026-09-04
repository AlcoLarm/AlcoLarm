# AlcoLarm

Android recovery-support app to help avoid alcohol relapse.

**Stack:** Kotlin · Jetpack Compose · Hilt · multi-module Gradle (Kotlin DSL)  
**Design:** Clear Signal — dark base, high contrast, amber accent, stress-ready dial

## MVP flow

1. **Splash** → onboarding or home
2. **Onboarding** — quit-reason chips (health optional text; family optional text + photo picker stub)
3. **Risk places** — chips (bar, liquor store, supermarket, …)
4. **Emergency contact** — name + phone; test dial via `ACTION_DIAL`
5. **Home** — summary + prominent **Simulate risk alert** (until Places is wired)
6. **Alert** — AlarmStrip, reasons/photos placeholders, large Dial button

Privacy: **live location only** (no history APIs). Minimal permissions. Respectful copy. No backend in this scaffold.

## Modules

| Module | Package | Role |
|--------|---------|------|
| `:app` | `com.alcolarm.app` | Application, Hilt, NavHost |
| `:core:model` | `com.alcolarm.core.model` | Domain models |
| `:core:data` | `com.alcolarm.core.data` | DataStore prefs (no location history) |
| `:core:designsystem` | `com.alcolarm.core.designsystem` | Clear Signal theme + chips / buttons / AlarmStrip / Dial |
| `:feature:onboarding` | `com.alcolarm.feature.onboarding` | Quit reasons |
| `:feature:riskplaces` | `com.alcolarm.feature.riskplaces` | Risk place chips |
| `:feature:emergency` | `com.alcolarm.feature.emergency` | Emergency contact + dial |
| `:feature:location` | `com.alcolarm.feature.location` | Home + simulate trigger + live-location stub |
| `:feature:alert` | `com.alcolarm.feature.alert` | Alert screen |

## Open in Android Studio / run

1. Clone this repo and open the **root** folder in Android Studio Ladybug+ (AGP 8.7 / JDK 17).
2. Let Gradle sync (version catalog: `gradle/libs.versions.toml`).
3. Select the `app` run configuration → Run on an emulator or device (API 26+).
4. Walk the flow; on Home tap **Simulate risk alert** to open the alert screen.

```bash
./gradlew :app:assembleDebug
```

**Requirements:** JDK 17, Android SDK compile/target 35, minSdk 26.

## Design notes (Clear Signal)

- Near-black background (`#0B0D10`), amber primary (`#FFB020`), teal support accent
- Large dial on alert; calm pulse on AlarmStrip — not frantic
- Copy stays supportive (“Pause. You’ve got this.”)

## What’s next (not in this PR)

- Wire Fused Location + Places / geofencing (still no history trail)
- Family photo preview on alert
- Notification channel for real risk events
