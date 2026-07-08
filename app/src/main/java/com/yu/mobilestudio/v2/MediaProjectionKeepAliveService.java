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
    public static final String ACTION_STOP = "com.yu.mobilestudio.v2.action.STOP_MEDIA_PROJECTION_SERVICE";

    private static final String CHANNEL_ID = "mobilestudio_capture";
    private static final int NOTIFICATION_ID = 4005;
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
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification();
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MobileStudio capture",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Keeps the local screen preview capture session active.");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
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
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setContentTitle("MobileStudioV2 preview active")
                .setContentText("Local preview is running with capture metrics. No video is being sent.")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
