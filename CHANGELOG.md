# Changelog

TrackIQ (formerly Pitlane HQ) is in **beta**. The phone apps share their version number with TrackIQ.exe and the
web (`version.txt`): `0.MINOR.PATCH`, PATCH for fixes, MINOR for a set of new features, 1.0.0
when the beta ends. Every version is listed here, newest first, and gets a release on GitHub.

## 0.5.6 beta

**Changed**
- Light contact and loss of control are told apart (both 2x in iRacing): the map readout and the
  incident summary name them separately, from what TrackIQ.exe 0.5.6 records.

## 0.5.5 beta

Same version as TrackIQ.exe and the web, which now need your account like the phone apps already
did, and where the session bar on phones was fixed. No changes in the phone apps.

## 0.5.4 beta

**New**
- The track map in the lap analysis shows the same as the web, the PC and the race summary: where
  you brake (orange) and where the reference brakes (blue), the incidents with their points
  (1x, 2x, 4x) as the game gives them, the coach's corners (where you lose time), and switches
  for Braking, Incidents and Coach. One line sums up the incidents of the lap.

## 0.5.3 beta

Same version as TrackIQ.exe and the web (fixes on the web app). No changes in the phone apps.

## 0.5.2 beta

**Changed**
- The incidents near the point you touch on the map are named as the game does: Off track 1x,
  Loss of control or slight contact 2x, Car contact 4x. Same version as TrackIQ.exe and the web.

## 0.5.1 beta

**New**
- Incidents on your laps: the lap list shows ⚠ with the points of each lap, and the track map in the
  lap analysis marks where they happened (a switch hides them). Touch the map near one to read it.
  Incidents never make a lap invalid; only leaving the track does.

## 0.5.0 beta

**New**
- The apps are now **TrackIQ**, like the PC app and the web: the name on the phone, in every screen
  and in the release notes. Your account and settings stay where they were.
- **Track map** in the lap analysis: the circuit drawn from the lap itself (TrackIQ.exe 0.5 records
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
