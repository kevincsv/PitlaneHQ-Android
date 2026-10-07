# TrackIQ for Android and iOS 0.5.5

The phone apps of TrackIQ (formerly Pitlane HQ). They connect to the same TrackIQ server and the same account as
the PC app (TrackIQ.exe) and the web app (`/app`), which live in their own repository.

| Folder | App | Built with |
| --- | --- | --- |
| `app/` | Android | Kotlin, Jetpack Compose (no WebView) |
| `ios/` | iPhone and iPad | Swift, SwiftUI (no WebView), project generated with XcodeGen |

Both apps do the same:

- **Sign in** with the TrackIQ account. The password only derives the keys on the phone
  (PBKDF2-SHA256, 600 000 rounds, then HKDF): the server gets the login key and returns the
  data key sealed with AES-256-GCM. The session token and the data key are kept in the
  Android Keystore or the iOS Keychain.
- **Home:** recent races from the account sync (races.json written by TrackIQ.exe): finish,
  iRating change, incidents, SOF, a racing summary and each race with laps and results.
  Safety rating is in development until iRacing switches its data API back on.
- **Analysis:** sessions and personal bests; a lap compared with your best lap or the community's
  fastest (speed, delta and input charts, where the time goes, sector deltas).
- **Community:** leaderboards with your position, race reports and setups.
- **Live:** live telemetry from TrackIQ.exe through the account's live room on the server,
  from any network. Messages are sealed with the account's data key; the server only passes
  them along, and the PC only streams while the Live screen is open.
- **Settings:** account, devices signed in, language (phone, English, Spanish), web. Admins also
  get demo data and DRINKS mode (switch the friend driving on the PC; their laps go to the
  community under their name).
- **Offline:** a banner, the last data saved on the phone (encrypted) and retry.

Real data by default. Settings → Demo data shows invented data to test the apps: only on the
phone, never uploaded, with a DEMO banner on every screen.


## Server

`https://pitlanehq.app`

| Use | Endpoint |
| --- | --- |
| Sign in / out, account | `POST /account/login`, `POST /account/logout`, `GET /account/me` |
| Synced PC settings | `GET /account/sync` |
| Your sessions, laps and bests | `GET /api/sessions`, `GET /api/sessions/<id>`, `GET /api/laps/<id>`, `GET /api/bests` |
| Devices | `GET /account/sessions`, `POST /account/sessions/revoke` |
| Community | `GET /community/combos`, `/community/laps`, `/community/laps/<id>`, `/community/reports`, `/community/setups` |
| Live telemetry | WebSocket `/live?role=view` |

## Builds and releases

To publish a release: Actions → Android CI and iOS CI → Run workflow with **release** ticked (or push a
`v0.2.0` tag). Each build attaches its files to the GitHub release `v<version>`.

Android release key (optional, for updates that install over each other): add the GitHub secrets
`ANDROID_KEYSTORE_BASE64` (the .jks file in base64), `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`. Without them the APK is signed with the CI
debug key.

## CI

GitHub Actions (`master`, or by hand from the Actions tab):

- **Android CI:** debug APK, release APK (unsigned) and release AAB.
- **iOS CI:** unsigned `.ipa` (macOS, Xcode). To install it on an iPhone it has to be signed with
  an Apple developer account (for example with AltStore or Sideloadly, or later with signing
  secrets in GitHub).

Build iOS locally on a Mac: `brew install xcodegen`, then `cd ios && xcodegen generate` and open
`PitlaneHQ.xcodeproj`.
