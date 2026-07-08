package com.yu.mobilestudio.v2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ModeActivity extends Activity {

    private static final int REQUEST_SCREEN_CAPTURE = 2002;

    private String mode;
    private TextView statusText;
    private TextView primaryButton;

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
        badge.setText("Phase 2");
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
            description.setText("Request Android screen-capture permission. This phase does not preview, encode, stream, or record yet.");
        } else {
            description.setText("Studio Mode is still a placeholder in Phase 2. Real receiving and layout tools come later.");
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
        root.addView(statusText, fullWidthWrapWithBottom(dp(24)));

        if (isSender) {
            primaryButton = makeButton("Request Screen Capture Permission", Color.rgb(124, 58, 237), Color.WHITE);
            primaryButton.setOnClickListener(v -> requestScreenCapturePermission());
            root.addView(primaryButton, fullWidthWrapWithBottom(dp(14)));
        }

        TextView back = makeButton("Back", Color.rgb(39, 39, 42), Color.WHITE);
        back.setOnClickListener(v -> finish());
        root.addView(back, wrap());

        setContentView(root);
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
            setStatus("Status: Screen capture permission granted", true);
            if (primaryButton != null) {
                primaryButton.setText("Request Again");
            }
        } else {
            setStatus("Status: Permission denied or cancelled", false);
        }
    }

    private void setStatus(String value, boolean success) {
        if (statusText == null) {
            return;
        }
        statusText.setText(value);
        statusText.setTextColor(success ? Color.rgb(22, 101, 52) : Color.rgb(68, 64, 60));
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
