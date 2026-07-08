package com.yu.mobilestudio.v2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class MediaProjectionKeepAliveService extends Service {

    public static final String ACTION_START = "com.yu.mobilestudio.v2.action.START_MEDIA_PROJECTION_SERVICE";
    public static final String ACTION_STOP = "com.yu.mobilestudio.v2.STOP_PROJECTION_SERVICE";

    private static final String CHANNEL_ID = "mobilestudio_projection";
    private static final int NOTIFICATION_ID = 2042;
    private static volatile boolean running = false;

    public static boolean isRunning() {
        return running;
    }

    public static Intent startIntent(Context context) {
        Intent intent = new Intent(context, MediaProjectionKeepAliveService.class);
        intent.setAction(ACTION_START);
        return intent;
    }

    public static Intent stopIntent(Context context) {
        Intent intent = new Intent(context, MediaProjectionKeepAliveService.class);
        intent.setAction(ACTION_STOP);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            running = false;
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        running = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "MobileStudio capture",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Foreground capture session for MobileStudioV2");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setContentTitle("MobileStudioV2 Sender")
                .setContentText("H.264 LAN dry-run capture session active")
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        return builder.build();
    }
}
