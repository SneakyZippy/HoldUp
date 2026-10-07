# HoldUp

An Android app that intercepts doomscroll apps (Instagram, TikTok, etc.) with a mindful pause instead of a hard block.

## How it works

When you open a monitored app, HoldUp pauses you to break autopilot habits:

- **Mindful Reflections**: Curated grounding prompts and reality checks (with hide/downvote and custom quotes).
- **Breathing Exercise**: Rhythmic breath cycle with gentle haptics.
- **Personal Anchor**: Photo of a loved one or video message from a friend.
- **Healthy Swaps**: Quick ideas of rewarding things to do instead.

Choose to **Walk Away** (logs a mindful win and takes you home) or open for a set time (**5, 10, or 15 min**). When your time expires, a soft nudge brings you back to reality.

## Download & Build

Download the latest APK directly from [Releases](https://github.com/SneakyZippy/HoldUp/releases).

Build locally:
```bash
./gradlew assembleDebug
```

## Permissions
- **Accessibility Service**: Detects app launches and handles navigation when you walk away.
- **Usage Access**: Displays today's screen time in the reality check banner.
