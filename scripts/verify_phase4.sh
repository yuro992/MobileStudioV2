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

grep -q 'namespace "com.yu.mobilestudio.v2"' app/build.gradle || fail "namespace mismatch"
grep -q 'applicationId "com.yu.mobilestudio.v2"' app/build.gradle || fail "applicationId mismatch"
grep -q 'versionName "0.4.0-phase4"' app/build.gradle || fail "phase4 versionName missing"
grep -q 'FOREGROUND_SERVICE_MEDIA_PROJECTION' app/src/main/AndroidManifest.xml || fail "media projection foreground service permission missing"
grep -q 'foregroundServiceType="mediaProjection"' app/src/main/AndroidManifest.xml || fail "mediaProjection foreground service type missing"
grep -q 'MediaProjectionManager' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MediaProjectionManager missing"
grep -q 'MediaProjection.Callback' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MediaProjection callback missing"
grep -q 'SurfaceView' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "SurfaceView missing"
grep -q 'VirtualDisplay' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "VirtualDisplay missing"
grep -q 'createVirtualDisplay' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "createVirtualDisplay missing"
grep -q 'Start Local Preview' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Start Local Preview button missing"
grep -q 'Stop Local Preview' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Stop Local Preview button missing"
grep -q 'Capture preview active' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "active preview status missing"
grep -q 'startForeground' app/src/main/java/com/yu/mobilestudio/v2/MediaProjectionKeepAliveService.java || fail "foreground service start missing"
grep -q 'setup-gradle' .github/workflows/android-build.yml || fail "Gradle setup action missing"
grep -q 'verify_phase4.sh' .github/workflows/android-build.yml || fail "workflow must run verify_phase4.sh"

FORBIDDEN_REGEX='AudioPlaybackCapture|WebRTC|RTMP|Rtmp|webrtc|ffmpeg|FFmpeg|MediaCodec|MediaMuxer|AudioRecord|AudioTrack'

if grep -RInE "$FORBIDDEN_REGEX" app build.gradle settings.gradle 2>/dev/null; then
  fail "forbidden Phase 5+ implementation keyword found in source/build files"
fi

pass "Required files exist"
pass "Package/app metadata verified"
pass "MediaProjection foreground service verified"
pass "SurfaceView local preview implementation verified"
pass "VirtualDisplay implementation verified"
pass "No Phase 5+ implementation found in source/build files"

echo
echo "Phase 4 verify: PASS"
