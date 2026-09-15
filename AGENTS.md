# AGENTS.md — Ping — Technical Playbook for AI Agents

> **Read this file before touching any code in this repository.**
> It is the single source of truth for architecture, theme system, ad
> integration, feature roadmap, and the operational rules every future agent
> MUST follow.

---

## 1. Project Overview

**Ping** is a privacy-first Android app for **reminders, alarms, and
location-based alerts**. Users set time reminders, location geofence triggers,
and full-screen alarms — all offline, no account, no cloud, no tracking.

Built natively in **Kotlin + Jetpack Compose + Material 3**, with an optional
**AMOLED dark theme** for OLED screens.

### Package

`com.aditya.ping` — `com.aditya` is the reusable brand prefix for all apps
by Aditya Jain. The app name is appended (e.g. `com.aditya.ping`,
`com.aditya.nextapp`).

### Product Flavors

Ping ships in a single flavor via the `distribution` flavor dimension:

| Flavor | App ID | Ads | Purpose |
|--------|--------|-----|---------|
| `playstore` | `com.aditya.ping` | Yes (AdMob) | Play Store release with banner ads |

- Ad code (`BannerAd`, `AdConfig`, `AdInitializer`) lives in **`main` source
  set** — there is only one build flavor now.
- `play-services-ads` dependency is a regular `implementation` — every build
  includes the AdMob SDK.
- AdMob meta-data + `INTERNET`/`ACCESS_NETWORK_STATE` permissions are in
  `src/main/AndroidManifest.xml`.

### Why this app exists
Google removed location-based reminders from Keep in H2 2025. Existing
alternatives (Tasks.org, TickTick, Any.do, Local Reminder) are either bloated,
subscription-gated, or noisy. Ping is the calm, minimal, ad-supported
alternative — no account required, offline-first, no tracking. It expands
beyond location into time reminders, alarms, nag mode, and smart scheduling.

---

## 2. Feature Roadmap

### Current (v0.1 — shipped)
- Location-based reminders (arrive/leave geofence)
- Material 3 + AMOLED theme (system/light/dark/AMOLED)
- Room persistence, offline-first
- Play Store flavor with AdMob banner ads
- GitHub Actions CI/CD

### Phase 1 — Core Reminder & Alarm Power

#### 1.1 Time-based reminders (P0)
- `dueAt: Long?` field on `ReminderEntity`
- `AlarmManager` scheduling for exact-time triggers
- A reminder can have both a time AND a location trigger
- Time + location: "Remind me at 5 PM OR when I arrive at the store"

