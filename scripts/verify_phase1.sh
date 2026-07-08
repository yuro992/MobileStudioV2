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

grep -q 'namespace "com.yu.mobilestudio.v2"' app/build.gradle || fail "namespace mismatch"
grep -q 'applicationId "com.yu.mobilestudio.v2"' app/build.gradle || fail "applicationId mismatch"
grep -q '<string name="app_name">MobileStudioV2</string>' app/src/main/res/values/strings.xml || fail "app name missing"
grep -q 'Sender Mode' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "Sender Mode missing"
grep -q 'Studio Mode' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "Studio Mode missing"
grep -q 'Phase 1: UI skeleton only' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "Phase 1 label missing"
grep -q 'Mode Ready' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "ModeActivity ready text missing"

FORBIDDEN_REGEX='MediaProjection|AudioPlaybackCapture|WebRTC|RTMP|FFmpeg|Rtmp|webrtc|ffmpeg'

if grep -RInE "$FORBIDDEN_REGEX" app build.gradle settings.gradle 2>/dev/null; then
  fail "forbidden Phase 2+ implementation keyword found in source/build files"
fi

pass "Required files exist"
pass "Package/app metadata verified"
pass "Sender/Studio placeholders verified"
pass "No Phase 2+ implementation found in source/build files"

echo
printf '%s\n' 'Phase 1 verify: PASS'
