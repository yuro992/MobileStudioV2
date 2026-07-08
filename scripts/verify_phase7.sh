#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "FAIL: $1"
  exit 1
}

pass() {
  echo "OK: $1"
}

[ -f settings.gradle ] || fail "missing settings.gradle"
[ -f build.gradle ] || fail "missing root build.gradle"
[ -f app/build.gradle ] || fail "missing app/build.gradle"
[ -f app/src/main/AndroidManifest.xml ] || fail "missing AndroidManifest.xml"
[ -f app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java ] || fail "missing MainActivity.java"
[ -f app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java ] || fail "missing ModeActivity.java"
[ -f README.md ] || fail "missing README.md"
[ -f ROADMAP.md ] || fail "missing ROADMAP.md"
[ -f HANDOFF.md ] || fail "missing HANDOFF.md"
[ -f .github/workflows/android-build.yml ] || fail "missing GitHub Actions workflow"

grep -q 'versionName "0.7.0-phase7"' app/build.gradle || fail "versionName is not Phase 7"
grep -q 'versionCode 7' app/build.gradle || fail "versionCode is not 7"
grep -q 'android.permission.INTERNET' app/src/main/AndroidManifest.xml || fail "INTERNET permission missing"
grep -q 'Phase 7: Two-phone LAN pairing test' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "Phase 7 main label missing"
grep -q 'Start LAN Test Server' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Sender start server button missing"
grep -q 'Stop LAN Test Server' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Sender stop server button missing"
grep -q 'Connect to Sender' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Studio connect button missing"
grep -q 'Disconnect' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Studio disconnect button missing"
grep -q 'ServerSocket' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "ServerSocket missing"
grep -q 'Socket' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Socket client/server code missing"
grep -q 'MOBILESTUDIOV2_HEARTBEAT' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "heartbeat protocol missing"
grep -q 'DEFAULT_PORT = 56789' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "default port missing"
grep -q 'verify_phase7.sh' .github/workflows/android-build.yml || fail "workflow does not run Phase 7 verify"

FORBIDDEN_SOURCE_REGEX='AudioPlaybackCapture|FFmpeg|ffmpeg|Rtmp|RTMP|WebRTC|webrtc'
if grep -RInE "$FORBIDDEN_SOURCE_REGEX" app/src/main/java app/build.gradle settings.gradle 2>/dev/null; then
  fail "forbidden future transport/live/audio keyword found in source/build files"
fi

pass "Required files exist"
pass "Phase 7 version metadata verified"
pass "Android network permission verified"
pass "Sender LAN server UI verified"
pass "Studio LAN client UI verified"
pass "TCP heartbeat code verified"
pass "Workflow verify hook verified"
pass "No forbidden future-scope keywords found in source/build files"

echo
echo "Phase 7 verify: PASS"