#### 1.2 Alarms (P0)
- Full-screen ringing `Activity` when alarm fires
- `AlarmManager.setExactAndAllowWhileIdle` for Doze-proof firing
- Escalating volume (starts soft, ramps over 30s)
- Snooze (5/10/15 min, configurable per alarm)
- Anti-sleep dismiss (optional: solve math / long-press 3s)
- Gradual wake (vibration starts 1 min before sound)
- Alarm labels ("Gym", "Flight", "Medication")
- Repeat days (Mon-Fri, weekends, custom)
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` permission flow (Android 12+)
- Upcoming alarm persistent notification ("Next alarm: 7:00 AM")

#### 1.3 Reboot recovery (P0)
- `BootReceiver` re-schedules ALL time reminders + alarms after reboot
- Re-registers geofences after reboot
- Verify no reminders/alarms are lost

#### 1.4 Recurring reminders + alarms (P0)
- Recurrence rules: daily, weekly, weekdays, weekends, monthly, yearly
- Custom intervals ("every 2 weeks", "every 15 minutes")
- End date or repeat count
- When marked done, next occurrence auto-schedules

### Phase 2 — Context & Intelligence

#### 2.1 Nag mode (P1)
- Per-reminder toggle: "Keep nagging until done"
- Configurable nag interval (5/15/30/60 min)
- Escalation: after N missed alerts, increase urgency
- Auto-repost if notification swiped away without marking done

#### 2.2 Saved places (P1)
- Named locations: Home, Work, Gym, Grocery Store
- Reuse across reminders
- Custom aliases + custom radius per saved place

#### 2.3 Smart snooze (P1)
- Human presets: "Soon" (15 min), "Later today" (3 hr), "This evening" (6 PM),
  "Tomorrow morning" (9 AM)
- Custom snooze picker
- Snooze chains for recurring reminders

#### 2.4 Quiet hours (P2)
- Global quiet hours (e.g., 10 PM – 7 AM)
- Reminders during quiet hours queue and fire when quiet hours end
- Per-reminder override ("high priority ignores quiet hours")

#### 2.5 Combined triggers (P2)
- Time + location: "Fire at 5 PM OR when I arrive, whichever first"
- Time window: "Only fire location reminder between 9 AM – 9 PM"
- AND logic: "Only when I'm at the gym AND it's Monday"

### Phase 3 — Calendar & Organization

#### 3.1 Calendar view (P2)
- Month/week/day views showing time-based reminders + alarms
- Location reminders shown as pins on a map view
- Tap a day → see all reminders for that day

#### 3.2 Lists & categories (P2)
- Create named lists ("Groceries", "Work", "Errands")
- Assign reminders to lists
- Filter home screen by list
- Color tags per list

#### 3.3 Smart lists (P3)
- "Due Today" — all time reminders firing today
- "Nearby" — all location reminders within 1 km
- "Overdue" — anything past due and not done
- "No location" — time-only reminders

#### 3.4 Search (P3)
- Full-text search across reminder titles + notes
- Filter by list, date range, location
- Search history

### Phase 4 — Reliability & Polish

#### 4.1 Doze mode & battery exemptions (P4)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for high-priority reminders
- `setExactAndAllowWhileIdle` for critical reminders
- Inexact alarms for low-priority to save battery

#### 4.2 Notification actions (P1)
- "Done" / "Snooze" / "Defer to tomorrow" buttons in the notification
- Mark done from notification → schedules next recurrence

#### 4.3 Widgets (P4)
- Home screen widget: "Due Today" list
- Quick-add widget (tap → type → save)
- Map widget showing nearby location reminders

### Phase 5 — Advanced (future)

#### 5.1 Natural language input (P3)
- On-device regex/NLP parser (no cloud)
- Parse: time ("at 5 PM"), date ("tomorrow"), recurrence ("every week"),
  location ("at the store")
- Pre-fill reminder form from parsed text

#### 5.2 On-device AI scheduling (P5)
- Suggest best time based on existing schedule
- Batch low-priority reminders into one notification
- Adaptive timing: learn when you complete tasks

#### 5.3 Import/export (P5)
- JSON export/import (backup/restore, no cloud)
- CSV export
- Import from Google Tasks (for Keep exodus migrants)

#### 5.4 Voice integration (P5)
- `Intent.ACTION_SEND` so voice assistants create reminders in Ping
- "Hey Google, remind me to buy milk with Ping"

#### 5.5 Wear OS (P5)
- Wear OS tile: "Due Today"
- Quick-complete from watch
- Vibration-only nudges

---

## 3. System Architecture

```
app/src/main/
├── java/com/aditya/ping/
│   ├── PingApplication.kt          ← Application class (AdMob init, theme bootstrap)
│   ├── MainActivity.kt             ← Single-activity host (Compose + theme provider)
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt            ← Light/Dark/AMOLED color tokens
│   │   │   ├── Theme.kt            ← PingTheme composable (dynamic + AMOLED switch)
│   │   │   ├── Type.kt             ← Material 3 type scale
│   │   │   └── Shape.kt           ← Corner radius tokens
│   │   ├── navigation/
│   │   │   └── NavGraph.kt         ← NavHost routes (home, add, edit, settings, alarm)
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt       ← List of reminders (sorted by proximity)
│   │   │   ├── AddEditScreen.kt    ← Create/edit a reminder + pick location
│   │   │   ├── SettingsScreen.kt   ← Theme switch, AMOLED toggle, about
│   │   │   └── AlarmScreen.kt      ← (future) Alarm ringing full-screen UI
│   │   └── components/
│   │       ├── ReminderCard.kt     ← List item card
│   │       └── BannerAd.kt         ← AdMob banner wrapper (flavor-specific)
│   ├── data/
│   │   ├── ReminderEntity.kt      ← Room entity (title, note, lat/lng, dueAt, recurrence)
│   │   ├── ReminderDao.kt         ← Room DAO (Flow-based queries)
│   │   ├── PingDatabase.kt        ← Room database (singleton)
│   │   ├── ReminderRepository.kt  ← Single source of truth for CRUD
│   │   └── ThemeRepository.kt     ← DataStore theme persistence
│   ├── domain/
│   │   ├── ThemeMode.kt           ← enum: LIGHT / DARK / AMOLED / SYSTEM
│   │   └── TriggerType.kt         ← enum: TIME / LOCATION / BOTH (future)
│   ├── service/
│   │   ├── GeofenceService.kt     ← Foreground service: location monitoring
│   │   ├── AlarmReceiver.kt       ← (future) AlarmManager broadcast receiver
│   │   └── BootReceiver.kt        ← Re-schedule reminders + alarms after reboot
│   └── util/
│       ├── LocationUtil.kt        ← FusedLocationProvider wrapper
│       ├── PermissionUtil.kt      ← Permission state helpers
│       ├── AdConfig.kt            ← (flavor) AdMob test ad unit IDs
│       └── NotificationChannels.kt ← Channel IDs (geofence, service, alarm)
├── res/
│   ├── values/                    ← strings, colors, themes (Material 3 XML)
│   ├── values-night/              ← dark-mode XML overrides
│   ├── drawable/                  ← launcher icon foreground/background
│   ├── mipmap-anydpi-v26/         ← adaptive icon XML
│   └── xml/                       ← backup rules, data extraction rules
└── AndroidManifest.xml           ← permissions, AdMob app ID, services
```

### State layers

| Layer | Where | Purpose |
|-------|-------|---------|
| **UI** | Compose screens | Stateless composables driven by ViewModel state |
| **ViewModel** | `androidx.lifecycle.ViewModel` | Holds `StateFlow<UiState>`, calls repository |
| **Repository** | `ReminderRepository` | Single CRUD entry point; wraps DAO |
| **Persistence** | Room DB (`ping.db`) | Reminders table, offline-first |
| **Location** | `GeofenceService` (foreground) | Fused location client, geofence triggers |
| **Time** | `AlarmManager` + `AlarmReceiver` | Exact-time triggers for reminders + alarms |
| **Theme** | `DataStore<Preferences>` | Persisted theme mode (light/dark/AMOLED/system) |
| **Ads** | `BannerAd` composable | AdMob banner, test IDs in debug (playstore only) |

---

## 4. Theme System (Material 3 + AMOLED)

### Theme modes (`ThemeMode` enum)

| Mode | Behavior |
|------|----------|
| `SYSTEM` | Follows system dark/light setting (default) |
| `LIGHT`  | Always light |
| `DARK`   | Always dark (Material 3 dark tokens) |
| `AMOLED` | Pure-black surfaces (#000000) for OLED battery saving |

### Implementation rules

- `PingTheme` composable reads `ThemeMode` from a `CompositionLocal` (set by
  `MainActivity` from `DataStore`).
- When `AMOLED` is active, override `colorScheme.surface*` and
  `colorScheme.background` to `Color.Black`. All other tokens stay Material 3
  dark.
- Dynamic Color (Android 12+) is supported in `SYSTEM`, `LIGHT`, `DARK` but
  **disabled in `AMOLED`** (Dynamic Color can't produce true black).
- Theme choice persists across launches via `DataStore` key `theme_mode`.

### AMOLED color overrides

```
surface         = #000000
surfaceVariant  = #0A0A0A
background      = #000000
surfaceContainer = #111111
```

---

## 5. AdMob Integration

### Test ad unit IDs (Google official — safe for development)

> ⚠️ **NEVER ship with these in production.** Replace before Play Store release.

| Format | Test Ad Unit ID |
|--------|-----------------|
| App ID (manifest) | `ca-app-pub-3940256099942544~3347511713` (Android) |
| Banner | `ca-app-pub-3940256099942544/6300978111` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |
| Rewarded | `ca-app-pub-3940256099942544/5224354917` |

### Initialization

- `MobileAds.initialize()` is called once in `PingApplication.onCreate()`
  via `AdInitializer.init()` (flavor-swappable).
- `AdView` is created inside the `BannerAd` composable using `AndroidView`.
- In `debug` builds, `RequestConfiguration` is set to
  `TestDeviceIds` so all requests return test ads.
- Banner ads are shown on **every screen**: `HomeScreen`, `CalendarScreen`,
  `SavedPlacesScreen`, `AutomationsScreen`, `HistoryScreen`, `ListsScreen`,
  `SettingsScreen`, and `AddEditScreen`. Ads always appear at the bottom of
  scrollable content so they never cover or push content. The only screen
  without ads is `AlarmActivity` (full-screen ringing alarm).

### Production checklist

1. Replace manifest `android:value` with your real AdMob app ID.
2. Replace banner ad unit ID in `BannerAd.kt`.
3. Remove `TestDeviceIds` debug config (or keep your device IDs for QA).

---

## 6. Operational Guidelines for Future Agents

### STRICT commit rule

> **NEVER** append `Co-authored-by:`, `Authored-by:`, `Generated with`, or any
> bot signature trailer to any git commit. Every commit must be authored
> **solely** by **Aditya Jain** (`jaditya700@gmail.com`).

Git identity is pre-configured in `.git/config`. Do not change it.

### Build-test-commit workflow

> **ALWAYS** build → test → commit for each feature. Do not batch multiple
> features into a single commit. Run `./gradlew assemblePlaystoreDebug` before
> every commit. If the build fails, fix it before committing.

### Code style

- Kotlin, Jetpack Compose, Material 3.
- `camelCase` for functions/variables, `PascalCase` for classes/composables,
  `UPPER_SNAKE` for constants.
- Do NOT add or remove comments unless asked.
- Use `StateFlow` / `Flow` for reactive data; never block the main thread.
- Keep `service/` and `data/` free of Compose imports.

### Adding a new screen

1. Add the route to `NavGraph.kt`.
2. Create the screen composable in `ui/screens/`.
3. Wire a ViewModel if state is needed.
4. Add a string resource (never hardcode user-facing strings).
5. Build, test, commit.

### Adding a new ad format

1. Add the test ad unit ID to `AdConfig.kt` (playstore flavor).
2. Initialize in `PingApplication` via `AdInitializer` if it needs preload.
3. Show only on screens approved in this playbook.
4. Document the test ID here in section 5.
5. Build, test, commit.

---

## 7. Command Cheat Sheet

```bash
# Build playstore debug APK (with ads)
./gradlew assemblePlaystoreDebug

