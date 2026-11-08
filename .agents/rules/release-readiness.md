---
description: Proactively prompts for Google Play Store release preparation after major feature or UI milestones.
always_on: true
---

# Play Store Release Readiness

Whenever a substantial set of changes, feature completions, or visual overhauls reach a stable, polished milestone ("working with a nice version"):

1. **Proactively Prompt the User**:
   Ask the user if they would like to review Play Store release readiness before moving to new tasks:
   *"We've reached a polished milestone with these changes. Would you like to prepare a Google Play Store release update?"*

2. **Pre-Release Checklist to Offer**:
   - **Version Bump**: Check and increment `versionCode` and update `versionName` in `app/build.gradle.kts`.
   - **Target SDK Compatibility**: Verify `targetSdk` aligns with current Google Play requirements (e.g., API 35).
   - **Store Visuals & Screenshots**: Ask if new screenshots or mockups are needed to reflect the updated UI.
   - **Policy & Privacy Disclosures**: Verify that permissions (`AccessibilityService`, `UsageStats`, etc.) and `docs/privacy.html` match the app manifest.
   - **Release Bundle Verification**: Validate that `./gradlew bundleRelease` compiles cleanly into an `.aab`.
