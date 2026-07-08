package com.yu.mobilestudio.v2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

public class CaptureService extends Service {

    public static final String ACTION_START = "com.yu.mobilestudio.v2.action.START_CAPTURE_SESSION";
    public static final String ACTION_STOP = "com.yu.mobilestudio.v2.action.STOP_CAPTURE_SESSION";
    public static final String ACTION_STATUS = "com.yu.mobilestudio.v2.action.CAPTURE_STATUS";
    public static final String EXTRA_RESULT_CODE = "com.yu.mobilestudio.v2.extra.RESULT_CODE";
    public static final String EXTRA_RESULT_DATA = "com.yu.mobilestudio.v2.extra.RESULT_DATA";
    public static final String EXTRA_STATUS = "com.yu.mobilestudio.v2.extra.STATUS";
    public static final String EXTRA_SUCCESS = "com.yu.mobilestudio.v2.extra.SUCCESS";

    private static final String CHANNEL_ID = "capture_session";
    private static final int NOTIFICATION_ID = 3003;

    private MediaProjection mediaProjection;
    private final MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            mediaProjection = null;
            broadcastStatus("Capture session stopped by Android", false);
            stopForegroundServiceSafely();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startCaptureSession(intent);
            return START_NOT_STICKY;
        }

        if (ACTION_STOP.equals(action)) {
            stopCaptureSession("Capture session stopped");
            return START_NOT_STICKY;
        }

        return START_NOT_STICKY;
    }

    private void startCaptureSession(Intent intent) {
        broadcastStatus("Capture session starting...", false);
        startAsForegroundService();

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData = readProjectionData(intent);

        if (resultCode == 0 || resultData == null) {
            broadcastStatus("Capture session failed: missing permission data", false);
            stopForegroundServiceSafely();
            return;
        }

        stopExistingProjectionOnly();

        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (projectionManager == null) {
            broadcastStatus("Capture session failed: service unavailable", false);
            stopForegroundServiceSafely();
            return;
        }

        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
        if (mediaProjection == null) {
            broadcastStatus("Capture session failed: projection unavailable", false);
            stopForegroundServiceSafely();
            return;
        }

        mediaProjection.registerCallback(projectionCallback, null);
        broadcastStatus("Capture session active", true);
    }

    @SuppressWarnings("deprecation")
    private Intent readProjectionData(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        }
        return intent.getParcelableExtra(EXTRA_RESULT_DATA);
    }

    private void startAsForegroundService() {
        Notification notification = buildNotification("Capture session is active");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String message) {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("MobileStudioV2")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Capture Session",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Keeps the screen-capture session alive while MobileStudioV2 is running.");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void stopCaptureSession(String status) {
        stopExistingProjectionOnly();
        broadcastStatus(status, false);
        stopForegroundServiceSafely();
    }

    private void stopExistingProjectionOnly() {
        if (mediaProjection == null) {
            return;
        }

        try {
            mediaProjection.unregisterCallback(projectionCallback);
        } catch (RuntimeException ignored) {
            // Callback may already be unregistered if Android stopped the projection first.
        }

        try {
            mediaProjection.stop();
        } catch (RuntimeException ignored) {
            // Projection may already be stopped by the system.
        }

        mediaProjection = null;
    }

    private void stopForegroundServiceSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    private void broadcastStatus(String status, boolean success) {
        Intent intent = new Intent(ACTION_STATUS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_SUCCESS, success);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        stopExistingProjectionOnly();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
