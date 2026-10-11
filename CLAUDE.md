# Pitlane HQ phone apps: rules for working on this project

- Reply to the owner in Spanish.
- Push only when the owner says so ("súbelo", "sube todo"…). Never create pull requests unless asked.
- Anything sensitive that goes to GitHub is encrypted or kept in GitHub secrets (signing keys,
  passwords); never paste tokens or keys anywhere.
- Real data by default. Invented data only behind Settings → Demo data, which only the server's
  admins see (off by default, a DEMO banner on every screen, only on the phone, never uploaded).
- DRINKS mode (formerly Friday night mode, admins only): friends drive on the owner's PC and their laps
  go to the community under their name. Switched on the PC or from the phone (Settings → DRINKS
  mode) through the encrypted live link ("drinks" event); the PC checks the account is an admin.
- A race against iRacing's AI (`ai` on the race) has no iRating: no change in the race rows or the summary, no
  iRating in its results, nothing in the licence summary. A session the server has no `ai` flag for is against the AI when it
  is an iRacing session without a subsession (id "t-…") that is not a test (`offlineAI` when the sessions are parsed),
  so My sessions, Laps and the Coach show "vs AI" on it like the PC and the web.
- The admin panel's account card has "Link a DRINKS driver" (the driver's name; `adminAccount(id, "link-guest", name)`)
  and "Unlink DRINKS driver …" for each one joined (`guestLinked` in the accounts list, "gid|name,…").
- This repository is only the Android app (`app/`) and the iOS app (`ios/`). The PC app and the
  web/server (Cloudflare) live in the Pitwall-test repository; the apps talk to that server and
  must keep its protocol: account keys (PBKDF2 600 000 + HKDF, AES-GCM with "pitlanehq-v1"),
  live relay (`/live`, "e:" messages with "pitlanehq-live-v1", gzip JSON [event, data]).
- Android and iOS stay the same app: same screens, same features, same wording, and the PC's: Telemetry has the PC's
  Telemetry, Standings and Tyres (`Live` in PitlaneApp.kt, `LiveTabs.swift`, fed by `LiveField`); Analysis has Laps,
  Coach, My sessions and My races (`Analysis` in PitlaneApp.kt, `AnalysisView` and `Analysis.swift`), lap B is your own
  best, the model's next level or its record (`modelRefs`), and each driver is measured against their own laps.
- One version for Pitlane HQ everywhere: `version.txt` (the same number as PitlaneHQ.exe and the
  web), shown with "beta" until 1.0.0. Raise it slowly (PATCH for fixes, MINOR for a set of
  features) and add the version to CHANGELOG.md: its section becomes the release notes.
- `master` is the main branch the builds come from. Running both workflows by hand with `release` ticked
  (or a `v…` tag) builds both apps and attaches the APK, AAB and IPA to the GitHub release (notes in `.github/release-notes.md`).
- Strings: Android `ui/I18n.kt` is the source; `ios/PitlaneHQ/I18n.swift` is generated from it.
