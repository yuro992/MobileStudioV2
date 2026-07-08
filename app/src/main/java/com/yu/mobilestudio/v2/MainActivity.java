package com.yu.mobilestudio.v2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static final String EXTRA_MODE = "com.yu.mobilestudio.v2.EXTRA_MODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("MobileStudioV2");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(40), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(250, 250, 255));

        TextView title = new TextView(this);
        title.setText("MobileStudioV2");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(28, 25, 23));
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidthWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Two-phone mobile live studio");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(87, 83, 78));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(8), 0, dp(16));
        root.addView(subtitle, fullWidthWrap());

        TextView phase = new TextView(this);
        phase.setText("Phase 1: UI skeleton only");
        phase.setTextSize(14);
        phase.setTextColor(Color.rgb(124, 58, 237));
        phase.setTypeface(Typeface.DEFAULT_BOLD);
        phase.setGravity(Gravity.CENTER);
        phase.setPadding(dp(12), dp(10), dp(12), dp(10));
        phase.setBackground(makeRoundedBackground(Color.rgb(237, 233, 254), dp(16)));
        root.addView(phase, fullWidthWrapWithBottom(dp(28)));

        View senderCard = makeModeCard(
                "Sender Mode",
                "Game phone: screen sender placeholder",
                "Sender"
        );
        root.addView(senderCard, fullWidthHeightWithBottom(dp(118), dp(14)));

        View studioCard = makeModeCard(
                "Studio Mode",
                "Stream phone: studio controller placeholder",
                "Studio"
        );
        root.addView(studioCard, fullWidthHeightWithBottom(dp(118), dp(20)));

        TextView footer = new TextView(this);
        footer.setText("Next phase will add Android screen-capture permission flow.");
        footer.setTextSize(13);
        footer.setTextColor(Color.rgb(120, 113, 108));
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, fullWidthWrap());

        setContentView(root);
    }

    private View makeModeCard(String title, String description, String mode) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        card.setBackground(makeCardBackground());

        TextView cardTitle = new TextView(this);
        cardTitle.setText(title);
        cardTitle.setTextSize(22);
        cardTitle.setTypeface(Typeface.DEFAULT_BOLD);
        cardTitle.setTextColor(Color.rgb(39, 39, 42));
        card.addView(cardTitle, fullWidthWrap());

        TextView cardDesc = new TextView(this);
        cardDesc.setText(description);
        cardDesc.setTextSize(14);
        cardDesc.setTextColor(Color.rgb(82, 82, 91));
        cardDesc.setPadding(0, dp(8), 0, 0);
        card.addView(cardDesc, fullWidthWrap());

        card.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ModeActivity.class);
            intent.putExtra(EXTRA_MODE, mode);
            startActivity(intent);
        });

        return card;
    }

    private GradientDrawable makeCardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), Color.rgb(221, 214, 254));
        return drawable;
    }

    private GradientDrawable makeRoundedBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidthWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams fullWidthWrapWithBottom(int bottomMargin) {
        LinearLayout.LayoutParams params = fullWidthWrap();
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
