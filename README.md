# MobileStudioV2

MobileStudioV2 is a two-phone mobile live studio experiment.

## Phase 1 status

Phase 1 is UI skeleton only.

Current features:

- Android app package: `com.yu.mobilestudio.v2`
- App name: `MobileStudioV2`
- Home screen with two modes:
  - Sender Mode
  - Studio Mode
- Placeholder screen for each mode
- Verify script
- GitHub Actions workflow for debug APK build

## Not included yet

Phase 1 intentionally does not include screen capture, internal audio capture, wireless sender/receiver stream, live output, realtime browser source, or native media encoding.

## Verify

```bash
bash scripts/verify_phase1.sh
```
