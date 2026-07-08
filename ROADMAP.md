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

Status: CURRENT

Goal:

Stabilize Sender local preview before network work by adding basic capture metrics, keep-screen-on behavior, safer back handling, resource cleanup, and clearer preview state.

Pass criteria:

- Phase 5 UI appears
- Sender preview still works
- Metrics panel is visible
- Metrics include screen size, dpi, preview surface size, orientation, uptime, keep-screen-on state, and FPS placeholder
- Capture session starts and stops cleanly
- Back/destroy releases capture resources
- No video encoding, wireless transport, RTMP, WebRTC, FFmpeg, or audio capture implementation

## Phase 6 — Sender Frame Pipeline Prep

Goal:

Prepare a frame-oriented pipeline for the sender side without sending video over the network yet.

Do not start until Phase 5 is confirmed PASS.
