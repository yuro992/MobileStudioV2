# MobileStudioV2 Handoff

## Current phase

Phase 8 — H.264 LAN Packet Sender Dry Run

## What exists

- Sender screen-capture permission flow
- Sender MediaProjection foreground keep-alive service
- Sender MediaCodec H.264 encoder
- Sender TCP packet server on port 56790
- Sender framed H.264 packet writer
- Studio TCP packet receiver
- Studio packet metrics for bytes, packets, config buffers, key frames, delta frames

## Important constraints

Do not add these in Phase 8:

- RTMP
- WebRTC
- AudioPlaybackCapture
- H.264 decode preview
- Browser Source
- File recording
- API keys
- `.env`

## Next phase

Phase 9 should add Studio H.264 decode preview only.

It should not add RTMP, audio, browser source, or streaming platform output yet.
