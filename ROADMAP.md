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

Created minimal Android app with Sender Mode and Studio Mode placeholders.

## Phase 2 — Screen Capture Permission Flow

Status: PASS

Added Android MediaProjection permission request flow in Sender Mode.

## Phase 3 — MediaProjection Capture Session

Status: PASS

Added foreground capture session start/stop behavior and keep-alive notification.

## Phase 4 — Local Screen Preview

Status: CURRENT

Goal:

Show the phone screen inside Sender Mode using MediaProjection, VirtualDisplay, and SurfaceView.

Pass criteria:

- Sender Mode has a preview surface
- Android screen-capture permission can be requested
- Foreground capture service starts
- VirtualDisplay is created after permission is granted
- Local preview starts and stops
- No video encoding, RTMP, WebRTC, FFmpeg, wireless sender/receiver, or internal audio capture implementation

## Phase 5 — Preview Stability + Orientation Handling

Status: NEXT

Goal:

Make local preview more stable across rotation, app background/foreground, screen size changes, and repeated start/stop cycles.

Do not start until Phase 4 is confirmed PASS.
