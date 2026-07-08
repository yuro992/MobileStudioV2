# MobileStudioV2

MobileStudioV2 is a two-phone mobile live studio experiment.

## Phase 9 status

Phase 9 is Studio H.264 decode preview.

Current features:

- Sender Mode requests Android screen-capture permission.
- Sender Mode encodes the screen with MediaCodec H.264.
- Sender Mode opens a TCP LAN server on port `56791`.
- Sender sends framed H.264 packet chunks to a connected Studio client.
- Studio Mode connects to Sender by IP and port.
- Studio Mode receives packet data, feeds it to a MediaCodec H.264 decoder, and renders to a preview SurfaceView.
- Studio Mode reports packet, byte, decoder, and frame metrics.

## Not included yet

Phase 9 intentionally does not include:

- Streaming-platform output
- Sound capture
- Browser overlay source
- File recording

## Verify

```bash
bash scripts/verify_phase9.sh
```

