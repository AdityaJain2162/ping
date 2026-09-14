# AGENTS.md — GeoNote: Location Reminders — Technical Playbook for AI Agents

> **Read this file before touching any code in this repository.**
> It is the single source of truth for architecture, theme system, ad
> integration, and the operational rules every future agent MUST follow.

---

## 1. Project Overview

**GeoNote** is a privacy-first Android app for location-based reminders. Users
pin a place on the map (or use their current location), write a note, and get a
notification when they arrive at (or leave) that place.

Built natively in **Kotlin + Jetpack Compose + Material 3**, with an optional
**AMOLED dark theme** for OLED screens.

### Package

`com.aditya.geonote` — `com.aditya` is the reusable brand prefix for all apps
by Aditya Jain. The app name is appended (e.g. `com.aditya.geonote`,
`com.aditya.nextapp`).

### Product Flavors

GeoNote ships in two flavors via the `distribution` flavor dimension:

| Flavor | App ID | Ads | Purpose |
|--------|--------|-----|---------|
| `community` | `com.aditya.geonote.community` | No | Ad-free, sideloadable, open build |
| `playstore` | `com.aditya.geonote` | Yes (AdMob) | Play Store release with banner ads |

- Ad code (`BannerAd`, `AdConfig`, `AdInitializer`) lives in **flavor source
  sets**, not `main`. The community flavor provides no-op stubs; the playstore
  flavor provides real AdMob implementations.
- `play-services-ads` dependency is `playstoreImplementation` only — the
  community APK contains zero ad SDK code.
- AdMob meta-data + `INTERNET`/`ACCESS_NETWORK_STATE` permissions are in
  `src/playstore/AndroidManifest.xml` only.

### Why this app exists
Google removed location-based reminders from Keep in H2 2025. Existing
alternatives (Tasks.org, TickTick, Any.do, Local Reminder) are either bloated,
subscription-gated, or noisy. GeoNote is the calm, minimal, ad-supported
alternative — no account required, offline-first, no tracking.

---

## 2. System Architecture

```
app/src/main/
├── java/com/adityajain/geonote/
│   ├── GeoNoteApplication.kt        ← Application class (AdMob init, theme bootstrap)
│   ├── MainActivity.kt              ← Single-activity host (Compose + theme provider)
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt             ← Light/Dark/AMOLED color tokens
│   │   │   ├── Theme.kt             ← GeoNoteTheme composable (dynamic + AMOLED switch)
│   │   │   ├── Type.kt              ← Material 3 type scale
│   │   │   └── Shape.kt             ← Corner radius tokens
│   │   ├── navigation/
│   │   │   └── NavGraph.kt           ← NavHost routes (home, add, edit, settings)
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt        ← List of reminders (sorted by proximity)
│   │   │   ├── AddEditScreen.kt     ← Create/edit a reminder + pick location
│   │   │   └── SettingsScreen.kt    ← Theme switch, AMOLED toggle, about
│   │   └── components/
│   │       ├── ReminderCard.kt      ← List item card
│   │       └── BannerAd.kt          ← AdMob banner wrapper
│   ├── data/
│   │   ├── ReminderEntity.kt        ← Room entity
│   │   ├── ReminderDao.kt           ← Room DAO (Flow-based queries)
│   │   ├── GeoNoteDatabase.kt       ← Room database (singleton)
│   │   └── ReminderRepository.kt   ← Single source of truth for CRUD
│   ├── domain/
│   │   └── ThemeMode.kt             ← enum: LIGHT / DARK / AMOLED / SYSTEM
│   ├── service/
│   │   └── GeofenceService.kt       ← Foreground service: location monitoring
│   └── util/
│       ├── LocationUtil.kt          ← FusedLocationProvider wrapper
│       └── PermissionUtil.kt        ← Compose permission state helpers
├── res/
│   ├── values/                      ← strings, colors, themes (Material 3 XML)
│   ├── values-night/                ← dark-mode XML overrides
│   ├── drawable/                    ← launcher icon foreground/background
│   ├── mipmap-anydpi-v26/           ← adaptive icon XML
│   └── xml/                         ← backup rules, data extraction rules
└── AndroidManifest.xml             ← permissions, AdMob app ID, services
```

### State layers

| Layer | Where | Purpose |
|-------|-------|---------|
| **UI** | Compose screens | Stateless composables driven by ViewModel state |
| **ViewModel** | `androidx.lifecycle.ViewModel` | Holds `StateFlow<UiState>`, calls repository |
| **Repository** | `ReminderRepository` | Single CRUD entry point; wraps DAO |
| **Persistence** | Room DB (`geonote.db`) | Reminders table, offline-first |
| **Location** | `GeofenceService` (foreground) | Fused location client, geofence triggers |
| **Theme** | `DataStore<Preferences>` | Persisted theme mode (light/dark/AMOLED/system) |
| **Ads** | `BannerAd` composable | AdMob banner, test IDs in debug |

---

## 3. Theme System (Material 3 + AMOLED)

