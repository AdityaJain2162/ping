# Ping — Reminders, Alarms & Location Alerts

> Reminders that find you. Alarms that wake you. Privacy-first, no account, no cloud.

Ping is a native Android app built in **Kotlin + Jetpack Compose + Material 3**
for location-based reminders, time-based reminders, and full-screen alarms. It
was built to fill the gap left when Google removed location-based reminders
from Keep in H2 2025 — and expands beyond location into a complete reminder +
alarm hub.

## Current features (v0.1)

- **Location-based reminders** — pin a place, write a note, get notified on
  arrival or departure.
- **Material 3 theming** with four modes: System, Light, Dark, and **AMOLED**
  (true black for OLED battery saving).
- **Dynamic Color** on Android 12+ (auto-disabled for AMOLED to keep true black).
- **Privacy-first** — all data is local (Room DB). No account, no cloud sync,
  no tracking.
- **Ad-supported** (Play Store flavor) — a single banner ad on the home screen
  (never mid-task). Community flavor is ad-free.
- **Offline-first** — reminders fire without an internet connection.

## Roadmap

See [AGENTS.md](AGENTS.md) section 2 for the full feature roadmap. Highlights:

- **Time-based reminders** + AlarmManager (P0)
- **Full-screen alarms** with escalating volume, snooze, anti-sleep dismiss (P0)
- **Reboot recovery** — reminders + alarms survive reboot (P0)
- **Recurring reminders + alarms** (P0)
- **Nag mode** — persistent notifications until done (P1)
- **Saved places** — Home, Work, Gym instant pick (P1)
- **Calendar view** — month/week/day (P2)
- **Smart lists** — Due Today, Nearby, Overdue (P3)
- **Natural language input** — on-device parsing (P3)
- **Wear OS** support (P5)

## Tech stack

| Layer | Tech |
|-------|------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation-Compose |
| Persistence | Room (KSP) |
| Location | Google Play Services FusedLocationProvider |
| Alarms | AlarmManager + AlarmReceiver (coming) |
| Ads | Google Mobile Ads (AdMob) — playstore flavor only |
| Theme persistence | DataStore Preferences |
| Background | Foreground service + boot receiver |

## Product flavors

| Flavor | App ID | Ads | Purpose |
|--------|--------|-----|---------|
| `community` | `com.aditya.ping.community` | No | Ad-free, sideloadable |
| `playstore` | `com.aditya.ping` | Yes (AdMob) | Play Store release |

## Build

```bash
# Requires Android SDK Platform 34 + Build-Tools 34.0.0 + Google Play services
./gradlew assembleCommunityDebug    # ad-free APK
./gradlew assemblePlaystoreDebug     # ad-supported APK
./gradlew assembleDebug              # both flavors
./gradlew installCommunityDebug      # install ad-free on device
```

## Project structure

See [AGENTS.md](AGENTS.md) for the full architecture playbook.

```
app/src/main/java/com/aditya/ping/
├── ui/theme/        Material 3 + AMOLED color tokens, PingTheme
├── ui/screens/      Home, AddEdit, Settings + ViewModels
├── ui/components/   ReminderCard, BannerAd (flavor-specific)
├── ui/navigation/   PingNavHost routes
├── data/            Room entity, DAO, DB, repository, ThemeRepository
├── domain/          ThemeMode enum
├── service/         GeofenceService (foreground), BootReceiver
└── util/            LocationUtil, PermissionUtil, NotificationChannels

app/src/community/java/com/aditya/ping/   No-op ad stubs
app/src/playstore/java/com/aditya/ping/  Real AdMob (AdConfig, BannerAd, AdInitializer)
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
| `SCHEDULE_EXACT_ALARM` | Exact alarm scheduling (Android 12+) — coming |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule reminders after reboot |
| `VIBRATE` | Alarm vibration — coming |
| `WAKE_LOCK` | Keep CPU awake during alarm — coming |
| `INTERNET` / `ACCESS_NETWORK_STATE` | AdMob banner ads (playstore only) |

## License

Source is project-private. Material Icons are Apache 2.0. Google Play
Services and AdMob SDKs are governed by their respective terms.
