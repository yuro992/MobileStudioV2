package com.yu.mobilestudio.v2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ModeActivity extends Activity {

    private static final int REQUEST_CAPTURE_PERMISSION = 2402;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String mode;
    private TextView statusView;
    private TextView requestButton;
    private TextView startButton;
    private TextView stopButton;
    private SurfaceView previewSurfaceView;

    private Intent projectionPermissionData;
    private int projectionPermissionResultCode = Activity.RESULT_CANCELED;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private boolean surfaceReady = false;
    private boolean captureActive = false;
    private boolean releasingSession = false;

    private final MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            mainHandler.post(() -> releaseCaptureSession(false, "Capture preview stopped by Android. Request permission again."));
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
        releaseCaptureSession(true, null);
        super.onDestroy();
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
            setStatus("Permission granted. Ready to start preview", Color.rgb(21, 128, 61));
        } else {
            projectionPermissionResultCode = Activity.RESULT_CANCELED;
            projectionPermissionData = null;
            setStatus("Screen capture permission denied", Color.rgb(185, 28, 28));
        }

        updateButtons();
    }

    private void buildSenderScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(250, 250, 255));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView badge = makeBadge("Phase 4");
        root.addView(badge, wrapWithBottom(dp(18)));

        TextView title = makeText("Sender Mode Ready", 28, Color.rgb(28, 25, 23), true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidthWrapWithBottom(dp(12)));

        TextView description = makeText(
                "Request permission, then start a local screen preview. This phase does not encode, send, or record video yet.",
                15,
                Color.rgb(87, 83, 78),
                false
        );
        description.setGravity(Gravity.CENTER);
        root.addView(description, fullWidthWrapWithBottom(dp(18)));

        FrameLayout previewFrame = new FrameLayout(this);
        previewFrame.setPadding(dp(2), dp(2), dp(2), dp(2));
        previewFrame.setBackground(makePreviewBackground());

        previewSurfaceView = new SurfaceView(this);
        previewSurfaceView.setZOrderOnTop(false);
        previewSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surfaceReady = true;
                updateButtons();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                surfaceReady = true;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceReady = false;
                if (captureActive) {
                    releaseCaptureSession(true, "Preview surface was destroyed. Request permission again.");
                }
                updateButtons();
            }
        });

        previewFrame.addView(previewSurfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        root.addView(previewFrame, fullWidthHeightWithBottom(dp(250), dp(16)));

        statusView = makeText("Status: Not requested", 15, Color.rgb(68, 64, 60), true);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(12), dp(12), dp(12), dp(12));
        statusView.setBackground(makeRoundedBackground(Color.WHITE, dp(18)));
        root.addView(statusView, fullWidthWrapWithBottom(dp(16)));

        requestButton = makeButton("Request Screen Capture Permission", Color.rgb(124, 58, 237));
        requestButton.setOnClickListener(v -> requestScreenCapturePermission());
        root.addView(requestButton, fullWidthWrapWithBottom(dp(10)));

        startButton = makeButton("Start Local Preview", Color.rgb(21, 128, 61));
        startButton.setOnClickListener(v -> startLocalPreview());
        root.addView(startButton, fullWidthWrapWithBottom(dp(10)));

        stopButton = makeButton("Stop Local Preview", Color.rgb(185, 28, 28));
        stopButton.setOnClickListener(v -> releaseCaptureSession(true, "Capture preview stopped. Request permission again."));
        root.addView(stopButton, fullWidthWrapWithBottom(dp(14)));

        TextView back = makeButton("Back", Color.rgb(39, 39, 42));
        back.setOnClickListener(v -> finish());
        root.addView(back, wrap());

        updateButtons();
        setContentView(scrollView);
    }

    private void buildStudioPlaceholderScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(250, 250, 255));

        root.addView(makeBadge("Phase 4"), wrapWithBottom(dp(22)));

        TextView title = makeText("Studio Mode Ready", 28, Color.rgb(28, 25, 23), true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidthWrapWithBottom(dp(12)));

        TextView description = makeText(
                "Studio receiver is still a placeholder. Sender local preview comes first.",
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
        if (captureActive) {
            setStatus("Stop the active preview before requesting again", Color.rgb(185, 28, 28));
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

    private void startLocalPreview() {
        if (captureActive) {
            setStatus("Capture preview already active", Color.rgb(21, 128, 61));
            return;
        }

        if (projectionPermissionData == null || projectionPermissionResultCode != Activity.RESULT_OK) {
            setStatus("Request screen-capture permission first", Color.rgb(185, 28, 28));
            requestScreenCapturePermission();
            return;
        }

        if (!surfaceReady || previewSurfaceView == null || !previewSurfaceView.getHolder().getSurface().isValid()) {
            setStatus("Preview surface not ready yet. Try again in a moment.", Color.rgb(185, 28, 28));
            return;
        }

        setStatus("Starting foreground preview service", Color.rgb(124, 58, 237));
        startKeepAliveService();
        waitForServiceThenCreatePreview(0);
    }

    private void waitForServiceThenCreatePreview(int attempt) {
        if (MediaProjectionKeepAliveService.isRunning() || attempt >= 12) {
            createVirtualDisplayPreview();
            return;
        }

        mainHandler.postDelayed(() -> waitForServiceThenCreatePreview(attempt + 1), 150);
    }

    private void createVirtualDisplayPreview() {
        try {
            MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) {
                setStatus("MediaProjectionManager unavailable", Color.rgb(185, 28, 28));
                stopKeepAliveService();
                return;
            }

            mediaProjection = manager.getMediaProjection(projectionPermissionResultCode, projectionPermissionData);
            if (mediaProjection == null) {
                setStatus("Could not create MediaProjection. Request permission again.", Color.rgb(185, 28, 28));
                stopKeepAliveService();
                return;
            }

            mediaProjection.registerCallback(projectionCallback, mainHandler);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

            int width = Math.max(metrics.widthPixels, 1);
            int height = Math.max(metrics.heightPixels, 1);
            int density = metrics.densityDpi;

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "MobileStudioV2LocalPreview",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    previewSurfaceView.getHolder().getSurface(),
                    null,
                    mainHandler
            );

            if (virtualDisplay == null) {
                releaseCaptureSession(true, "Could not create VirtualDisplay. Request permission again.");
                return;
            }

            captureActive = true;
            setStatus("Capture preview active", Color.rgb(21, 128, 61));
            updateButtons();
        } catch (Exception exception) {
            releaseCaptureSession(true, "Preview failed: " + exception.getClass().getSimpleName());
        }
    }

    private void releaseCaptureSession(boolean stopProjection, String message) {
        if (releasingSession) {
            return;
        }

        releasingSession = true;

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

        captureActive = false;
        projectionPermissionData = null;
        projectionPermissionResultCode = Activity.RESULT_CANCELED;
        stopKeepAliveService();

        if (message != null) {
            setStatus(message, Color.rgb(68, 64, 60));
        }

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

    private void updateButtons() {
        boolean hasPermission = projectionPermissionData != null && projectionPermissionResultCode == Activity.RESULT_OK;

        if (requestButton != null) {
            requestButton.setText(hasPermission ? "Request Again" : "Request Screen Capture Permission");
            requestButton.setEnabled(!captureActive);
            requestButton.setAlpha(captureActive ? 0.55f : 1.0f);
        }

        if (startButton != null) {
            startButton.setEnabled(hasPermission && surfaceReady && !captureActive);
            startButton.setAlpha((hasPermission && surfaceReady && !captureActive) ? 1.0f : 0.45f);
        }

        if (stopButton != null) {
            stopButton.setEnabled(captureActive);
            stopButton.setAlpha(captureActive ? 1.0f : 0.45f);
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

    private GradientDrawable makePreviewBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.rgb(24, 24, 27));
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

    private LinearLayout.LayoutParams fullWidthHeightWithBottom(int height, int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
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
