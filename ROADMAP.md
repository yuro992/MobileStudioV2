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

Completed:

- Android project exists
- Package name is `com.yu.mobilestudio.v2`
- App name is `MobileStudioV2`
- Main screen shows Sender Mode and Studio Mode
- Sender Mode opens placeholder screen
- Studio Mode opens placeholder screen
- Verify script exists and passes
- GitHub Actions workflow exists and builds APK

## Phase 2 — Screen Capture Permission Flow

Status: IN PROGRESS

Goal:

Add Android screen-capture permission request in Sender Mode.

Pass criteria:

- Sender Mode has a screen-capture permission button
- Android permission dialog opens
- Granting permission updates status to `Screen capture permission granted`
- Cancelling/denying permission updates status to `Permission denied or cancelled`
- Studio Mode remains placeholder only
- Verify script exists and passes
- GitHub Actions workflow builds debug APK
- No preview, virtual display, encoding, audio, wireless stream, RTMP, WebRTC, or FFmpeg implementation

## Phase 3 — Local Capture Preview

Goal:

Use the granted permission to create a safe local preview path.

Do not start until Phase 2 is confirmed PASS.
