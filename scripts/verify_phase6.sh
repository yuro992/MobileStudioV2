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

for script in scripts/verify_phase1.sh scripts/verify_phase2.sh scripts/verify_phase3.sh scripts/verify_phase4.sh scripts/verify_phase5.sh; do
  [ -f "$script" ] || fail "missing previous verify script: $script"
done

grep -q 'namespace "com.yu.mobilestudio.v2"' app/build.gradle || fail "namespace mismatch"
grep -q 'applicationId "com.yu.mobilestudio.v2"' app/build.gradle || fail "applicationId mismatch"
grep -q 'versionName "0.6.0-phase6"' app/build.gradle || fail "phase6 versionName missing"
grep -q '<string name="app_name">MobileStudioV2</string>' app/src/main/res/values/strings.xml || fail "app name missing"
grep -q 'Phase 6: H.264 encoder dry run' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "Phase 6 home label missing"
grep -q 'makeBadge("Phase 6")' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Phase 6 sender badge missing"
grep -q 'MediaCodec' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MediaCodec encoder missing"
grep -q 'MediaFormat.MIMETYPE_VIDEO_AVC' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "H.264 MIME type missing"
grep -q 'createInputSurface' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "encoder input surface missing"
grep -q 'createVirtualDisplay' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "VirtualDisplay missing"
grep -q 'dequeueOutputBuffer' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "encoder output drain missing"
grep -q 'encodedBytes' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "encoded bytes metric missing"
grep -q 'encodedOutputCount' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "encoded output count metric missing"
grep -q 'keyFrameCount' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "key frame metric missing"
grep -q 'Network: off | File: off' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "network/file off metric missing"
grep -q 'Verify Phase 6' .github/workflows/android-build.yml || fail "workflow does not run phase6 verify"
grep -q 'scripts/verify_phase6.sh' README.md || fail "README does not mention phase6 verify"
grep -q 'Phase 6 — Sender H.264 Encoder Dry Run' ROADMAP.md || fail "ROADMAP phase6 missing"

FORBIDDEN_REGEX='AudioPlaybackCapture|WebRTC|RTMP|FFmpeg|Rtmp|webrtc|ffmpeg|ServerSocket|DatagramSocket|SocketChannel|MediaMuxer|Muxer|\.mp4|rtmp://|ws://|wss://'

if grep -RInE "$FORBIDDEN_REGEX" app build.gradle settings.gradle 2>/dev/null; then
  fail "forbidden Phase 7+ transport/audio/file-output keyword found in source/build files"
fi

pass "Required files exist"
pass "Package/app metadata verified"
pass "Phase 6 H.264 encoder dry run verified"
pass "Encoder metrics verified"
pass "No transport/audio/file-output implementation found"

echo
echo "Phase 6 verify: PASS"
