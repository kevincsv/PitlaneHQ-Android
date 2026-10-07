# TrackIQ phone apps: rules for working on this project

- Reply to the owner in Spanish.
- Push only when the owner says so ("súbelo", "sube todo"…). Never create pull requests unless asked.
- Anything sensitive that goes to GitHub is encrypted or kept in GitHub secrets (signing keys,
  passwords); never paste tokens or keys anywhere.
- Real data by default. Invented data only behind Settings → Demo data, which only the server's
  admins see (off by default, a DEMO banner on every screen, only on the phone, never uploaded).
- DRINKS mode (formerly Friday night mode, admins only): friends drive on the owner's PC and their laps
  go to the community under their name. Switched on the PC or from the phone (Settings → DRINKS
  mode) through the encrypted live link ("drinks" event); the PC checks the account is an admin.
- This repository is only the Android app (`app/`) and the iOS app (`ios/`). The PC app and the
  web/server (Cloudflare) live in the Pitwall-test repository; the apps talk to that server and
  must keep its protocol: account keys (PBKDF2 600 000 + HKDF, AES-GCM with "pitlanehq-v1"),
  live relay (`/live`, "e:" messages with "pitlanehq-live-v1", gzip JSON [event, data]).
- Android and iOS stay the same app: same screens, same features, same wording.
- One version for TrackIQ everywhere: `version.txt` (the same number as TrackIQ.exe and the
  web), shown with "beta" until 1.0.0. Raise it slowly (PATCH for fixes, MINOR for a set of
  features) and add the version to CHANGELOG.md: its section becomes the release notes.
- `master` is the main branch the builds come from. Running both workflows by hand with `release` ticked
  (or a `v…` tag) builds both apps and attaches the APK, AAB and IPA to the GitHub release (notes in `.github/release-notes.md`).
- Strings: Android `ui/I18n.kt` is the source; `ios/PitlaneHQ/I18n.swift` is generated from it.
