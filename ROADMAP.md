# MobileStudioV2 Roadmap

## Phase 0 — Toolchain Ready
Status: PASS

## Phase 1 — Android Skeleton + APK Build
Status: PASS

## Phase 2 — Screen Capture Permission Flow
Status: PASS

## Phase 3 — MediaProjection Capture Session
Status: PASS

## Phase 4 — Local Screen Preview
Status: PASS

## Phase 5 — Sender Preview Cleanup + Capture Metrics
Status: PASS

## Phase 6 — H.264 Encoder Dry Run
Status: PASS

## Phase 7 — Two-phone LAN Pairing + Studio Receiver Skeleton
Status: PASS

## Phase 8 — H.264 LAN Packet Sender Dry Run
Goal:

Send encoded H.264 packet chunks from Sender to Studio over LAN and count packets/bytes on Studio. Studio does not decode video yet.

Pass criteria:

- Sender shows local IP and port.
- Sender opens TCP packet server.
- Sender encodes screen with MediaCodec H.264.
- Sender sends config/key/delta packets to Studio.
- Studio connects to Sender.
- Studio receives packet chunks and shows packet/byte metrics.
- No decode preview, RTMP, audio, file output, or browser source.

## Phase 9 — Studio H.264 Decode Preview
Goal:

Decode received H.264 packets on Studio and render them to a preview Surface.

Do not start until Phase 8 is confirmed PASS.
