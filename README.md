# HoldUp 🛑🧘

**HoldUp** is a mindful digital wellbeing Android app designed to curb unconscious doomscrolling and compulsive app-opening habits (Instagram, TikTok, X, etc.) through compassionate friction.

Rather than acting as a punitive or adversarial blocker, HoldUp introduces a conscious pause when you tap an addictive app—giving your prefrontal cortex time to catch up before autopilot takes over.

---

## ✨ Features & Interventions

- 🌬️ **Pulsating Breathing Orb**: Smooth animated breathing cycle (Inhale, Hold, Exhale) with synchronized rhythmic haptic vibration feedback.
- ⏳ **Calm Countdown Delay Bar**: A silent, distraction-free pause bar (5–20s) allowing the dopamine surge to settle.
- ❤️ **Loved One's Photo & Note**: Displays a personal photo and heartfelt message reminding you what truly matters.
- 🎬 **Friend's Video Message**: Plays a short personal video clip from a friend (powered by Jetpack Media3 / ExoPlayer).
- 🌱 **Healthy Swaps (Alternative Activities)**: Instant actionable suggestions (*"Drink a glass of water"*, *"Do 10 pushups"*, *"Read 2 pages"*, *"Step outside"*).
- 📊 **Real-time Reality Check**: Context banner showing today's open count and cumulative screen time for that app.
- ⏱️ **Mindful Choice & Timed Sessions**:
  - **Walk Away**: Logs a *Mindful Win* and routes you back to your Home screen.
  - **Open for 5 / 10 / 15 min**: Allows intentional access with a strict time boundary.
- 🔔 **Soft Nudge Overlay**: When your session expires, a gentle non-punitive reminder pops up asking if you're ready to wrap up.
- 📈 **Mindful Dashboard**: Tracks daily Walk-Away Rate (Success %), estimated reclaimed screen time, and mindful wins.

---

## 🛠️ Architecture & Tech Stack

- **Platform**: Native Android (Kotlin 2.0.21, Jetpack Compose, Material 3)
- **Design System**: Custom Zen Dark palette (slate, calming sage `#A3C9A8`, muted lavender `#B8B8D1`)
- **App Detection**: `HoldUpAccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`) for instant zero-lag interception
- **Screen Time Analytics**: `UsageStatsManager` for precise foreground time queries
- **Video & Media**: `androidx.media3:media3-exoplayer` + Coil 3 for image rendering
- **Persistence**: Jetpack DataStore Preferences for local, 100% private on-device storage
- **Session Timers**: `SessionMonitorService` with persistent ongoing notification and soft nudge trigger

---

## 🚀 Installation & Testing

### 1. Build the APK
The debug APK is ready in:
```
app/build/outputs/apk/debug/app-debug.apk
```

To build manually with Gradle:
```powershell
.\gradlew.bat assembleDebug
```

### 2. Install on Device or Emulator via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Setup Permissions
1. Open **HoldUp**.
2. Tap **Enable Interceptor** on the dashboard to enable the **HoldUp App Interceptor** under Android **Accessibility Settings**.
3. Tap **Usage Access** to grant permission for real-time app screen-time statistics.
4. Go to the **Apps** tab to toggle which apps to shield, and customize interventions in the **Interventions** tab.
