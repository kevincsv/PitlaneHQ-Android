# Changelog

Pitlane HQ is in **beta**. The phone apps share their version number with PitlaneHQ.exe and the
web (`version.txt`): `0.MINOR.PATCH`, PATCH for fixes, MINOR for a set of new features, 1.0.0
when the beta ends. Every version is listed here, newest first, and gets a release on GitHub.

## 0.8.38 beta

- Same version as the PC and the web (the PC's overlays use the app's fonts and look). No changes in the apps.

## 0.8.37 beta

- Same version as the PC and the web (native overlays on the PC; braking beeps from the model, with an off switch in
  the PC's Settings → Engineer). No changes in the apps.

## 0.8.36 beta

- Same version as the PC and the web (a see-through radar and overlays in your language on the PC). No changes in the apps.

## 0.8.35 beta

- Same version as the PC and the web (the overlay settings preview with sample data). No changes in the apps.

## 0.8.34 beta

- The share code text says Telemetry instead of Live.

## 0.8.33 beta

- The Live tab is called Telemetry, as on the PC and the web.

## 0.8.32 beta

- Same version as the PC and the web (the estimated iRating at the start of a race). No changes in the apps.

## 0.8.31 beta

- Same version as the PC and the web (smoother overlays, floating settings window on the PC). No changes in the apps.

## 0.8.30 beta

- Same version as the PC and the web (the PC's overlays). No changes in the apps.

## 0.8.29 beta

- Same version as the PC and the web (the PC's radar and standings overlays). No changes in the apps.

## 0.8.28 beta

- Same version as the PC and the web (the standings overlay on the PC). No changes in the apps.

## 0.8.27 beta

- Same version as the PC and the web (how moving an overlay looks on the PC). No changes in the apps.

## 0.8.26 beta

- Same version as the PC and the web (fixes in the PC's overlays). No changes in the apps.

## 0.8.25 beta

- Same version as the PC and the web (fixes in the PC's lap analyzer and Settings). No changes in the apps.

## 0.8.24 beta

- Live: after the PC declined this phone, pressing Connect again asks the PC again.

## 0.8.23 beta

- Same version as the PC and the web (the admin email panel). No changes in the apps.

## 0.8.22 beta

- Same version as the PC and the web (the logo in the inbox for Pitlane HQ's emails). No changes in the apps.

## 0.8.21 beta

- Same version as the PC and the web (public record pages on the web). No changes in the apps.

## 0.8.20 beta

**Changed**
- A new app icon: the Pitlane HQ rev gauge with a kerb in the rev limit, the same logo as the PC and the web.

## 0.8.19 beta

- Same version as the PC and the web (emails through Resend). No changes in the apps.

## 0.8.18 beta

- Same version as the PC and the web (emails over Proton, the domain check for admins). No changes in
  the apps.

## 0.8.17 beta

**Changed**
- Analysis: the filters are four small menus (discipline, kind of session, car, track) and the days you
  drove open from a button, instead of long rows of chips.
- A track or car whose name came with broken accents ("AutÃ³dromo") is shown right and only once.
- Signing in with an email that has no account says so: create it with "Create an account".

## 0.8.16 beta

**New**
- **The car card** in the lap analysis: what this car does on other tracks (its hardest braking, the
  speeds the fast drivers shift up at, its top speed and where), learnt by the server from its real laps
  everywhere, against what this lap did. It helps when nobody known has driven this car on this track yet.

**Changed**
- Same version as the PC and the web (lap B with "+ Session" shows only the other session, the model's
  references once, the title of "Choose a session" stays at the top).

## 0.8.15 beta

**New**
- Analysis: the car and the track are two filters.
- Admin: Activity (the latest sessions) and Blocked (unblock one by one) tabs; for each account,
  confirm its email, turn off its two-step sign-in, sign it out everywhere, rename it, unblock it.

**Fixed**
- Admin: "unblock the sign-ins" works again with the server of 0.8.15.

## 0.8.14 beta

**New**
- **Analysis filters like the web's "Choose a session":** the kind of session (races, qualifying,
  practice, test drives) and the days you drove (press a day on the calendar), next to the discipline
  and the car and track.
- **Connect asks your PC:** Live waits until you accept this phone in the window on the PC, says when
  it was declined, and lets you try again if the PC did not answer.
- **Admins:** the admin profile opens on an overview (activity, leaderboards, leagues, supporters and
  Patreon, the coach models) with tools to rebuild the coach models and unblock sign-ins; each account
  shows its id, its "in Pitlane HQ since" date (change it) and its profile. "See it as a normal user"
  in Settings hides everything for admins and in development, to test what everyone sees.

**Changed**
- The Live screen no longer talks about encryption.

## 0.8.13 beta

- Same version as the PC and the web (they no longer stay on the loading screen for whoever had
  chosen several race reminders). No changes in the apps.

## 0.8.12 beta

**New**
- **Live: Connect, Disconnect and codes.** Live shows whether your PC is online and connects when
  you press Connect (until Disconnect). While you watch your PC you can get a code for others; and
  with someone's code you watch their telemetry (Watch another driver). The PC app's download is
  right under it.
- **Leagues** for admins (in development): explore by discipline, post yours with its Discord
  invite, edit or remove it. Everyone else still reads that we are working on it.
- **Profiles:** each lap opens its leaderboard and says the driver's place; the days they drove.
- **Your badges** (Admin, Supporter) next to your name on Home and in Account; the supporter badge
  comes by itself with Patreon (same email). Your email stays hidden until you press Show.

**Changed**
- Leaderboards without license classes: the fastest drivers of each car and track of the discipline.
- Same version as the PC and the web.

## 0.8.11 beta

**New**
- **Leaderboards by discipline and license**: Community filters by Oval, Sports Car, Formula Car,
  Dirt Oval and Dirt Road; each leaderboard shows the license class of every driver and filters by
  class. When your lap is the fastest, it says you are the fastest.
- **Driver profiles**: tap a driver on a leaderboard to see their nickname (never the iRacing name),
  license classes, recent races and laps. Yours is in Account → My profile. Anonymous laps open no
  profile. Your recent races are summarised for your profile after each sync (only your own result).
- **Supporter badge** next to the drivers who donate; admins give it and take it away in the admin
  profile, and a supporter hides it from their profile.
- **Leagues** tab in Community, in development: admins see what is coming, everyone else reads that
  we are working on it.
- **Analysis filters your sessions** by license (discipline) and then car and track; a session shows
  the car, the discipline and your license class.

**Changed**
- Same version as the PC and the web (fastest lap shared by itself, test drives out of the model and
  the leaderboards, incidents completed in older race summaries).

## 0.8.10 beta

**Fixed**
- **The apps sync by themselves**: when they open and every time they come back to the screen
  (at most every 30 seconds, without the spinner), so a public name changed on another device,
  new races and laps show without pressing Sync. Same version as the PC and the web (automatic
  sync everywhere with merged changes, one public name per account, automatic PC updates; deleting
  an account keeps its leaderboard laps and what the model learnt, anonymous).

## 0.8.9 beta

**Changed**
- Same version as the PC and the web (a shorter coach and lap analyzer; the model has no panel of
  its own, the coach shows the record, the next level and your pace among the drivers).



## 0.8.8 beta

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
