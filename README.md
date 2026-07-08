# MobileStudioV2

MobileStudioV2 is a two-phone mobile live studio experiment.

## Current status

Phase 7 adds two-phone LAN pairing test behavior.

Current features:

- Android app package: `com.yu.mobilestudio.v2`
- App name: `MobileStudioV2`
- Sender Mode LAN test server
- Studio Mode LAN client
- TCP heartbeat messages between phones on the same Wi-Fi/LAN
- Connection metrics and heartbeat counters
- GitHub Actions workflow for debug APK build

## Phase 7 scope

Phase 7 tests only the connection path between the game phone and the stream phone.

Included:

- Sender local IP display
- Sender port display
- Start/stop LAN test server
- Studio IP input
- Studio port input
- Connect/disconnect button
- Heartbeat counter
- Basic latency estimate

Not included yet:

- Video packet transport
- Decoder
- Audio
- Browser Source
- Live output
- Recording

## Verify

```bash
bash scripts/verify_phase7.sh
```
