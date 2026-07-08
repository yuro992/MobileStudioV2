# MobileStudioV2 Handoff

## Current phase

Phase 7 — Two-phone LAN Pairing + Studio Receiver Skeleton

## What changed in Phase 7

- Added Android network permissions
- Sender Mode now starts a TCP LAN test server
- Sender displays local IPv4 address and fixed port `56789`
- Sender sends one heartbeat message per second to connected Studio clients
- Studio Mode can enter Sender IP and port
- Studio connects to Sender and reads heartbeat messages
- Studio displays heartbeat counter, last heartbeat age, and basic latency estimate
- Added `scripts/verify_phase7.sh`
- Updated GitHub Actions to verify Phase 7

## Test instructions

Use two phones on the same Wi-Fi/LAN:

1. Install the same Phase 7 APK on both phones.
2. On the game phone, open Sender Mode.
3. Tap `Start LAN Test Server`.
4. Copy the Sender IP shown on the Sender phone.
5. On the stream phone, open Studio Mode.
6. Enter the Sender IP and port `56789`.
7. Tap `Connect to Sender`.
8. Confirm that heartbeat counters increase.
9. Stop/disconnect and confirm both sides recover.

## Important constraints

Phase 7 intentionally does not include:

- Video transport
- Decoder
- Audio
- Browser Source
- Live output
- Recording
- API keys
- `.env`

## Next phase

Phase 8 should send encoded H.264 buffers over the LAN path established in Phase 7.
