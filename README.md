# MobileStudioV2

MobileStudioV2 is a two-phone mobile live studio experiment.

## Phase 2 status

Phase 2 adds Android screen-capture permission flow in Sender Mode.

Current features:

- Android app package: `com.yu.mobilestudio.v2`
- App name: `MobileStudioV2`
- Home screen with two modes:
  - Sender Mode
  - Studio Mode
- Sender Mode can request Android screen-capture permission
- Sender Mode displays permission status:
  - Not requested
  - Waiting for Android permission dialog
  - Screen capture permission granted
  - Permission denied or cancelled
- Studio Mode remains placeholder only
- Verify script
- GitHub Actions workflow for debug APK build

## Not included yet

Phase 2 intentionally does not include:

- Screen preview
- Virtual display creation
- Video encoding
- Internal audio capture
- Wireless sender/receiver stream
- RTMP
- WebRTC
- FFmpeg
- Browser Source

## Verify

```bash
bash scripts/verify_phase2.sh
```

## Build

GitHub Actions builds the debug APK.

Workflow file:

```txt
.github/workflows/android-build.yml
```
