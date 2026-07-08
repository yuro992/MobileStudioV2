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
Status: PASS

## Phase 9 — Studio H.264 Decode Preview
Goal:

Receive H.264 packets on Studio, decode them with MediaCodec, and render them to a preview SurfaceView.

Pass criteria:

- Sender opens TCP packet server on port 56791.
- Sender encodes screen and sends config/key/delta packets.
- Studio connects to Sender.
- Studio receives packets.
- Studio creates H.264 decoder.
- Studio renders decoded frames to preview surface.
- Studio shows decoded frame metrics.
- No platform output, sound capture, file output, or overlay source.

## Phase 10 — Studio Preview Stability + Reconnect
Goal:

Improve decoder resilience, reconnect behavior, and preview scaling before adding live composition work.

Do not start until Phase 9 is confirmed PASS.

## Phase 9 hotfix — Studio preview render path

Use TextureView as the decoder output target for a more reliable preview surface on mobile devices.
