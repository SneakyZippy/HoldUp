---
name: play-store-assets
description: Complete workflow for Google Play Store metadata, feature graphics, ADB device screenshots, branded mockups, sensitive permission compliance (Accessibility, Foreground Services, Package Visibility), and release verification.
---

# Google Play Store Asset, Compliance & Publishing Pipeline

A comprehensive guide and reference for preparing, verifying, capturing, and generating all required store listing assets, navigating sensitive permission policies, and managing releases for Google Play.

## 1. Metadata Requirements & Validation Guardrails

Always strictly validate character limits and store policies before submission:

* **App Title**: Maximum 30 characters.
  - No promotional phrases (e.g., "Free", "Best", "#1", "Download Now").
  - No emojis, symbols, or all-caps shouting.
* **Short Description**: Maximum 80 characters.
  - Concise single-sentence or two-clause value proposition.
* **Full Description**: Maximum 4,000 characters.
  - Clear section headers, bullet lists, and privacy disclosures.
  - **Mandatory Policy Section**: If the application utilizes sensitive Android permissions such as `AccessibilityService` (`android.permission.BIND_ACCESSIBILITY_SERVICE`) or `UsageStats` (`PACKAGE_USAGE_STATS`), Google Play reviewers require an explicit **Permissions** section clearly stating *why* the permission is necessary and confirming that no private keystrokes or screen contents are harvested.
* **App Icon**: Exactly 512 $\times$ 512 px, PNG or JPEG, max 1MB.

---

## 2. Feature Graphic (Vorstellungsgrafik)

* **Dimensions**: Exactly 1024 $\times$ 500 px.
* **Color Space & Format**: 24-bit RGB PNG or JPEG (no alpha / no transparency).
* **Max File Size**: 15 MB.
* **Composition & Safe Zone**:
  - Keep primary typography, badge pills, and logos centered or within an 80% safe zone (at least 80–100 px from edges) to prevent clipping across various Android device form factors.
  - Use subtle ambient lighting / radial glows matching the app's palette against an OLED/dark canvas (`#090A0D`).

---

## 3. Reliable ADB Screenshot Capture (Windows & Multiplatform)

When capturing live screenshots from a connected Android device on Windows:

1. **Locate ADB**:
   Fallback to `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe` if not present in the user's system `PATH`.
2. **Binary Safety**:
   Never pipe raw binary output in PowerShell (`adb exec-out screencap -p > file.png`), as Windows line-ending conversions can corrupt PNG headers.
3. **Standard 3-Step Pull Pattern**:
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb -s <device_id> shell screencap -p /sdcard/temp_screen.png
   & $adb -s <device_id> pull /sdcard/temp_screen.png <destination_path>
   & $adb -s <device_id> shell rm /sdcard/temp_screen.png
   ```

---

## 4. Store Mockup Framing Guidelines

When preparing screenshots for presentation in the Play Store:

1. **Preserve Originals**: Always keep raw, unedited screenshots in `artwork/screenshots/` (e.g. `screenshot_1.png` to `screenshot_N.png`).
2. **Localized Folders**: Store framed versions in subfolders such as `store_de/` and `store_en/`.
3. **System UI Cropping**:
   - Crop the top ~130–140 px to strip the status bar icons (clock, battery, Wi-Fi) and physical camera hole punch cutouts.
   - Crop the bottom ~40 px to remove the Android system navigation bar handle.
4. **Device Mockup Framing**:
   - Embed the cropped image into a rounded titanium/dark bezel with soft shadow.
   - Add ambient glow corresponding to the screen's primary color theme (e.g., emerald green, lavender, amber).
   - Add a top category pill badge, bold headline, and secondary explanatory tagline.

---

## 5. Sensitive Android Permissions & Policy Compliance

### A. App Inventory & Package Visibility (`QUERY_ALL_PACKAGES` vs `<queries>`)
* **Policy Rule**: Google Play strictly restricts `android.permission.QUERY_ALL_PACKAGES` to device search, antivirus, and enterprise MDM apps. Using it for app blockers, wellbeing tools, or launcher pickers risks rejection.
* **The Clean Solution**: Never request `QUERY_ALL_PACKAGES`. Instead, declare launcher queries in `AndroidManifest.xml`:
  ```xml
  <queries>
      <intent>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
      </intent>
  </queries>
  ```
  This allows querying all user-facing launchable apps on Android 11+ (API 30+) without triggering any Google Play declaration forms or scrutiny.

### B. AccessibilityService API Compliance
If using `AccessibilityService` (`android.permission.BIND_ACCESSIBILITY_SERVICE`):
1. **In-App Prominent Disclosure**: Must be displayed *before* routing the user to Android Settings.
   - Must explicitly name the "AccessibilityService API".
   - Must explain the exact purpose (e.g., detecting foreground apps to show mindful pauses).
   - Must guarantee 100% offline privacy (no keystroke logging, no screen text capture, `canRetrieveWindowContent="false"` in `accessibility_service_config.xml`).
   - Must provide affirmative user choice ("Agree & Continue" vs "Not Now").
2. **Video Requirement**: Google requires an unlisted YouTube video (<3 minutes) showing:
   - The prominent disclosure dialog inside the app.
   - The user tapping "Agree" and toggling the service in Android Settings.
   - An active example of the feature in action (e.g., opening a blocked app and walking away).

### C. Foreground Service (`FOREGROUND_SERVICE_SPECIAL_USE`)
For Android 14+ (API 34+), when using `specialUse`:
* Must include `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" ... />` in `AndroidManifest.xml`.
* **Play Console Justification**:
  - **User-facing task**: Ongoing visible notification in the status bar (e.g., real-time countdown timer for allowed session duration).
  - **Why alternatives (WorkManager/AlarmManager) fail**: Real-time per-second/per-minute notification updates and immediate intervention when time expires require an active Foreground Service.

---

## 6. Anti-Patterns to Avoid

* **Anti-Uninstallation / System Settings Interception**:
  - Never attempt to prevent app uninstallation or intercept `com.android.settings` via accessibility services.
  - Race conditions allow fast users to bypass it anyway, and Google Play's *Device Abuse* & *Unwanted Software* policies explicitly prohibit apps from preventing their own removal.
  - Instead, use transparent features like **Hard Lockout** (PIN-protecting the app itself and immediately closing monitored apps upon launch).

---

## 7. Google Play Console Release Workflow & Warning Guide

### A. Ghost Permission Warnings
* If Play Console still warns about a permission after removing it from code, check the active bundles. Google flags permissions from any bundle that is **"Aktiv"** across any track (Internal, Closed, Production) or draft.
* Deactivate the old bundle by creating a new release with an incremented `versionCode` and removing the old bundle from the draft.

### B. Errors vs. Warnings
* **Errors (Red)**: Block release submission. Must be resolved before publishing.
* **Warnings (Yellow/Gray)**: Do **not** block release submission:
  - *No deobfuscation/mapping file*: Informational; safe to ignore when `isMinifyEnabled = false`.
  - *Native debug symbols*: Triggered by standard media/C++ libraries (e.g., ExoPlayer/Media3); safe to ignore.
