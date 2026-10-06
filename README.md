# Pitlane HQ Android

Native Android companion app for Pitlane HQ / PitWall.

## Architecture
- Kotlin + Jetpack Compose
- MVVM with repository boundary
- OkHttp WebSocket for live PitWall telemetry
- Navigation Compose
- Designed to consume stable PitWall PC/API contracts rather than iRacing directly

## Current scope
Dashboard, Sessions, My Laps, Full Telemetry, Connection Center and Settings navigation, plus a live WebSocket client.

## Important
The Android app does not connect directly to iRacing shared memory. The PitWall PC remains the telemetry gateway.

The WebSocket endpoint is currently treated as /api/ws; this adapter is isolated in PitWallRepository so the UI architecture can remain stable while the final PC stream contract is hardened.

## Roadmap
1. Harden PC discovery and pairing
2. Stable telemetry protocol/schema
3. Session/lap API integration
4. Offline cache
5. iRacing OAuth
6. Authentication and cloud sync
7. Full lap analysis
8. Engineer/AI features
9. Background service and Android notifications
10. Release signing and Play Store bundle
