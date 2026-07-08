# MobileStudioV2 Handoff

## Current phase

Phase 9 — Studio H.264 Decode Preview

## What exists

- Sender screen-capture permission flow
- Sender MediaProjection foreground keep-alive service
- Sender MediaCodec H.264 encoder
- Sender TCP packet server on port 56791
- Sender framed H.264 packet writer with dimensions in packet header
- Studio TCP packet receiver
- Studio MediaCodec H.264 decoder
- Studio SurfaceView preview renderer
- Studio metrics for packets, bytes, config buffers, key frames, delta frames, decoded frames, drops, and errors

## Important constraints

Do not add these in Phase 9:

- Streaming-platform output
- WebRTC
- Sound capture path
- Browser overlay source
- File recording
- API keys
- `.env`

## Next phase

Phase 10 should stabilize Studio preview and reconnect behavior only.

It should not add live platform output, sound capture, browser overlay source, or full scene composition yet.

## Phase 9 hotfix note

Studio preview output was moved from SurfaceView to TextureView. Keep this in mind before future decode/render work.
