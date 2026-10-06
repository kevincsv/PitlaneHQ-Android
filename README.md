# Pitlane HQ for Android and iOS 0.1.0

The phone apps of Pitlane HQ. They connect to the same Pitlane HQ server and the same account as
the PC app (PitlaneHQ.exe) and the web app (`/app`), which live in their own repository.

| Folder | App | Built with |
| --- | --- | --- |
| `app/` | Android | Kotlin, Jetpack Compose (no WebView) |
| `ios/` | iPhone and iPad | Swift, SwiftUI (no WebView), project generated with XcodeGen |

Both apps do the same:

- **Sign in** with the Pitlane HQ account. The password only derives the keys on the phone
  (PBKDF2-SHA256, 600 000 rounds, then HKDF): the server gets the login key and returns the
  data key sealed with AES-256-GCM. The session token and the data key are kept in the
  Android Keystore or the iOS Keychain.
- **Home:** account and the PC settings synced into the account (opened on the phone, never
  readable by the server).
- **Analysis (My laps):** the sessions and laps PitlaneHQ.exe uploaded, with the best lap and the
  best sectors in purple.
- **Community:** every track and car with shared iRacing laps, and the fastest drivers.
- **Live:** live telemetry from PitlaneHQ.exe through the account's live room on the server,
  from any network. Messages are sealed with the account's data key; the server only passes
  them along, and the PC only streams while the Live screen is open.
- **Profile:** sign out, and the full Pitlane HQ on the web for everything else.

Only real data: nothing is invented in the apps.

## Server

`https://pitlanehq.app`

| Use | Endpoint |
| --- | --- |
| Sign in / out, account | `POST /account/login`, `POST /account/logout`, `GET /account/me` |
| Synced PC settings | `GET /account/sync` |
| Your sessions and laps | `GET /api/sessions`, `GET /api/sessions/<id>` |
| Community | `GET /community/combos?game=iracing`, `GET /community/laps?game=iracing&trackId=&carId=` |
| Live telemetry | WebSocket `/live?role=view` |

## Builds

GitHub Actions (`master`, or by hand from the Actions tab):

- **Android CI:** debug APK, release APK (unsigned) and release AAB.
- **iOS CI:** unsigned `.ipa` (macOS, Xcode). To install it on an iPhone it has to be signed with
  an Apple developer account (for example with AltStore or Sideloadly, or later with signing
  secrets in GitHub).

Build iOS locally on a Mac: `brew install xcodegen`, then `cd ios && xcodegen generate` and open
`PitlaneHQ.xcodeproj`.
