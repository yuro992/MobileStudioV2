package com.yu.mobilestudio.v2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ModeActivity extends Activity {

    private static final int REQUEST_SCREEN_CAPTURE = 2003;

    private String mode;
    private TextView statusText;
    private TextView requestButton;
    private TextView startButton;
    private TextView stopButton;
    private int projectionResultCode = 0;
    private Intent projectionResultData;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver captureStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !CaptureService.ACTION_STATUS.equals(intent.getAction())) {
                return;
            }

            String status = intent.getStringExtra(CaptureService.EXTRA_STATUS);
            boolean success = intent.getBooleanExtra(CaptureService.EXTRA_SUCCESS, false);
            if (status == null || status.trim().isEmpty()) {
                return;
            }

            setStatus("Status: " + status, success);
            if (!success && (status.contains("stopped") || status.contains("failed"))) {
                clearProjectionPermissionForNextSession();
            }
            updateSessionButtons(success);
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

        boolean isSender = "Sender".equalsIgnoreCase(mode);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(250, 250, 255));

        TextView badge = new TextView(this);
        badge.setText("Phase 3");
        badge.setTextSize(14);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.rgb(124, 58, 237));
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(14), dp(8), dp(14), dp(8));
        badge.setBackground(makeRoundedBackground(Color.rgb(237, 233, 254), dp(18)));
        root.addView(badge, wrapWithBottom(dp(22)));

        TextView title = new TextView(this);
        title.setText(mode + " Mode Ready");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(28, 25, 23));
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidthWrapWithBottom(dp(12)));

        TextView description = new TextView(this);
        if (isSender) {
            description.setText("Request permission, then start a foreground capture session. This phase does not preview, encode, stream, or record yet.");
        } else {
            description.setText("Studio Mode is still a placeholder in Phase 3. Real receiving and layout tools come later.");
        }
        description.setTextSize(15);
        description.setTextColor(Color.rgb(87, 83, 78));
        description.setGravity(Gravity.CENTER);
        root.addView(description, fullWidthWrapWithBottom(dp(22)));

        statusText = new TextView(this);
        statusText.setText(isSender ? "Status: Not requested" : "Status: Studio placeholder only");
        statusText.setTextSize(15);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusText.setTextColor(Color.rgb(68, 64, 60));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(14), dp(10), dp(14), dp(10));
        statusText.setBackground(makeRoundedBackground(Color.WHITE, dp(18)));
        root.addView(statusText, fullWidthWrapWithBottom(dp(18)));

        if (isSender) {
            requestButton = makeButton("Request Screen Capture Permission", Color.rgb(124, 58, 237), Color.WHITE);
            requestButton.setOnClickListener(v -> requestScreenCapturePermission());
            root.addView(requestButton, fullWidthWrapWithBottom(dp(12)));

            startButton = makeButton("Start Capture Session", Color.rgb(22, 101, 52), Color.WHITE);
            startButton.setOnClickListener(v -> startCaptureSession());
            root.addView(startButton, fullWidthWrapWithBottom(dp(12)));

            stopButton = makeButton("Stop Capture Session", Color.rgb(185, 28, 28), Color.WHITE);
            stopButton.setOnClickListener(v -> stopCaptureSession());
            root.addView(stopButton, fullWidthWrapWithBottom(dp(18)));

            updatePermissionButtons(false);
            updateSessionButtons(false);
        }

        TextView back = makeButton("Back", Color.rgb(39, 39, 42), Color.WHITE);
        back.setOnClickListener(v -> finish());
        root.addView(back, wrap());

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerCaptureStatusReceiver();
    }

    @Override
    protected void onPause() {
        unregisterCaptureStatusReceiver();
        super.onPause();
    }

    private void requestScreenCapturePermission() {
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (projectionManager == null) {
            setStatus("Status: Screen-capture service unavailable", false);
            return;
        }

        setStatus("Status: Waiting for Android permission dialog...", false);
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_SCREEN_CAPTURE) {
            return;
        }

        if (resultCode == RESULT_OK && data != null) {
            projectionResultCode = resultCode;
            projectionResultData = data;
            setStatus("Status: Permission granted. Ready to start session", true);
            if (requestButton != null) {
                requestButton.setText("Request Again");
            }
            updatePermissionButtons(true);
            updateSessionButtons(false);
        } else {
            projectionResultCode = 0;
            projectionResultData = null;
            setStatus("Status: Permission denied or cancelled", false);
            updatePermissionButtons(false);
            updateSessionButtons(false);
        }
    }

    private void startCaptureSession() {
        if (projectionResultCode == 0 || projectionResultData == null) {
            setStatus("Status: Request permission before starting session", false);
            updatePermissionButtons(false);
            updateSessionButtons(false);
            return;
        }

        setStatus("Status: Capture session starting...", false);

        Intent intent = new Intent(this, CaptureService.class);
        intent.setAction(CaptureService.ACTION_START);
        intent.putExtra(CaptureService.EXTRA_RESULT_CODE, projectionResultCode);
        intent.putExtra(CaptureService.EXTRA_RESULT_DATA, projectionResultData);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopCaptureSession() {
        Intent intent = new Intent(this, CaptureService.class);
        intent.setAction(CaptureService.ACTION_STOP);
        startService(intent);
        clearProjectionPermissionForNextSession();
        setStatus("Status: Capture session stopping...", false);
        updateSessionButtons(false);
    }

    private void registerCaptureStatusReceiver() {
        if (receiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter(CaptureService.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(captureStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(captureStatusReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void unregisterCaptureStatusReceiver() {
        if (!receiverRegistered) {
            return;
        }

        try {
            unregisterReceiver(captureStatusReceiver);
        } finally {
            receiverRegistered = false;
        }
    }

    private void setStatus(String value, boolean success) {
        if (statusText == null) {
            return;
        }
        statusText.setText(value);
        statusText.setTextColor(success ? Color.rgb(22, 101, 52) : Color.rgb(68, 64, 60));
    }

    private void clearProjectionPermissionForNextSession() {
        projectionResultCode = 0;
        projectionResultData = null;
        if (requestButton != null) {
            requestButton.setText("Request Again");
        }
    }

    private void updatePermissionButtons(boolean hasPermission) {
        if (startButton == null) {
            return;
        }

        startButton.setEnabled(hasPermission);
        startButton.setAlpha(hasPermission ? 1.0f : 0.42f);
    }

    private void updateSessionButtons(boolean sessionActive) {
        if (startButton != null) {
            startButton.setEnabled(!sessionActive && projectionResultCode != 0 && projectionResultData != null);
            startButton.setAlpha(startButton.isEnabled() ? 1.0f : 0.42f);
        }

        if (stopButton != null) {
            stopButton.setEnabled(sessionActive);
            stopButton.setAlpha(sessionActive ? 1.0f : 0.42f);
        }
    }

    private TextView makeButton(String text, int backgroundColor, int textColor) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(18), dp(12), dp(18), dp(12));
        button.setBackground(makeRoundedBackground(backgroundColor, dp(18)));
        return button;
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
