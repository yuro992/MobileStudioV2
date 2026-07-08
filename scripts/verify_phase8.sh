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

grep -q 'versionCode 8' app/build.gradle || fail "versionCode is not 8"
grep -q 'versionName "0.8.0-phase8"' app/build.gradle || fail "versionName is not phase8"
grep -q 'Phase 8: H.264 LAN packet dry run' app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java || fail "MainActivity phase label missing"
grep -q 'Phase 8' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "ModeActivity phase badge missing"
grep -q 'MediaCodec' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MediaCodec usage missing"
grep -q 'MediaProjection' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "MediaProjection usage missing"
grep -q 'ServerSocket' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "ServerSocket usage missing"
grep -q 'DataOutputStream' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "DataOutputStream packet sender missing"
grep -q 'DataInputStream' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "DataInputStream packet receiver missing"
grep -q 'Start H.264 LAN Dry Run' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Sender start button missing"
grep -q 'Packet receive metrics' app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java || fail "Studio packet metrics missing"
grep -q 'android.permission.INTERNET' app/src/main/AndroidManifest.xml || fail "INTERNET permission missing"
grep -q 'FOREGROUND_SERVICE_MEDIA_PROJECTION' app/src/main/AndroidManifest.xml || fail "foreground media projection permission missing"
grep -q 'verify_phase8.sh' .github/workflows/android-build.yml || fail "workflow does not run verify_phase8.sh"

FORBIDDEN_REGEX='RTMP|Rtmp|rtmp|WebRTC|webrtc|AudioPlaybackCapture|Browser Source|browser source|RECORD_AUDIO'
if grep -RInE "$FORBIDDEN_REGEX" app/src/main/java app/build.gradle app/src/main/AndroidManifest.xml 2>/dev/null; then
  fail "forbidden Phase 9+ keyword found in source/build files"
fi

ok "Required files exist"
ok "Phase 8 metadata verified"
ok "Sender H.264 packet sender code verified"
ok "Studio packet receiver code verified"
ok "No forbidden future features found"

echo
echo "Phase 8 verify: PASS"
