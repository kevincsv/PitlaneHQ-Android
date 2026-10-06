# Pitlane HQ Android 0.1.0

Native Android companion app for Pitlane HQ / PitWall.

## 0.1.0 scope
- Native Jetpack Compose UI; no WebView.
- Pitlane HQ account sign-in using the same account as the PC app.
- Secure Android Keystore storage for the session token and local data key.
- Pulls the end-to-end encrypted PC sync bundle from the Pitlane HQ account.
- Home, Analysis, Community and Profile companion areas.
- Optional live telemetry from the PitWall PC over the local network.
- Architecture prepared for future iRacing OAuth.
- No marketplace, prices or purchases.

## Architecture
The Android app does not connect directly to iRacing shared memory. The PitWall PC remains the telemetry gateway.

Account flow:

Pitlane HQ account -> PC sync -> Pitlane HQ cloud -> Android companion

Live telemetry flow:

PitWall PC -> /api/stream -> Android

The account sync payload remains encrypted end-to-end. The server stores the encrypted bundle and the Android app decrypts it only after deriving the account key locally from the user's password.

## Build
GitHub Actions builds:
- debug APK
- release APK (app-release-unsigned.apk)
- release Android App Bundle (app-release.aab)

Version: 0.1.0.
