# MobileStudioV2 Handoff

## Current phase

Phase 3 — MediaProjection Capture Session

## What exists

- Minimal Android app project
- Java-based native Android UI
- `MainActivity`
- `ModeActivity`
- `CaptureService`
- Sender Mode screen-capture permission request
- Sender Mode capture session start/stop buttons
- Foreground service with `mediaProjection` service type
- Capture service notification
- Capture session status broadcasts back to Sender Mode
- Studio Mode placeholder
- GitHub Actions workflow for debug APK
- Phase 3 verify script

## Important constraints

Do not add these in Phase 3:

- Screen preview
- Virtual display output
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

1. Install the Phase 3 debug APK.
2. Open MobileStudioV2.
3. Tap Sender Mode.
4. Tap `Request Screen Capture Permission`.
5. Confirm Android shows the screen-capture permission dialog.
6. Tap allow/start now.
7. Confirm app status changes to `Status: Permission granted. Ready to start session`.
8. Tap `Start Capture Session`.
9. Confirm app status changes to `Status: Capture session active`.
10. Confirm Android shows a MobileStudioV2 capture notification.
11. Tap `Stop Capture Session`.
12. Confirm app status changes to stopped/stopping.
13. Confirm `Start Capture Session` becomes disabled until permission is requested again.
14. Open Studio Mode and confirm it is still placeholder only.

## Next phase

Phase 4 should add local preview only.

The next phase should not add RTMP, audio, wireless streaming, or Browser Source yet.
