# Pitlane HQ phone apps: rules for working on this project

- Reply to the owner in Spanish.
- Push only when the owner says so ("súbelo", "sube todo"…). Never create pull requests unless asked.
- Anything sensitive that goes to GitHub is encrypted or kept in GitHub secrets (signing keys,
  passwords); never paste tokens or keys anywhere.
- Only real data: no demo races, sample drivers or made-up laps.
- This repository is only the Android app (`app/`) and the iOS app (`ios/`). The PC app and the
  web/server (Cloudflare) live in the Pitwall-test repository; the apps talk to that server and
  must keep its protocol: account keys (PBKDF2 600 000 + HKDF, AES-GCM with "pitlanehq-v1"),
  live relay (`/live`, "e:" messages with "pitlanehq-live-v1", gzip JSON [event, data]).
- Android and iOS stay the same app: same screens, same features, same wording.
- `master` is the main branch the builds come from.
