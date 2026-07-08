# MobileStudioV2

MobileStudioV2 is a two-phone mobile live studio experiment.

## Phase 3 status

Phase 3 adds a safe MediaProjection foreground-service capture session in Sender Mode.

Current features:

- Android app package: `com.yu.mobilestudio.v2`
- App name: `MobileStudioV2`
- Home screen with two modes:
  - Sender Mode
  - Studio Mode
- Sender Mode can request Android screen-capture permission
- Sender Mode can start a foreground capture session after permission is granted
- Sender Mode can stop the capture session
- Capture service shows an Android notification while active
- Sender Mode displays session status:
  - Not requested
  - Waiting for Android permission dialog
  - Permission granted. Ready to start session
  - Capture session starting
  - Capture session active
  - Capture session stopped
- Studio Mode remains placeholder only
- Verify script
- GitHub Actions workflow for debug APK build

## Not included yet

Phase 3 intentionally does not include:

- Screen preview
- Virtual display output
- Video encoding
- Internal audio capture
- Wireless sender/receiver stream
- RTMP
- WebRTC
- FFmpeg
- Browser Source

## Verify

```bash
bash scripts/verify_phase3.sh
```

## Build

GitHub Actions builds the debug APK.

Workflow file:

```txt
.github/workflows/android-build.yml
```
