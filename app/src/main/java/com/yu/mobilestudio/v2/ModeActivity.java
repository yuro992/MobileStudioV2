package com.yu.mobilestudio.v2;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ModeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String mode = getIntent().getStringExtra(MainActivity.EXTRA_MODE);
        if (mode == null || mode.trim().isEmpty()) {
            mode = "Unknown";
        }

        setTitle(mode + " Mode");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(250, 250, 255));

        TextView badge = new TextView(this);
        badge.setText("Phase 1");
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
        description.setText("This is a placeholder screen. Real streaming features are intentionally disabled in Phase 1.");
        description.setTextSize(15);
        description.setTextColor(Color.rgb(87, 83, 78));
        description.setGravity(Gravity.CENTER);
        root.addView(description, fullWidthWrapWithBottom(dp(28)));

        TextView back = new TextView(this);
        back.setText("Back");
        back.setTextSize(16);
        back.setTypeface(Typeface.DEFAULT_BOLD);
        back.setTextColor(Color.WHITE);
        back.setGravity(Gravity.CENTER);
        back.setPadding(dp(20), dp(12), dp(20), dp(12));
        back.setBackground(makeRoundedBackground(Color.rgb(124, 58, 237), dp(18)));
        back.setOnClickListener(v -> finish());
        root.addView(back, wrap());

        setContentView(root);
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
