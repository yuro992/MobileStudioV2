# MobileStudioV2 Handoff

## Current phase

Phase 4 — Local Screen Preview

## What exists

- Minimal Android app project
- Java-based native Android UI
- `MainActivity`
- `ModeActivity`
- `MediaProjectionKeepAliveService`
- Sender Mode screen-capture permission flow
- Foreground media projection service
- Local preview through `SurfaceView`
- `VirtualDisplay` created from `MediaProjection`
- Stop button releases `VirtualDisplay` and `MediaProjection`
- Studio Mode placeholder
- Phase 4 verify script
- GitHub Actions workflow for debug APK

## Important constraints

Do not add these in Phase 4:

- Video encoding
- RTMP
- WebRTC
- FFmpeg
- Internal audio capture
- Real wireless sender/receiver
- API keys
- `.env`

## Known UX note

Because the preview captures the device screen and displays it inside the same app, it can show a mirror-recursion effect. That is expected for Phase 4.

## Next phase

Phase 5 should stabilize preview behavior across orientation changes, app lifecycle changes, and repeated start/stop cycles.

The next phase should still avoid RTMP, audio, and wireless transport.
