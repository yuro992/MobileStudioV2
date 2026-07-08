# MobileStudioV2

MobileStudioV2 is a two-phone mobile live studio experiment.

## Current status

Phase 5 is Sender preview cleanup plus capture metrics.

Current features:

- Android app package: `com.yu.mobilestudio.v2`
- App name: `MobileStudioV2`
- Home screen with two modes:
  - Sender Mode
  - Studio Mode
- Sender Mode can request Android screen-capture permission
- Sender Mode can start and stop a foreground capture session
- Sender Mode can show the phone screen in a local preview surface
- Sender Mode shows basic capture metrics:
  - screen size
  - density dpi
  - preview surface size
  - orientation
  - preview uptime
  - keep-screen-on state
  - FPS placeholder status
- Sender Mode keeps the screen awake while preview is active
- Sender Mode releases capture resources when leaving the screen
- Studio Mode remains a placeholder
- Verify script
- GitHub Actions workflow for debug APK build

## Not included yet

Phase 5 intentionally does not include:

- Video encoding
- Wireless sender/receiver video transport
- RTMP output
- WebRTC
- FFmpeg
- Internal audio capture
- Browser Source

## Verify

```bash
bash scripts/verify_phase5.sh
```

## Build

The APK build runs through GitHub Actions.

Workflow file:

```txt
.github/workflows/android-build.yml
```
