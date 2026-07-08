#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "FAIL: $1"
  exit 1
}

ok() {
  echo "OK: $1"
}

[ -f app/build.gradle ] || fail "missing app/build.gradle"
[ -f app/src/main/AndroidManifest.xml ] || fail "missing AndroidManifest.xml"
[ -f app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java ] || fail "missing MainActivity.java"
[ -f app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java ] || fail "missing ModeActivity.java"
[ -f app/src/main/java/com/yu/mobilestudio/v2/MediaProjectionKeepAliveService.java ] || fail "missing MediaProjectionKeepAliveService.java"
[ -f .github/workflows/android-build.yml ] || fail "missing GitHub Actions workflow"

grep -q 'versionCode 9' app/build.gradle || fail "versionCode is not 9"
grep -q 'versionName "0.9.0-phase9"' app/build.gradle || fail "versionName is not phase9"
grep -q 'Phase 9: Studio H.264 decode preview' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "MainActivity phase label missing"
grep -q 'Phase 9' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "ModeActivity phase badge missing"
grep -q '0x4D535639' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MSV9 packet magic missing"
grep -q 'DEFAULT_PORT = 56791' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "phase9 port missing"
grep -q 'SurfaceView' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Studio preview SurfaceView missing"
grep -q 'createDecoderByType' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "H.264 decoder creation missing"
grep -q 'Connect + Decode Preview' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Studio decode button missing"
grep -q 'Decoded frames' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "decoded frame metrics missing"
grep -q 'releaseOutputBuffer(outputIndex, true)' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "decoder render output missing"
grep -q 'MediaCodec' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MediaCodec usage missing"
grep -q 'MediaProjection' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MediaProjection usage missing"
grep -q 'ServerSocket' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "ServerSocket usage missing"
grep -q 'DataOutputStream' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "DataOutputStream packet sender missing"
grep -q 'DataInputStream' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "DataInputStream packet receiver missing"
grep -q 'android.permission.INTERNET' app/src/main/AndroidManifest.xml || fail "INTERNET permission missing"
grep -q 'FOREGROUND_SERVICE_MEDIA_PROJECTION' app/src/main/AndroidManifest.xml || fail "foreground media projection permission missing"
grep -q 'FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION' app/src/main/java/com/yu/mobilestudio/v2/MediaProjectionKeepAliveService.java || fail "service type startForeground missing"
grep -q 'verify_phase9.sh' .github/workflows/android-build.yml || fail "workflow does not run verify_phase9.sh"

FORBIDDEN_REGEX='RTMP|Rtmp|rtmp|WebRTC|webrtc|AudioPlaybackCapture|Browser Source|browser source|RECORD_AUDIO'
if grep -RInE "$FORBIDDEN_REGEX" app/src/main/java app/build.gradle app/src/main/AndroidManifest.xml 2>/dev/null; then
  fail "forbidden future feature keyword found in source/build files"
fi

ok "Required files exist"
ok "Phase 9 metadata verified"
ok "Sender packet transport code verified"
ok "Studio H.264 decode preview code verified"
ok "No forbidden future features found"

echo
echo "Phase 9 verify: PASS"

