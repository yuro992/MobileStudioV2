package com.yu.mobilestudio.v2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Locale;

public class ModeActivity extends Activity {

    private static final int REQUEST_CAPTURE_PERMISSION = 2602;
    private static final int TARGET_SHORT_SIDE = 720;
    private static final int TARGET_FPS = 30;
    private static final int TARGET_BITRATE = 4_000_000;
    private static final int I_FRAME_INTERVAL_SECONDS = 2;
    private static final String AVC_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String mode;
    private TextView statusView;
    private TextView metricsView;
    private TextView requestButton;
    private TextView startButton;
    private TextView stopButton;

    private Intent projectionPermissionData;
    private int projectionPermissionResultCode = Activity.RESULT_CANCELED;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec videoEncoder;
    private Surface encoderInputSurface;
    private Thread encoderDrainThread;

    private volatile boolean encoderActive = false;
    private volatile boolean drainRunning = false;
    private boolean releasingSession = false;

    private int sourceWidth = 0;
    private int sourceHeight = 0;
    private int sourceDensity = 0;
    private int encoderWidth = 0;
    private int encoderHeight = 0;
    private long encoderStartedAtMs = 0L;
    private long encodedBytes = 0L;
    private long encodedOutputCount = 0L;
    private long keyFrameCount = 0L;
    private long codecConfigCount = 0L;
    private String outputFormatSummary = "waiting";

    private final Runnable metricsTicker = new Runnable() {
        @Override
        public void run() {
            updateMetrics();
            if (encoderActive) {
                mainHandler.postDelayed(this, 1000);
            }
        }
    };

    private final MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            mainHandler.post(() -> releaseEncoderSession(false, "Encoder dry run stopped by Android. Request permission again."));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mode = getIntent().getStringExtra(MainActivity.EXTRA_MODE);
        if (mode == null || mode.trim().isEmpty()) {
            mode = "Unknown";
        }

        setTitle(mode + " Mode");

