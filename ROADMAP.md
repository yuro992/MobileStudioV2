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

Status: PASS

Completed:

- Sender Mode has a screen-capture permission button
- Android permission dialog opens
- Granting permission updates status to `Screen capture permission granted`
- Cancelling/denying permission updates status to `Permission denied or cancelled`
- Studio Mode remains placeholder only
- Verify script exists and passes
- GitHub Actions workflow builds debug APK

## Phase 3 — MediaProjection Capture Session

Status: IN PROGRESS

Goal:

After permission is granted, start and stop a safe MediaProjection foreground-service session.

Pass criteria:

- Sender Mode can request screen-capture permission
- Sender Mode can start capture session after permission is granted
- A foreground service of type `mediaProjection` is declared
- Capture service calls `getMediaProjection`
- Capture service registers `MediaProjection.Callback`
- Capture service displays foreground notification while active
- Sender Mode can stop capture session
- Sender Mode receives service status updates
- Verify script exists and passes
- GitHub Actions workflow builds debug APK
- No preview, virtual display output, encoding, audio, wireless stream, RTMP, WebRTC, or FFmpeg implementation

## Phase 4 — Local Capture Preview

Goal:

Create a local preview path by attaching the active capture session to a display surface.

Do not start until Phase 3 is confirmed PASS.
