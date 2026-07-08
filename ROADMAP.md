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

## Phase 6 — Sender H.264 Encoder Dry Run

Status: PASS

## Phase 7 — Two-phone LAN Pairing + Studio Receiver Skeleton

Status: CURRENT

Goal:

Make the Sender phone and Studio phone connect over Wi-Fi/LAN using a simple TCP heartbeat test before transporting video packets.

Pass criteria:

- Main screen shows Phase 7
- Sender Mode shows local IP and port
- Sender can start and stop a LAN test server
- Studio Mode can enter Sender IP and port
- Studio can connect to Sender
- Sender sends heartbeat messages
- Studio receives heartbeat messages
- Metrics show connection state and counters
- No video transport, decoder, audio, live output, or recording yet

## Phase 8 — H.264 Packet Transport Dry Run

Goal:

Send encoded H.264 buffers from Sender to Studio over the already-tested LAN path.

Do not add audio or live output in Phase 8.
