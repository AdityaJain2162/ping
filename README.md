# GeoNote — Location Reminders

> Reminders that find you. Privacy-first, ad-supported, no account, no cloud.

GeoNote is a native Android app built in **Kotlin + Jetpack Compose + Material 3**
that fires a notification when you arrive at (or leave) a saved place. It was
built to fill the gap left when Google removed location-based reminders from
Keep in H2 2025.

## Features

- **Location-based reminders** — pin a place, write a note, get notified on
  arrival or departure.
- **Material 3 theming** with four modes: System, Light, Dark, and **AMOLED**
  (true black for OLED battery saving).
- **Dynamic Color** on Android 12+ (auto-disabled for AMOLED to keep true black).
- **Privacy-first** — all data is local (Room DB). No account, no cloud sync,
  no tracking.
- **Ad-supported** — a single banner ad on the home screen (never mid-task).
- **Offline-first** — reminders fire without an internet connection.

## Tech stack

| Layer | Tech |
|-------|------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation-Compose |
| Persistence | Room (KSP) |
| Location | Google Play Services FusedLocationProvider |
| Ads | Google Mobile Ads (AdMob) |
| Theme persistence | DataStore Preferences |
| Background | Foreground service + boot receiver |

## Build

```bash
# Requires Android SDK Platform 34 + Build-Tools 34.0.0 + Google Play services
./gradlew assembleDebug
./gradlew installDebug
```

## Project structure

See [AGENTS.md](AGENTS.md) for the full architecture playbook.

```
app/src/main/java/com/adityajain/geonote/
├── ui/theme/        Material 3 + AMOLED color tokens, GeoNoteTheme
├── ui/screens/      Home, AddEdit, Settings + ViewModels
├── ui/components/   ReminderCard, BannerAd
├── ui/navigation/   NavHost routes
├── data/            Room entity, DAO, DB, repository, ThemeRepository
├── domain/          ThemeMode enum
├── service/         GeofenceService (foreground), BootReceiver
└── util/            LocationUtil, PermissionUtil, AdConfig, NotificationChannels
```

## AdMob — test vs production

This project ships with **Google's official test ad unit IDs** (safe for
development, generate no real revenue):

| Format | Test ID |
|--------|---------|
| App ID (manifest) | `ca-app-pub-3940256099942544~3347511713` |
| Banner | `ca-app-pub-3940256099942544/6300978111` |

**Before publishing to the Play Store:**
1. Replace the manifest `APPLICATION_ID` with your real AdMob app ID.
2. Replace `AdConfig.BANNER_AD_UNIT_ID` with your real banner ad unit ID.
3. Register your physical test device in `AdConfig` / `RequestConfiguration`
   so you keep getting test ads during QA.

## Permissions

| Permission | Why |
|------------|-----|
| `ACCESS_FINE_LOCATION` | Precise geofence triggers |
| `ACCESS_BACKGROUND_LOCATION` | Triggers while app is closed |
| `POST_NOTIFICATIONS` | Show arrival/departure alerts (API 33+) |
| `FOREGROUND_SERVICE_LOCATION` | Keep monitoring alive in background |
| `INTERNET` / `ACCESS_NETWORK_STATE` | AdMob banner ads |

## License

Source is project-private. Material Icons are Apache 2.0. Google Play
Services and AdMob SDKs are governed by their respective terms.
