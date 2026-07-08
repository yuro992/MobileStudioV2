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
grep -q 'versionCode 2' app/build.gradle || fail "versionCode is not 2"
grep -q 'versionName "0.2.0-phase2"' app/build.gradle || fail "versionName is not phase2"
grep -q '<string name="app_name">MobileStudioV2</string>' app/src/main/res/values/strings.xml || fail "app name missing"

grep -q 'Phase 2: screen-capture permission flow' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "Phase 2 home label missing"
grep -q 'Request Screen Capture Permission' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "permission request button missing"
grep -q 'MediaProjectionManager' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MediaProjectionManager missing"
grep -q 'createScreenCaptureIntent' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "screen capture intent missing"
grep -q 'startActivityForResult' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "permission launch missing"
grep -q 'onActivityResult' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "permission result handler missing"
grep -q 'Screen capture permission granted' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "granted status missing"
grep -q 'Permission denied or cancelled' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "denied/cancelled status missing"

FORBIDDEN_REGEX='AudioPlaybackCapture|WebRTC|RTMP|FFmpeg|Rtmp|webrtc|ffmpeg|createVirtualDisplay|VirtualDisplay|MediaCodec|ServerSocket|DatagramSocket'

if grep -RInE "$FORBIDDEN_REGEX" app/src app/build.gradle build.gradle settings.gradle 2>/dev/null; then
  fail "forbidden Phase 3+ implementation keyword found in source/build files"
fi

pass "Required files exist"
pass "Package/app metadata verified"
pass "Phase 2 permission flow source verified"
pass "No Phase 3+ implementation found in source/build files"

echo
echo "Phase 2 verify: PASS"
