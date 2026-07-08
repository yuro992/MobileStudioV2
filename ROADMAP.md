# MobileStudioV2 Roadmap

## Phase 0 — Toolchain Ready

Status: PASS

Required tools:

- git
- Java 17
- node
- npm
- python3
- jq
- ripgrep

## Phase 1 — Android Skeleton + APK Build

Status: PASS

Goal:

Create a minimal Android app with Sender Mode and Studio Mode placeholders, plus a GitHub Actions debug APK build.

## Phase 2 — Screen Capture Permission Flow

Status: PASS

Goal:

Add Android screen-capture permission request in Sender Mode.

## Phase 3 — MediaProjection Capture Session

Status: PASS

Goal:

After permission is granted, start and stop a foreground MediaProjection capture session.

## Phase 4 — Local Screen Preview

Status: PASS

Goal:

Show the phone screen inside the Sender Mode local preview surface.

## Phase 5 — Sender Preview Cleanup + Capture Metrics

Status: PASS

Goal:

Stabilize Sender local preview before network work by adding basic capture metrics, keep-screen-on behavior, safer back handling, resource cleanup, and clearer preview state.

## Phase 6 — Sender H.264 Encoder Dry Run

Status: CURRENT

Goal:

Encode the captured screen into local H.264 dry-run output buffers using MediaCodec, without saving, sending, streaming, or recording video.

Pass criteria:

- Phase 6 UI appears
- Sender can request screen-capture permission
- Sender can start Encoder Dry Run
- MediaProjection feeds a MediaCodec H.264 input surface
- Encoder metrics show resolution, bitrate, FPS, encoded bytes, output count, key frames, config buffers, and format
- Sender can stop the encoder cleanly
- No network video transport, RTMP, WebRTC, FFmpeg, audio capture, file output, or recording implementation

## Phase 7 — Sender Packet Buffer / Local Transport Prep

Goal:

Prepare encoded H.264 buffers for later transport without connecting two phones yet.

Do not start until Phase 6 is confirmed PASS.