# Install on connected device/emulator
./gradlew installPlaystoreDebug

# Run unit tests
./gradlew test

# Run instrumented tests (needs emulator/device)
./gradlew connectedAndroidTests

# Build release AAB (Play Store)
./gradlew bundlePlaystoreRelease

# Lint check
./gradlew lintPlaystoreDebug

# Clean build
./gradlew clean
```

---

## 8. Permissions

| Permission | Why | When requested |
|------------|-----|----------------|
| `ACCESS_FINE_LOCATION` | Precise geofence triggers | Runtime, on first add |
| `ACCESS_COARSE_LOCATION` | Fallback if fine denied | Runtime, on first add |
| `ACCESS_BACKGROUND_LOCATION` | Triggers while app closed | Runtime, after fine granted |
| `POST_NOTIFICATIONS` | Show arrival/departure alerts | Runtime, on first launch (API 33+) |
| `FOREGROUND_SERVICE` | `GeofenceService` keeps monitoring alive | Manifest |
| `FOREGROUND_SERVICE_LOCATION` | Type for API 34+ | Manifest |
| `SCHEDULE_EXACT_ALARM` | Exact alarm scheduling (Android 12+) | Runtime (API 31-32) |
| `USE_EXACT_ALARM` | Exact alarm scheduling (Android 13+) | Manifest (API 33+) |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule after reboot | Manifest |
| `VIBRATE` | Alarm vibration | Manifest |
| `WAKE_LOCK` | Keep CPU awake during alarm | Manifest |
| `INTERNET` | AdMob banner ads | Manifest (playstore only) |
| `ACCESS_NETWORK_STATE` | AdMob ad loading | Manifest (playstore only) |

---

## 9. Known Limitations

1. **No cloud sync** — all data is local. Adding sync is a future feature.
2. **No account** — by design. Privacy-first.
3. **Geofence limit** — Android allows 100 geofences per app. We cap at 50
   active reminders to stay safe.
4. **AMOLED disables Dynamic Color** — by design; Dynamic Color can't
   produce true black.
5. **Ads only on HomeScreen** — never interrupt the user mid-task.
6. **Alarm exact-accuracy** — Android 12+ restricts exact alarms; users may
   need to grant `SCHEDULE_EXACT_ALARM` manually.

---

## 10. Asset Credits

| Asset | Source | License |
|-------|--------|---------|
| Material Icons | `androidx.compose.material:material-icons-extended` | Apache 2.0 |
| App icon | Generated adaptive icon (vector) | CC0 / project |
| Google Play Services | `com.google.android.gms:play-services-*` | Apache 2.0 |
| AdMob | `com.google.android.gms:play-services-ads` | Google ToS |
| Alarm sounds | (future) Synthesized CC0 WAVs | CC0 1.0 (Public Domain) |
