# MobileStudioV2 Handoff

## Current phase

Phase 2 — Screen Capture Permission Flow

## What exists

- Minimal Android app project
- Java-based native Android UI
- `MainActivity`
- `ModeActivity`
- Sender Mode screen-capture permission request
- Sender Mode permission status display
- Studio Mode placeholder
- GitHub Actions workflow for debug APK
- Phase 2 verify script

## Important constraints

Do not add these in Phase 2:

- Screen preview
- Virtual display creation
- Video encoding
- Internal audio capture
- Wireless sender/receiver streaming
- RTMP
- WebRTC
- FFmpeg
- Browser Source
- API keys
- `.env`

## Manual test checklist

1. Install the Phase 2 debug APK.
2. Open MobileStudioV2.
3. Tap Sender Mode.
4. Tap `Request Screen Capture Permission`.
5. Confirm Android shows the screen-capture permission dialog.
6. Tap allow/start now.
7. Confirm app status changes to `Status: Screen capture permission granted`.
8. Repeat once and cancel.
9. Confirm app status changes to `Status: Permission denied or cancelled`.
10. Open Studio Mode and confirm it is still placeholder only.

## Next phase

Phase 3 should create a local capture preview path only.

The next phase should not add RTMP, audio, wireless streaming, or Browser Source yet.
