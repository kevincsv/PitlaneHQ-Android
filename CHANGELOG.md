# Changelog

Pitlane HQ is in **beta**. The phone apps share their version number with PitlaneHQ.exe and the
web (`version.txt`): `0.MINOR.PATCH`, PATCH for fixes, MINOR for a set of new features, 1.0.0
when the beta ends. Every version is listed here, newest first, and gets a release on GitHub.

## 0.8.9 beta

**Changed**
- Same version as the PC and the web (a shorter coach and lap analyzer; the model has no panel of
  its own, the coach shows the record, the next level and your pace among the drivers).



**Changed**
- Same version as the PC and the web (coach presentation fixes). In the apps, the session screen
  shows the average of the valid laps instead of a lap made of the best sectors (one model, from
  real laps, no theoretical lap anywhere), and the coach note no longer speaks of an "ideal lap".

## 0.8.7 beta

**Changed**
- Same version as the PC and the web (the community learns from everyone you race against, races
  without a report are rebuilt from the saved laps, the real iRating change replaces the estimate,
  the Garage 61 import is gone). In the apps, the leaderboard shows those anonymous rivals like
  any other driver.

## 0.8.6 beta

**Changed**
- Same version as the PC and the web (the top 3 of each race go to the leaderboard anonymously
  from the PC, the Garage 61 import works with the real Garage 61 and shares your best laps).
  Nothing changes in the phone apps.

## 0.8.5 beta

