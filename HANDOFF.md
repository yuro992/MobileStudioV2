# MobileStudioV2 Handoff

## Current phase

Phase 6 — Sender H.264 Encoder Dry Run

## What exists

- Minimal Android app project
- Java-based native Android UI
- `MainActivity`
- `ModeActivity`
- `MediaProjectionKeepAliveService`
- Sender Mode screen-capture permission flow
- Sender Mode foreground capture session
- Sender Mode local H.264 encoder dry run
- MediaProjection feeding a MediaCodec input surface
- Encoder drain thread that counts encoded output buffers
- Encoder metrics panel
- Keep-screen-on while encoder is active
- Safer Back/onDestroy cleanup path
- GitHub Actions workflow for debug APK
- Phase 1 through Phase 6 verify scripts

## Important constraints

Do not add these in Phase 6:

- RTMP
- WebRTC
- FFmpeg
- AudioPlaybackCapture
- Real wireless sender/receiver
- Video file recording
- API keys
- `.env`

## Manual test checklist

1. Open MobileStudioV2.
2. Open Sender Mode.
3. Confirm Phase 6 label is visible.
4. Confirm Encoder dry run panel and Encoder metrics panel are visible.
5. Request screen-capture permission.
6. Start Encoder Dry Run.
7. Confirm status says H.264 encoder dry run active.
8. Confirm encoded bytes or output count increases after a few seconds.
9. Confirm metrics show encoder resolution, bitrate, FPS, key frames/config buffers, output format, uptime, and Network/File off.
10. Stop Encoder Dry Run.
11. Confirm it stops without crash and asks for permission again before restart.

## Next phase

Phase 7 should prepare a packet/buffer handoff layer for encoded H.264 data only.

The next phase should still avoid RTMP, audio, and real two-phone wireless transport unless explicitly approved.
