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
[ -f app/src/main/java/com/yu/mobilestudio/v2/MediaProjectionKeepAliveService.java ] || fail "missing MediaProjectionKeepAliveService.java"
[ -f README.md ] || fail "missing README.md"
[ -f ROADMAP.md ] || fail "missing ROADMAP.md"
[ -f HANDOFF.md ] || fail "missing HANDOFF.md"
[ -f .github/workflows/android-build.yml ] || fail "missing GitHub Actions workflow"

for script in scripts/verify_phase1.sh scripts/verify_phase2.sh scripts/verify_phase3.sh scripts/verify_phase4.sh; do
  [ -f "$script" ] || fail "missing previous verify script: $script"
done

grep -q 'namespace "com.yu.mobilestudio.v2"' app/build.gradle || fail "namespace mismatch"
grep -q 'applicationId "com.yu.mobilestudio.v2"' app/build.gradle || fail "applicationId mismatch"
grep -q 'versionName "0.5.0-phase5"' app/build.gradle || fail "phase5 versionName missing"
grep -q '<string name="app_name">MobileStudioV2</string>' app/src/main/res/values/strings.xml || fail "app name missing"
grep -q 'Phase 5: Preview cleanup + capture metrics' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "Phase 5 home label missing"
grep -q 'makeBadge("Phase 5")' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Phase 5 sender badge missing"
grep -q 'Capture metrics' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "metrics panel missing"
grep -q 'Preview surface:' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "preview surface metric missing"
grep -q 'Uptime:' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "uptime metric missing"
grep -q 'Keep screen on:' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "keep-screen-on metric missing"
grep -q 'FPS:' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "FPS placeholder metric missing"
grep -q 'FLAG_KEEP_SCREEN_ON' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "keep screen on flag missing"
grep -q 'onBackPressed' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "back cleanup guard missing"
grep -q 'MediaProjectionKeepAliveService' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "capture service use missing"
grep -q 'Verify Phase 5' .github/workflows/android-build.yml || fail "workflow does not run phase5 verify"
grep -q 'scripts/verify_phase5.sh' README.md || fail "README does not mention phase5 verify"
grep -q 'Phase 5 — Sender Preview Cleanup + Capture Metrics' ROADMAP.md || fail "ROADMAP phase5 missing"

FORBIDDEN_REGEX='MediaCodec|AudioPlaybackCapture|WebRTC|RTMP|FFmpeg|Rtmp|webrtc|ffmpeg|ServerSocket|DatagramSocket'

if grep -RInE "$FORBIDDEN_REGEX" app build.gradle settings.gradle 2>/dev/null; then
  fail "forbidden Phase 6+ implementation keyword found in source/build files"
fi

pass "Required files exist"
pass "Package/app metadata verified"
pass "Phase 5 UI and metrics verified"
pass "Capture cleanup guards verified"
pass "No Phase 6+ transport/encode/audio implementation found"

echo
echo "Phase 5 verify: PASS"
