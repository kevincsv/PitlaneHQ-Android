# Pitlane HQ phone apps: rules for working on this project

- Reply to the owner in Spanish.
- Push only when the owner says so ("súbelo", "sube todo"…). Never create pull requests unless asked.
- Anything sensitive that goes to GitHub is encrypted or kept in GitHub secrets (signing keys,
  passwords); never paste tokens or keys anywhere.
- Real data by default. Invented data only behind Settings → Demo data (off by default, a DEMO
  banner on every screen, only on the phone, never uploaded), for testing the apps.
- This repository is only the Android app (`app/`) and the iOS app (`ios/`). The PC app and the
  web/server (Cloudflare) live in the Pitwall-test repository; the apps talk to that server and
  must keep its protocol: account keys (PBKDF2 600 000 + HKDF, AES-GCM with "pitlanehq-v1"),
  live relay (`/live`, "e:" messages with "pitlanehq-live-v1", gzip JSON [event, data]).
- Android and iOS stay the same app: same screens, same features, same wording.
- `master` is the main branch the builds come from. Running both workflows by hand with `release` ticked
  (or a `v…` tag) builds both apps and attaches the APK, AAB and IPA to the GitHub release (notes in `.github/release-notes.md`).
- Strings: Android `ui/I18n.kt` is the source; `ios/PitlaneHQ/I18n.swift` is generated from it.
