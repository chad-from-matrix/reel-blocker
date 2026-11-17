package com.iqbal.reelblock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;
    private TextView stats;
    private Button openSettings;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = dp(20);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, dp(48), pad, pad);

        TextView title = new TextView(this);
        title.setText("Reel Blocker");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, dp(16), 0, dp(8));
        box.addView(status);

        openSettings = new Button(this);
        openSettings.setText("Turn on in Accessibility settings");
        openSettings.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        box.addView(openSettings);

        box.addView(toggle("Block Instagram Reels", Prefs.IG_ON));
        box.addView(toggle("Block YouTube Shorts", Prefs.YT_ON));

        stats = new TextView(this);
        stats.setTextSize(16);
        stats.setPadding(0, dp(24), 0, 0);
        box.addView(stats);

        TextView note = new TextView(this);
        note.setText("Instagram feed, stories, DMs and normal YouTube videos keep working. "
                + "Only the Reels and Shorts players are blocked.");
        note.setTextColor(Color.GRAY);
        note.setPadding(0, dp(24), 0, 0);
        box.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
    }

    private Switch toggle(String label, String key) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextSize(18);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setPadding(0, dp(16), 0, dp(16));
        s.setChecked(Prefs.get(this).getBoolean(key, true));
        s.setOnCheckedChangeListener((v, on) ->
                Prefs.get(this).edit().putBoolean(key, on).apply());
        return s;
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isServiceOn();
        status.setText(on ? "✅ Blocker is ON" : "❌ Blocker is OFF — tap below and enable \"Reel Blocker\"");
        status.setTextColor(on ? Color.rgb(0, 140, 60) : Color.rgb(200, 40, 40));
        openSettings.setText(on ? "Accessibility settings" : "Turn on in Accessibility settings");
        stats.setText("Blocked today: " + Prefs.today(this)
                + "\nBlocked all time: " + Prefs.get(this).getInt(Prefs.TOTAL, 0));
    }

    private boolean isServiceOn() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(getPackageName() + "/");
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
