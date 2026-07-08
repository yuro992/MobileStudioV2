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
[ -f app/src/main/java/com/yu/mobilestudio/v2/CaptureService.java ] || fail "missing CaptureService.java"
[ -f README.md ] || fail "missing README.md"
[ -f ROADMAP.md ] || fail "missing ROADMAP.md"
[ -f HANDOFF.md ] || fail "missing HANDOFF.md"
[ -f .github/workflows/android-build.yml ] || fail "missing GitHub Actions workflow"

APP_BUILD="app/build.gradle"
MANIFEST="app/src/main/AndroidManifest.xml"
MAIN="app/src/main/java/com/yu/mobilestudio/v2/MainActivity.java"
MODE="app/src/main/java/com/yu/mobilestudio/v2/ModeActivity.java"
SERVICE="app/src/main/java/com/yu/mobilestudio/v2/CaptureService.java"

grep -q 'namespace "com.yu.mobilestudio.v2"' "$APP_BUILD" || fail "namespace mismatch"
grep -q 'applicationId "com.yu.mobilestudio.v2"' "$APP_BUILD" || fail "applicationId mismatch"
grep -q 'versionCode 3' "$APP_BUILD" || fail "versionCode is not 3"
grep -q 'versionName "0.3.0-phase3"' "$APP_BUILD" || fail "versionName is not phase3"
grep -q '<string name="app_name">MobileStudioV2</string>' app/src/main/res/values/strings.xml || fail "app name missing"

grep -q 'Phase 3: capture session service' "$MAIN" || fail "Phase 3 home label missing"
grep -q 'android.permission.FOREGROUND_SERVICE' "$MANIFEST" || fail "FOREGROUND_SERVICE permission missing"
grep -q 'android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION' "$MANIFEST" || fail "FOREGROUND_SERVICE_MEDIA_PROJECTION permission missing"
grep -q 'android:foregroundServiceType="mediaProjection"' "$MANIFEST" || fail "mediaProjection foreground service type missing"
grep -q 'android:name=".CaptureService"' "$MANIFEST" || fail "CaptureService manifest entry missing"

grep -q 'Request Screen Capture Permission' "$MODE" || fail "permission request button missing"
grep -q 'Start Capture Session' "$MODE" || fail "start capture session button missing"
grep -q 'Stop Capture Session' "$MODE" || fail "stop capture session button missing"
grep -q 'startForegroundService' "$MODE" || fail "startForegroundService missing"
grep -q 'CaptureService.ACTION_START' "$MODE" || fail "ACTION_START usage missing"
grep -q 'CaptureService.ACTION_STOP' "$MODE" || fail "ACTION_STOP usage missing"
grep -q 'BroadcastReceiver' "$MODE" || fail "status BroadcastReceiver missing"
grep -q 'Context.RECEIVER_NOT_EXPORTED' "$MODE" || fail "receiver not exported flag missing"

grep -q 'extends Service' "$SERVICE" || fail "CaptureService does not extend Service"
grep -q 'FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION' "$SERVICE" || fail "foreground service type call missing"
grep -q 'startForeground' "$SERVICE" || fail "startForeground missing"
grep -q 'getMediaProjection' "$SERVICE" || fail "getMediaProjection missing"
grep -q 'MediaProjection.Callback' "$SERVICE" || fail "MediaProjection.Callback missing"
grep -q 'registerCallback' "$SERVICE" || fail "MediaProjection callback registration missing"
grep -q 'Capture session active' "$SERVICE" || fail "active status missing"
grep -q 'Capture session stopped' "$SERVICE" || fail "stopped status missing"
grep -q 'NotificationChannel' "$SERVICE" || fail "notification channel missing"

FORBIDDEN_REGEX='AudioPlaybackCapture|WebRTC|RTMP|FFmpeg|Rtmp|webrtc|ffmpeg|createVirtualDisplay|VirtualDisplay|MediaCodec|ServerSocket|DatagramSocket'

if grep -RInE "$FORBIDDEN_REGEX" app/src app/build.gradle build.gradle settings.gradle 2>/dev/null; then
  fail "forbidden Phase 4+ implementation keyword found in source/build files"
fi

pass "Required files exist"
pass "Package/app metadata verified"
pass "Phase 3 foreground capture service source verified"
pass "No Phase 4+ implementation found in source/build files"

echo
echo "Phase 3 verify: PASS"
