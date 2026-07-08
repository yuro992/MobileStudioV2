#!/usr/bin/env bash
set -euo pipefail
fail() { echo "FAIL: $1" >&2; exit 1; }

[ -f app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java ] || fail "ModeActivity missing"
[ -x scripts/verify_phase9.sh ] || fail "verify_phase9.sh missing or not executable"

bash scripts/verify_phase9.sh

grep -q "Start Test Pattern LAN Preview" app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "test pattern button missing"
grep -q "startSenderTestPatternLanPreview" app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "test pattern start method missing"
grep -q "startTestPatternDrawThread" app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "test pattern draw thread missing"
grep -q "drawTestPatternFrame" app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "test pattern draw method missing"
grep -q "lockCanvas" app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Canvas render path missing"
grep -q "TEST PATTERN" app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "test pattern label missing"
grep -q "setButtonEnabled(testPatternButton, !senderActive)" app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "test pattern button enable rule missing"

echo "Phase 9B verify: PASS"
