package com.yu.mobilestudio.v2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class MediaProjectionKeepAliveService extends Service {
    public static final String CHANNEL_ID = "mobilestudio_projection";
    public static final int NOTIFICATION_ID = 2042;
    public static final String ACTION_STOP = "com.yu.mobilestudio.v2.STOP_PROJECTION_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
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
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setContentTitle("MobileStudioV2 Sender")
                .setContentText("H.264 LAN dry-run capture session active")
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setOngoing(true);

        return builder.build();
    }
}