### Theme modes (`ThemeMode` enum)

| Mode | Behavior |
|------|----------|
| `SYSTEM` | Follows system dark/light setting (default) |
| `LIGHT`  | Always light |
| `DARK`   | Always dark (Material 3 dark tokens) |
| `AMOLED` | Pure-black surfaces (#000000) for OLED battery saving |

### Implementation rules

- `GeoNoteTheme` composable reads `ThemeMode` from a `CompositionLocal` (set by
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

## 4. AdMob Integration

### Test ad unit IDs (Google official — safe for development)

> ⚠️ **NEVER ship with these in production.** Replace before Play Store release.

| Format | Test Ad Unit ID |
|--------|-----------------|
| App ID (manifest) | `ca-app-pub-3940256099942544~3347511713` (Android) |
| Banner | `ca-app-pub-3940256099942544/6300978111` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |
| Rewarded | `ca-app-pub-3940256099942544/5224354917` |

### Initialization

- `MobileAds.initialize()` is called once in `GeoNoteApplication.onCreate()`.
- `AdView` is created inside the `BannerAd` composable using `AndroidView`.
- In `debug` builds, `RequestConfiguration` is set to
  `TestDeviceIds` so all requests return test ads.
- Banner ads are shown only on `HomeScreen` (bottom of list). Never on
  `AddEditScreen` (user is mid-task) or `SettingsScreen`.

### Production checklist

1. Replace manifest `android:value` with your real AdMob app ID.
2. Replace banner ad unit ID in `BannerAd.kt`.
3. Remove `TestDeviceIds` debug config (or keep your device IDs for QA).
4. Add `<meta-data android:name="com.google.android.gms.ads.flag.HAS_GOOGLE_BANNER"` if needed.

---

## 5. Operational Guidelines for Future Agents

### STRICT commit rule

> **NEVER** append `Co-authored-by:`, `Authored-by:`, `Generated with`, or any
> bot signature trailer to any git commit. Every commit must be authored
> **solely** by **Aditya Jain** (`jaditya700@gmail.com`).

Git identity is pre-configured in `.git/config`. Do not change it.

### Build-test-commit workflow

> **ALWAYS** build → test → commit for each feature. Do not batch multiple
> features into a single commit. Run `./gradlew assembleDebug` before every
> commit. If the build fails, fix it before committing.

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

1. Add the test ad unit ID to `BannerAd.kt` (or a new wrapper).
2. Initialize in `GeoNoteApplication` if it needs preload.
3. Show only on screens approved in this playbook.
4. Document the test ID here in section 4.
5. Build, test, commit.

---

## 6. Command Cheat Sheet

```bash
# Build community debug APK (ad-free)
./gradlew assembleCommunityDebug

# Build playstore debug APK (with ads)
./gradlew assemblePlaystoreDebug

# Build both flavors
./gradlew assembleDebug

# Install on connected device/emulator (specify flavor)
./gradlew installCommunityDebug
./gradlew installPlaystoreDebug

# Run unit tests
./gradlew test

# Run instrumented tests (needs emulator/device)
./gradlew connectedAndroidTests

# Build release AAB (Play Store — playstore flavor only)
./gradlew bundlePlaystoreRelease

# Lint check
./gradlew lintCommunityDebug

# Clean build
./gradlew clean
```

---

## 7. Permissions

| Permission | Why | When requested |
|------------|-----|----------------|
| `ACCESS_FINE_LOCATION` | Precise geofence triggers | Runtime, on first add |
| `ACCESS_COARSE_LOCATION` | Fallback if fine denied | Runtime, on first add |
| `ACCESS_BACKGROUND_LOCATION` | Triggers while app closed | Runtime, after fine granted |
| `POST_NOTIFICATIONS` | Show arrival/departure alerts | Runtime, on first launch (API 33+) |
| `FOREGROUND_SERVICE` | `GeofenceService` keeps monitoring alive | Manifest |
| `FOREGROUND_SERVICE_LOCATION` | Type for API 34+ | Manifest |
| `INTERNET` | AdMob banner ads | Manifest |
| `ACCESS_NETWORK_STATE` | AdMob ad loading | Manifest |

---

## 8. Known Limitations

1. **No cloud sync** — all data is local. Adding sync is a future feature.
2. **No account** — by design. Privacy-first.
3. **Geofence limit** — Android allows 100 geofences per app. We cap at 50
   active reminders to stay safe.
4. **AMOLED disables Dynamic Color** — by design; Dynamic Color can't
   produce true black.
5. **Ads only on HomeScreen** — never interrupt the user mid-task.

---

## 9. Asset Credits

| Asset | Source | License |
|-------|--------|---------|
| Material Icons | `androidx.compose.material:material-icons-extended` | Apache 2.0 |
| App icon | Generated adaptive icon (vector) | CC0 / project |
| Google Play Services | `com.google.android.gms:play-services-*` | Apache 2.0 |
| AdMob | `com.google.android.gms:play-services-ads` | Google ToS |
