# MobileStudioV2 Handoff

## Current phase

Phase 5 — Sender Preview Cleanup + Capture Metrics

## What exists

- Minimal Android app project
- Java-based native Android UI
- `MainActivity`
- `ModeActivity`
- `MediaProjectionKeepAliveService`
- Sender Mode screen-capture permission flow
- Sender Mode foreground capture session
- Sender Mode local preview surface
- Sender Mode capture metrics panel
- Keep-screen-on while preview is active
- Safer Back/onDestroy cleanup path
- GitHub Actions workflow for debug APK
- Phase 1 through Phase 5 verify scripts

## Important constraints

Do not add these in Phase 5:

- RTMP
- WebRTC
- FFmpeg
- MediaCodec video encoding
- AudioPlaybackCapture
- Real wireless sender/receiver
- API keys
- `.env`

## Manual test checklist

1. Open MobileStudioV2.
2. Open Sender Mode.
3. Confirm Phase 5 label is visible.
4. Confirm preview area and capture metrics panel are visible.
5. Request screen-capture permission.
6. Start Stable Preview.
7. Confirm mirrored screen preview appears.
8. Confirm metrics show screen size, preview surface size, orientation, uptime, keep-screen-on state, and FPS placeholder.
9. Stop Stable Preview.
10. Confirm preview stops and permission must be requested again.
11. Start again, then press Back and confirm the app does not crash.

## Next phase

Phase 6 should prepare the sender frame pipeline only.

The next phase should still avoid RTMP, audio, and real two-phone wireless transport unless explicitly approved.