**Fixed**
- **Your own laps and analyses are marked "you" in Community**, also the anonymous ones (only you
  see that mark; the server tells the app which are yours), and "Anonymous" is translated. Same
  version as the PC and the web (iRating estimate the right way round, Garage 61 import working,
  incidents in the race summary's table).

## 0.8.4 beta

**Changed**
- Same version as the PC and the web (bigger menu, compact Analysis, DRINKS card, sharing on
  unknown tracks, Garage 61 import for admins on the web). Nothing changes in the phone apps.

## 0.8.3 beta

**Fixed**
- **Live telemetry works again** (the fix is on the server: since 0.6.1 it broke the live connection).
  Same version as the PC and the web.

## 0.8.2 beta

**Changed**
- Same version as the PC and the web (a realistic coach model again, a compact Analysis on the
  PC and the web). Nothing changes in the phone apps.

## 0.8.0 beta

**Added**
- **Admin profile:** delete an account, and a Server tab with the state of the emails, the last
  email error and the numbers of the platform.
- **Confirm your email first:** a clear message when an account still has to open its link.

**Changed**
- Same version as the PC and the web.

## 0.7.2 beta

**Changed**
- Same version as the PC and the web: the coach model learns older laps too and never forgets.
  Nothing changes in the phone apps.

## 0.7.1 beta

**Changed**
- Same version as the PC and the web: the coach model now learns on the server from every
  valid lap as soon as it arrives. Nothing changes in the phone apps.

## 0.7.0 beta

**Added**
- **Admin profile** in Settings (admins only): the accounts, and every shared lap and race
  analysis with the name it shows and who really uploaded it, with a delete button.
- **Unique nicknames** on the platform, with a clear message when one is taken.

## 0.6.2 beta

**Changed**
- **The app is called Pitlane HQ again**, and it talks to the server at https://pitlanehq.app
  (same account, same laps).
- The version check and the "What's new" notes come from pitlanehq.app (downloads page and
  changelog), not from GitHub.
- **Info** in the header of every screen instead of the floating bell: the alerts (new version,
  app news, the two-step sign-in recommendation; dismissable) and, always, support and feedback.
- The track map shows the three sectors: the start/finish line, a cut at each sector change and
  S1, S2, S3 in the middle of each sector, like the PC and the web.

## 0.6.1 beta

**New**
- **Two-step sign-in** (optional, recommended): turn it on in Settings with an authenticator
  app; signing in then asks for the 6-digit code (or a recovery code).
- **Inbox**: a floating bell with what matters now: a new version with its notes, news of the
  app, Patreon, feedback and the two-step sign-in recommendation. Dismiss what you have read.
- **New version notice at start** with a **What's new** button that opens that version's
  changelog, besides the banner.

## 0.6.0 beta

**New**
- **Days you drove** on Home: the last 26 weeks in squares, brighter the more races and sessions
  that day, like the PC and the web. Tap a day to see its race summaries and sessions.

**Changed**
- Not valid laps are grey and crossed out in the lap lists ("invalid" in words, no ✂), like the
  PC and the web.

## 0.5.9 beta

Same version as PitlaneHQ.exe and the web, which now open on Home like the phone apps and run a
live demo race. No changes in the phone apps.

## 0.5.8 beta

**Changed**
- The incident near the point you touch on the map reads "Car contact (4x)": the points in
  brackets, like the PC and the web.

## 0.5.7 beta

**Fixed**
- Races kept on the phone lost their incident events and the "not valid" mark of their laps; the
  race's incident total is now worked out before (the largest of the report's total, the laps'
  and the events'), and the laps keep the mark.
- Names saved with the wrong encoding by an older PC ("AutÃ³dromo", "LÃ©o") read right.
- The lap list and the map readout use words instead of the ⚠ symbol ("1x", "Car contact 4x").

## 0.5.6 beta

**Changed**
- Light contact and loss of control are told apart (both 2x in iRacing): the map readout and the
  incident summary name them separately, from what PitlaneHQ.exe 0.5.6 records.

## 0.5.5 beta

Same version as PitlaneHQ.exe and the web, which now need your account like the phone apps already
did, and where the session bar on phones was fixed. No changes in the phone apps.

## 0.5.4 beta

**New**
- The track map in the lap analysis shows the same as the web, the PC and the race summary: where
  you brake (orange) and where the reference brakes (blue), the incidents with their points
  (1x, 2x, 4x) as the game gives them, the coach's corners (where you lose time), and switches
  for Braking, Incidents and Coach. One line sums up the incidents of the lap.

## 0.5.3 beta

Same version as PitlaneHQ.exe and the web (fixes on the web app). No changes in the phone apps.

## 0.5.2 beta

**Changed**
- The incidents near the point you touch on the map are named as the game does: Off track 1x,
  Loss of control or slight contact 2x, Car contact 4x. Same version as PitlaneHQ.exe and the web.

## 0.5.1 beta

**New**
- Incidents on your laps: the lap list shows ⚠ with the points of each lap, and the track map in the
  lap analysis marks where they happened (a switch hides them). Touch the map near one to read it.
  Incidents never make a lap invalid; only leaving the track does.

## 0.5.0 beta

**New**
- The apps are now **Pitlane HQ**, like the PC app and the web: the name on the phone, in every screen
  and in the release notes. Your account and settings stay where they were.
- **Track map** in the lap analysis: the circuit drawn from the lap itself (PitlaneHQ.exe 0.5 records
  where the car was on every lap), coloured where you gain (green) or lose (red) time against the
  reference, with the start line and the sector marks. Touch or drag on the map to read that point:
  the speed, the reference and the gap, and the charts follow it. Demo data draws it too.

**Changed**
- Laps through the pit lane are no longer crossed out as "not valid": only leaving the track
  (cutting) makes a lap invalid, the same as in the race summary on the PC and the web.

## 0.4.0 beta (not built yet)

Same version as PitlaneHQ.exe and the web, where the Coach, the lap analyzer charts, the race map
and the phone Home were reworked. No changes in the phone apps.

## 0.3.9 beta

**Changed**
- Community shows the leaderboards only: sharing race analyses is switched off for now.

## 0.3.8 beta

Same version as PitlaneHQ.exe and the web, where race reports now use the same check for laps
off the track as the lap analyzer and sharing. No changes in the phone apps.

## 0.3.7 beta

**Changed**
- Sessions say practice, qualifying or race in your language.
- Race laps where you left the track say ✂ (not valid) and never count as your best lap.

Also in this version, on the PC and the web: sharing older sessions and laps without telemetry,
the lap analyzer charts and My races on phones.

## 0.3.6 beta

**Changed**
- Community leaderboard: laps whose telemetry was not shared say so.
- Setups are switched off for now.

## 0.3.5 beta

Same version as PitlaneHQ.exe and the web. This release lets the PC analyse the sessions in your
account, fixes the corners on race maps and anonymous race analyses, and fills in the telemetry
of shared laps on the leaderboard. No changes in the phone apps.

## 0.3.4 beta

Same version as PitlaneHQ.exe and the web. This release fixes race analyses (incident counts,
names with accents, anonymous sharing) and improves their map, corners, incidents and coach on
the PC and the web. No changes in the phone apps.

## 0.3.3 beta

**Changed**
- Lap charts answer at once: a tap shows the values and sliding sideways follows the finger,
  without pressing and holding first. Scrolling up and down still works over the charts.
- One card with everything at that point sits above the speed chart, with its place kept before
  you touch, so the charts do not move under your finger.

## 0.3.2 beta

**New**
- Lap charts: touching a chart shows everything at that point, like hovering on the web and the
  PC: distance, sector, the gap there, speed, throttle, brake and gear of both laps, and the
  sector times. The three charts follow the same point.

Also in this version, on PitlaneHQ.exe and the web: lap charts fixed on scaled Windows screens,
the demo data switch, and DRINKS mode in Settings on the PC and the web.

## 0.3.1 beta

**New**
- Lap analysis coach in four phases, like the web's: for each corner, which phase loses the time
  (braking, entry, apex or exit) and what to change (brake point and pressure, trail braking,
  coasting, minimum speed, throttle). A summary shows the time lost per phase over the lap and
  the metres spent coasting.

**Changed**
- More compact screens: smaller titles, panels and bottom bar.



The first numbered beta, on the same version as the PC app and the web.

**New**
- Home: licences per category (Sports Car, Formula, Oval, Dirt Road, Dirt Oval), in development
  until iRacing switches its data API back on; the summary boxes in even rows; how your data
  reaches your account through the PC agent.
- Lap charts you can read by touch: tap, or touch and hold and drag.
- Admins: demo data for testing (only on the phone, never uploaded) and DRINKS mode (switch the
  friend driving on your PC; their laps go to the community under their name).
- Analysis, Community and Live say that the full coach and the full live telemetry are in the
  web and PC app, with a button to open the web.
- The version shows "beta".
- A banner tells you when a newer version is out, with the download.
- Settings: support Pitlane HQ on Patreon.

**Fixed**
- Android: the status bar icons are always visible.
- Lap charts: lighter (at most 400 points per line) and safe with incomplete laps.

## 0.2.1 and 0.2.0

Home with your recent races and iRating, lap analysis with your best lap or the community's
fastest, community leaderboards, race reports and setups, Settings (account, devices, language),
offline mode with the data saved on the phone, and live telemetry from the PC through your
account.

## 0.1.0

First Android build: sign in with the Pitlane HQ account and sync.
