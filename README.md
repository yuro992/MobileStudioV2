# MobileStudioV2

MobileStudioV2 is a two-phone mobile live studio experiment.

## Current status

Phase 6 is Sender H.264 Encoder Dry Run.

Current features:

- Android app package: `com.yu.mobilestudio.v2`
- App name: `MobileStudioV2`
- Home screen with two modes:
  - Sender Mode
  - Studio Mode
- Sender Mode can request Android screen-capture permission
- Sender Mode can start and stop a foreground capture session
- Sender Mode can encode the captured screen into H.264 dry-run output buffers using `MediaCodec`
- Sender Mode shows encoder metrics:
  - source screen size
  - encoder resolution
  - target bitrate
  - target FPS
  - encoded bytes
  - encoded output count
  - key frame count
  - codec config buffer count
  - output format summary
  - uptime
- Nothing is saved, streamed, sent, or recorded in Phase 6
- Studio Mode remains a placeholder
- Verify script
- GitHub Actions workflow for debug APK build

## Not included yet

Phase 6 intentionally does not include:

- Wireless sender/receiver video transport
- RTMP output
- WebRTC
- FFmpeg
- Internal audio capture
- Browser Source
- Video recording or file output

## Verify

```bash
bash scripts/verify_phase6.sh
```

## Build

The APK build runs through GitHub Actions.

Workflow file:

```txt
.github/workflows/android-build.yml
```
