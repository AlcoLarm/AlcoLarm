# AlcoLarm

Android recovery-support app to help avoid alcohol relapse.

**Stack:** Kotlin · Jetpack Compose · modular  
**Design:** Clear Signal — dark, high contrast, stress-ready (amber/teal accent)

## MVP

1. **Onboarding** — quit reasons via chips (health, family, money, work, …); health optional text; family optional text + photos
2. **Risk places** — chips (bar, liquor store, supermarket, …)
3. **Emergency contact** — dial button
4. **Live location only** (no history) — stop at a risk place → alarm + reasons/photos + dial  
   Simulate path until Places is wired

## Modules (planned)

- `:app` — nav + DI
- `:core:model` / `:core:data` — models + DataStore (no location history)
- `:core:designsystem` — Clear Signal theme + shared UI
- `:feature:onboarding`
- `:feature:riskplaces`
- `:feature:emergency`
- `:feature:location` — live + simulate
- `:feature:alert`

Privacy: live location only · minimal permissions · respectful recovery copy.