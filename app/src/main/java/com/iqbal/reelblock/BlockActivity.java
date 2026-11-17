package com.iqbal.reelblock;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Full-screen 5-second reminder shown after something is blocked. */
public class BlockActivity extends Activity {

    static final String EXTRA_ADULT = "adult";
    private static final int SECONDS = 5;

    private static final int GOLD = Color.rgb(212, 175, 95);
    private static final int CREAM = Color.rgb(245, 238, 220);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean adult;
    private boolean done;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        adult = getIntent().getBooleanExtra(EXTRA_ADULT, false);
        String[] v = Verses.pick(adult);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.setStatusBarColor(Color.rgb(6, 40, 34));
        w.setNavigationBarColor(Color.rgb(3, 22, 19));

        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(6, 40, 34), Color.rgb(8, 58, 48), Color.rgb(3, 22, 19)});
        root.setBackground(bg);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(28), dp(24), dp(28), dp(24));

        // Small top label
        TextView label = new TextView(this);
        label.setText(adult ? "PROTECT YOUR GAZE" : "YOUR TIME IS PRECIOUS");
        label.setTextColor(GOLD);
        label.setTextSize(13);
        label.setLetterSpacing(0.25f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.CENTER);
        col.addView(label);

        // Ornament
        TextView orn = new TextView(this);
        orn.setText("۞");
        orn.setTextColor(GOLD);
        orn.setTextSize(34);
        orn.setGravity(Gravity.CENTER);
        orn.setPadding(0, dp(18), 0, dp(10));
        col.addView(orn);

        // Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(22), dp(26), dp(22), dp(24));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.argb(40, 255, 255, 255));
        cardBg.setCornerRadius(dp(24));
        cardBg.setStroke(dp(1), Color.argb(90, 212, 175, 95));
        card.setBackground(cardBg);

        if (!v[0].isEmpty()) {
            TextView ar = new TextView(this);
            ar.setText(v[0]);
            ar.setTextColor(Color.WHITE);
            ar.setTextSize(25);
            ar.setLineSpacing(dp(8), 1f);
            ar.setGravity(Gravity.CENTER);
            ar.setTextDirection(View.TEXT_DIRECTION_RTL);
            card.addView(ar);

            View line = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(60), dp(2));
            lp.topMargin = dp(18);
            lp.bottomMargin = dp(18);
            line.setLayoutParams(lp);
            line.setBackgroundColor(GOLD);
            card.addView(line);
        }

        TextView en = new TextView(this);
        en.setText("“" + v[1] + "”");
        en.setTextColor(CREAM);
        en.setTextSize(18);
        en.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
        en.setLineSpacing(dp(4), 1f);
        en.setGravity(Gravity.CENTER);
        card.addView(en);

        TextView ref = new TextView(this);
        ref.setText("— " + v[2]);
        ref.setTextColor(GOLD);
        ref.setTextSize(14);
        ref.setTypeface(Typeface.DEFAULT_BOLD);
        ref.setGravity(Gravity.CENTER);
        ref.setPadding(0, dp(16), 0, 0);
        card.addView(ref);

        col.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView sub = new TextView(this);
        sub.setText(adult ? "This content was blocked. Choose what pleases Allah."
                : "Reels blocked. Use this moment for something better.");
        sub.setTextColor(Color.argb(190, 245, 238, 220));
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(22), 0, 0);
        col.addView(sub);

        FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(col, colLp);

        // Countdown bar at the bottom
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setProgress(1000);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        bar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.argb(60, 255, 255, 255)));
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(6), Gravity.BOTTOM);
        barLp.setMargins(dp(40), 0, dp(40), dp(48));
        root.addView(bar, barLp);

        setContentView(root);

        // Fade in
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(350).start();

        ObjectAnimator anim = ObjectAnimator.ofInt(bar, "progress", 1000, 0);
        anim.setDuration(SECONDS * 1000L);
        anim.setInterpolator(new LinearInterpolator());
        anim.start();

        handler.postDelayed(this::close, SECONDS * 1000L);
    }

    private void close() {
        if (done) return;
        done = true;
        if (adult) {
            // Leave the app that showed adult content.
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        }
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Stay for the full 5 seconds.
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
