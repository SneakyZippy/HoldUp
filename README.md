# HoldUp

An Android app that intercepts distracting apps (Instagram, TikTok, etc.) with a customizable pause screen to prevent mindless doomscrolling.

Instead of hard-blocking apps, HoldUp introduces friction before an app opens so you can decide if you actually want to use it or if you just tapped it out of habit.

## How it works

When you launch a monitored app, HoldUp detects the window change through Android's Accessibility Service and overlays an intervention screen before the target app loads.

You can configure what appears during the pause:
- Breathing exercise with rhythmic haptic feedback
- Silent countdown delay bar (5–20 seconds)
- Photo of a loved one with a personal message
- Short video message from a friend
- Suggested alternative activities (drink water, stretch, read a book)
- Screen time stats showing today's opens and usage time

Once the pause finishes, you have two options:
1. Walk away: takes you straight back to the home screen and logs a mindful win.
2. Open with a limit: grants temporary access for 5, 10, or 15 minutes.

If you choose a timed session, HoldUp runs a background timer and displays a gentle reminder when your time runs out.

## Tech stack

- Kotlin & Jetpack Compose (Material 3 with Material You dynamic colors)
- Android Accessibility Service for instant launch interception and home navigation
- UsageStatsManager for screen time tracking
- Media3 (ExoPlayer) for video message playback
- Coil for image rendering
- Jetpack DataStore Preferences for local storage (no accounts, completely offline)

## Building and Installing

Build the debug APK:
```bash
./gradlew assembleDebug
```

Install to a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Permissions
- Accessibility Service: Used to detect when a monitored app is opened and return to the home screen when you choose to walk away.
- Usage Access: Used to fetch today's screen time and launch frequency for the reality check banner.