        if ("Sender".equals(mode)) {
            buildSenderScreen();
        } else {
            buildStudioPlaceholderScreen();
        }
    }

    @Override
    protected void onDestroy() {
        releaseEncoderSession(true, null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (encoderActive || projectionPermissionData != null) {
            releaseEncoderSession(true, null);
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CAPTURE_PERMISSION) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            projectionPermissionResultCode = resultCode;
            projectionPermissionData = data;
            setStatus("Permission granted. Ready to start encoder dry run", Color.rgb(21, 128, 61));
        } else {
            projectionPermissionResultCode = Activity.RESULT_CANCELED;
            projectionPermissionData = null;
            setStatus("Screen capture permission denied", Color.rgb(185, 28, 28));
        }

        updateMetrics();
        updateButtons();
    }

    private void buildSenderScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(250, 250, 255));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView badge = makeBadge("Phase 6");
        root.addView(badge, wrapWithBottom(dp(14)));

        TextView title = makeText("Sender Mode Ready", 28, Color.rgb(28, 25, 23), true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidthWrapWithBottom(dp(10)));

        TextView description = makeText(
                "H.264 encoder dry run. This phase encodes screen buffers locally but does not save, send, stream, or record video.",
                15,
                Color.rgb(87, 83, 78),
                false
        );
        description.setGravity(Gravity.CENTER);
        root.addView(description, fullWidthWrapWithBottom(dp(14)));

        TextView encoderPanel = makeText(
                "Encoder dry run\nMediaProjection → MediaCodec H.264\nNo file output • No wireless transport • No audio",
                13,
                Color.rgb(68, 64, 60),
                false
        );
        encoderPanel.setGravity(Gravity.CENTER);
        encoderPanel.setPadding(dp(12), dp(18), dp(12), dp(18));
        encoderPanel.setBackground(makePanelBackground());
        root.addView(encoderPanel, fullWidthWrapWithBottom(dp(12)));

        statusView = makeText("Status: Not requested", 15, Color.rgb(68, 64, 60), true);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(12), dp(12), dp(12), dp(12));
        statusView.setBackground(makeRoundedBackground(Color.WHITE, dp(18)));
        root.addView(statusView, fullWidthWrapWithBottom(dp(10)));

        metricsView = makeText("Encoder metrics: waiting for permission", 12, Color.rgb(87, 83, 78), false);
        metricsView.setGravity(Gravity.CENTER);
        metricsView.setPadding(dp(12), dp(10), dp(12), dp(10));
        metricsView.setBackground(makeRoundedBackground(Color.rgb(245, 245, 244), dp(16)));
        root.addView(metricsView, fullWidthWrapWithBottom(dp(14)));

        requestButton = makeButton("Request Screen Capture Permission", Color.rgb(124, 58, 237));
        requestButton.setOnClickListener(v -> requestScreenCapturePermission());
        root.addView(requestButton, fullWidthWrapWithBottom(dp(10)));

        startButton = makeButton("Start Encoder Dry Run", Color.rgb(21, 128, 61));
        startButton.setOnClickListener(v -> startEncoderDryRun());
        root.addView(startButton, fullWidthWrapWithBottom(dp(10)));

        stopButton = makeButton("Stop Encoder Dry Run", Color.rgb(185, 28, 28));
        stopButton.setOnClickListener(v -> releaseEncoderSession(true, "Encoder dry run stopped. Request permission again."));
        root.addView(stopButton, fullWidthWrapWithBottom(dp(14)));

        TextView back = makeButton("Back", Color.rgb(39, 39, 42));
        back.setOnClickListener(v -> finish());
        root.addView(back, wrap());

        updateMetrics();
        updateButtons();
        setContentView(scrollView);
    }

    private void buildStudioPlaceholderScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(250, 250, 255));

        root.addView(makeBadge("Phase 6"), wrapWithBottom(dp(22)));

        TextView title = makeText("Studio Mode Ready", 28, Color.rgb(28, 25, 23), true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidthWrapWithBottom(dp(12)));

        TextView description = makeText(
                "Studio receiver is still a placeholder. Sender H.264 dry-run comes first.",
                15,
                Color.rgb(87, 83, 78),
                false
        );
        description.setGravity(Gravity.CENTER);
        root.addView(description, fullWidthWrapWithBottom(dp(28)));

        TextView back = makeButton("Back", Color.rgb(39, 39, 42));
        back.setOnClickListener(v -> finish());
        root.addView(back, wrap());

        setContentView(root);
    }

    private void requestScreenCapturePermission() {
        if (encoderActive) {
            setStatus("Stop the active encoder before requesting again", Color.rgb(185, 28, 28));
            return;
        }

        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            setStatus("MediaProjectionManager unavailable", Color.rgb(185, 28, 28));
            return;
        }

        setStatus("Waiting for Android screen-capture permission", Color.rgb(124, 58, 237));
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE_PERMISSION);
    }

    private void startEncoderDryRun() {
        if (encoderActive) {
            setStatus("Encoder dry run already active", Color.rgb(21, 128, 61));
            return;
        }

        if (projectionPermissionData == null || projectionPermissionResultCode != Activity.RESULT_OK) {
            setStatus("Request screen-capture permission first", Color.rgb(185, 28, 28));
            return;
        }

        setStatus("Starting H.264 encoder dry run", Color.rgb(124, 58, 237));
        startKeepAliveService();
        waitForServiceThenCreateEncoder(0);
    }

    private void waitForServiceThenCreateEncoder(int attempt) {
        if (MediaProjectionKeepAliveService.isRunning() || attempt >= 12) {
            createEncoderDryRun();
            return;
        }

        mainHandler.postDelayed(() -> waitForServiceThenCreateEncoder(attempt + 1), 150);
    }

    private void createEncoderDryRun() {
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

            sourceWidth = Math.max(metrics.widthPixels, 1);
            sourceHeight = Math.max(metrics.heightPixels, 1);
            sourceDensity = metrics.densityDpi;
            int[] targetSize = chooseEncoderSize(sourceWidth, sourceHeight);
            encoderWidth = targetSize[0];
            encoderHeight = targetSize[1];

            MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) {
                setStatus("MediaProjectionManager unavailable", Color.rgb(185, 28, 28));
                stopKeepAliveService();
                return;
            }

            MediaFormat format = MediaFormat.createVideoFormat(AVC_MIME_TYPE, encoderWidth, encoderHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, TARGET_FPS);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS);

            videoEncoder = MediaCodec.createEncoderByType(AVC_MIME_TYPE);
            videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoderInputSurface = videoEncoder.createInputSurface();
            videoEncoder.start();

            mediaProjection = manager.getMediaProjection(projectionPermissionResultCode, projectionPermissionData);
            if (mediaProjection == null) {
                releaseEncoderSession(true, "Could not create MediaProjection. Request permission again.");
                return;
            }

            mediaProjection.registerCallback(projectionCallback, mainHandler);

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "MobileStudioV2H264EncoderDryRun",
                    encoderWidth,
                    encoderHeight,
                    sourceDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    encoderInputSurface,
                    null,
                    mainHandler
            );

            if (virtualDisplay == null) {
                releaseEncoderSession(true, "Could not create VirtualDisplay for encoder. Request permission again.");
                return;
            }

            encodedBytes = 0L;
            encodedOutputCount = 0L;
            keyFrameCount = 0L;
            codecConfigCount = 0L;
            outputFormatSummary = "waiting";
            encoderActive = true;
            drainRunning = true;
            encoderStartedAtMs = SystemClock.elapsedRealtime();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            startEncoderDrainThread();
            setStatus("H.264 encoder dry run active", Color.rgb(21, 128, 61));
            updateMetrics();
            mainHandler.removeCallbacks(metricsTicker);
            mainHandler.post(metricsTicker);
            updateButtons();
        } catch (Exception exception) {
            releaseEncoderSession(true, "Encoder failed: " + exception.getClass().getSimpleName());
        }
    }

    private int[] chooseEncoderSize(int width, int height) {
        int safeWidth = Math.max(width, 1);
        int safeHeight = Math.max(height, 1);

        int outputWidth;
        int outputHeight;
        if (safeHeight >= safeWidth) {
            outputWidth = TARGET_SHORT_SIDE;
            outputHeight = Math.round((float) safeHeight * outputWidth / safeWidth);
        } else {
            outputHeight = TARGET_SHORT_SIDE;
            outputWidth = Math.round((float) safeWidth * outputHeight / safeHeight);
        }

        outputWidth = clamp(alignTo16(outputWidth), 320, 1920);
        outputHeight = clamp(alignTo16(outputHeight), 320, 1920);

        if ((outputWidth & 1) != 0) {
            outputWidth++;
        }
        if ((outputHeight & 1) != 0) {
            outputHeight++;
        }

        return new int[]{outputWidth, outputHeight};
    }

    private int alignTo16(int value) {
        return Math.max(16, ((value + 15) / 16) * 16);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void startEncoderDrainThread() {
        encoderDrainThread = new Thread(() -> {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (drainRunning) {
                MediaCodec codec = videoEncoder;
                if (codec == null) {
                    break;
                }

                try {
                    int outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000);

                    if (outputIndex >= 0) {
                        ByteBuffer outputBuffer = codec.getOutputBuffer(outputIndex);
                        int size = Math.max(bufferInfo.size, 0);
                        if (outputBuffer != null && size > 0) {
                            encodedBytes += size;
                        }

                        encodedOutputCount++;
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                            keyFrameCount++;
                        }
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            codecConfigCount++;
                        }

                        codec.releaseOutputBuffer(outputIndex, false);
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = codec.getOutputFormat();
                        outputFormatSummary = summarizeFormat(newFormat);
                    }
                } catch (IllegalStateException exception) {
                    break;
                } catch (Exception exception) {
                    mainHandler.post(() -> setStatus("Encoder drain warning: " + exception.getClass().getSimpleName(), Color.rgb(185, 28, 28)));
                    break;
                }
            }
        }, "MobileStudioV2EncoderDrain");
        encoderDrainThread.start();
    }

    private String summarizeFormat(MediaFormat format) {
        if (format == null) {
            return "unknown";
        }
        int width = format.containsKey(MediaFormat.KEY_WIDTH) ? format.getInteger(MediaFormat.KEY_WIDTH) : encoderWidth;
        int height = format.containsKey(MediaFormat.KEY_HEIGHT) ? format.getInteger(MediaFormat.KEY_HEIGHT) : encoderHeight;
        return AVC_MIME_TYPE + " " + width + "x" + height;
    }

    private void releaseEncoderSession(boolean stopProjection, String message) {
        if (releasingSession) {
            return;
        }

        releasingSession = true;
        mainHandler.removeCallbacks(metricsTicker);
        drainRunning = false;

        Thread drainThread = encoderDrainThread;
        encoderDrainThread = null;
        if (drainThread != null && drainThread != Thread.currentThread()) {
            try {
                drainThread.join(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        if (virtualDisplay != null) {
            try {
                virtualDisplay.release();
            } catch (Exception ignored) {
            }
            virtualDisplay = null;
        }

        MediaProjection projectionToStop = mediaProjection;
        if (projectionToStop != null) {
            try {
                projectionToStop.unregisterCallback(projectionCallback);
            } catch (Exception ignored) {
            }
            mediaProjection = null;
            if (stopProjection) {
                try {
                    projectionToStop.stop();
                } catch (Exception ignored) {
                }
            }
        }

        if (encoderInputSurface != null) {
            try {
                encoderInputSurface.release();
            } catch (Exception ignored) {
            }
            encoderInputSurface = null;
        }

        if (videoEncoder != null) {
            try {
                videoEncoder.stop();
            } catch (Exception ignored) {
            }
            try {
                videoEncoder.release();
            } catch (Exception ignored) {
            }
            videoEncoder = null;
        }

        encoderActive = false;
        encoderStartedAtMs = 0L;
        sourceWidth = 0;
        sourceHeight = 0;
        sourceDensity = 0;
        encoderWidth = 0;
        encoderHeight = 0;
        projectionPermissionData = null;
        projectionPermissionResultCode = Activity.RESULT_CANCELED;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopKeepAliveService();

        if (message != null) {
            setStatus(message, Color.rgb(68, 64, 60));
        }

        updateMetrics();
        updateButtons();
        releasingSession = false;
    }

    private void startKeepAliveService() {
        Intent intent = MediaProjectionKeepAliveService.startIntent(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopKeepAliveService() {
        try {
            startService(MediaProjectionKeepAliveService.stopIntent(this));
        } catch (Exception ignored) {
        }
    }

    private void setStatus(String status, int color) {
        if (statusView == null) {
            return;
        }
        statusView.setText("Status: " + status);
        statusView.setTextColor(color);
    }

    private void updateMetrics() {
        if (metricsView == null) {
            return;
        }

        String orientation = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                ? "landscape"
                : "portrait";

        String source = encoderActive
                ? sourceWidth + "x" + sourceHeight + " @ " + sourceDensity + " dpi"
                : "not active";

        String encoderSize = encoderActive
                ? encoderWidth + "x" + encoderHeight
                : "target pending";

        String uptime = encoderActive && encoderStartedAtMs > 0L
                ? formatUptime(SystemClock.elapsedRealtime() - encoderStartedAtMs)
                : "0s";

        String keepAwake = encoderActive ? "on" : "off";
        String rate = encoderActive
                ? formatBytes(encodedBytes) + " encoded | outputs " + encodedOutputCount
                : "inactive";

        metricsView.setText(
                "Encoder metrics\n"
                        + "Source screen: " + source + "\n"
                        + "Encoder: H.264 " + encoderSize + " @ " + TARGET_FPS + " fps, " + formatBitrate(TARGET_BITRATE) + "\n"
                        + "Encoded: " + rate + "\n"
                        + "Key frames: " + keyFrameCount + " | Config buffers: " + codecConfigCount + "\n"
                        + "Format: " + outputFormatSummary + "\n"
                        + "Orientation: " + orientation + " | Uptime: " + uptime + "\n"
                        + "Keep screen on: " + keepAwake + " | Network: off | File: off"
        );
    }

    private String formatUptime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        }
        if (bytes >= 1024L) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private String formatBitrate(int bitrate) {
        return String.format(Locale.US, "%.1f Mbps", bitrate / 1_000_000.0);
    }

    private void updateButtons() {
        boolean hasPermission = projectionPermissionData != null && projectionPermissionResultCode == Activity.RESULT_OK;

        if (requestButton != null) {
            requestButton.setText(hasPermission ? "Request Again" : "Request Screen Capture Permission");
            requestButton.setEnabled(!encoderActive);
            requestButton.setAlpha(encoderActive ? 0.55f : 1.0f);
        }

        if (startButton != null) {
            startButton.setEnabled(hasPermission && !encoderActive);
            startButton.setAlpha((hasPermission && !encoderActive) ? 1.0f : 0.45f);
        }

        if (stopButton != null) {
            stopButton.setEnabled(encoderActive);
            stopButton.setAlpha(encoderActive ? 1.0f : 0.45f);
        }
    }

    private TextView makeBadge(String text) {
        TextView badge = makeText(text, 14, Color.rgb(124, 58, 237), true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(14), dp(8), dp(14), dp(8));
        badge.setBackground(makeRoundedBackground(Color.rgb(237, 233, 254), dp(18)));
        return badge;
    }

    private TextView makeText(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private TextView makeButton(String text, int color) {
        TextView button = makeText(text, 16, Color.WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(18), dp(14), dp(18), dp(14));
        button.setBackground(makeRoundedBackground(color, dp(18)));
        return button;
    }

    private GradientDrawable makePanelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.rgb(250, 250, 250));
        drawable.setCornerRadius(dp(20));
        drawable.setStroke(dp(2), Color.rgb(221, 214, 254));
        return drawable;
    }

    private GradientDrawable makeRoundedBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidthWrapWithBottom(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWithBottom(int bottomMargin) {
        LinearLayout.LayoutParams params = wrap();
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
