# MobileStudioV2 Roadmap

## Phase 0 — Toolchain Ready

Status: PASS

## Phase 1 — Android Skeleton + APK Build

Goal:

Create a minimal Android app with Sender Mode and Studio Mode placeholders, plus a GitHub Actions debug APK build.

Pass criteria:

- Android project exists
- Package name is `com.yu.mobilestudio.v2`
- App name is `MobileStudioV2`
- Main screen shows Sender Mode and Studio Mode
- Sender Mode opens placeholder screen
- Studio Mode opens placeholder screen
- Verify script exists and passes
- GitHub Actions workflow exists
- No real screen capture, audio, wireless, live output, realtime browser source, or native media encoding implementation

## Phase 2 — Screen Capture Permission Flow

Goal:

Add Android screen capture permission request and local preview path.

Do not start until Phase 1 is confirmed PASS.
