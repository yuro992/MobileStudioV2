# MobileStudioV2

MobileStudioV2 is a two-phone mobile live studio experiment.

## Phase 8 status

Phase 8 is H.264 LAN packet sender dry run.

Current features:

- Sender Mode requests Android screen-capture permission.
- Sender Mode encodes the screen with MediaCodec H.264.
- Sender Mode opens a TCP LAN server on port `56790`.
- Sender sends framed H.264 packet chunks to a connected Studio client.
- Studio Mode connects to Sender by IP and port.
- Studio Mode receives packet data and reports byte/packet/key-frame/config metrics.

## Not included yet

Phase 8 intentionally does not include:

- Studio H.264 decode preview
- RTMP output
- Audio capture
- Browser Source
- File recording

## Verify

```bash
bash scripts/verify_phase8.sh
```
