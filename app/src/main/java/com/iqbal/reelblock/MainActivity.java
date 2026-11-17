package com.iqbal.reelblock;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.Toast;

public class MainActivity extends Activity {

    static final String FAMILY_DNS = "family.adguard-dns.com";

    private TextView status;
    private TextView stats;
    private Button openSettings;
    private final java.util.List<Switch> toggles = new java.util.ArrayList<>();
    private TextView lockStatus;
    private Button lockBtn;
    private Button lockNowBtn;
    private Button adminBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = dp(20);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, dp(48), pad, dp(40));

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
        openSettings.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        box.addView(openSettings);

        box.addView(heading("Short videos"));
        box.addView(toggle("Block Instagram Reels", Prefs.IG_ON));
        box.addView(toggle("Block YouTube Shorts", Prefs.YT_ON));
        box.addView(toggle("Block Facebook Reels", Prefs.FB_ON));

        box.addView(heading("Adult content"));
        box.addView(toggle("Block adult content everywhere", Prefs.ADULT_ON));
        box.addView(note("Works in Chrome and other browsers, Instagram, Facebook, YouTube "
                + "and other social media apps. Other apps (Claude, Quran apps, WhatsApp, etc.) "
                + "are never blocked."));

        box.addView(heading("Extra protection (recommended)"));
        box.addView(note("Blocks adult websites for the whole phone, even in apps this blocker "
                + "can't read. Free, by AdGuard Family DNS.\n\n"
                + "1. Tap the button below (the address gets copied).\n"
                + "2. Find \"Private DNS\" (search it in Settings if needed. Samsung: "
                + "Connections → More connection settings → Private DNS).\n"
                + "3. Choose \"Private DNS provider hostname\", paste, tap Save."));
        Button dns = new Button(this);
        dns.setText("Copy address & open Settings");
        dns.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("dns", FAMILY_DNS));
            Toast.makeText(this, "Copied: " + FAMILY_DNS, Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        box.addView(dns);
        TextView addr = note("Address: " + FAMILY_DNS);
        addr.setTextIsSelectable(true);
        box.addView(addr);

        box.addView(heading("Uninstall protection"));
        lockStatus = note("");
        box.addView(lockStatus);
        lockBtn = new Button(this);
        lockBtn.setOnClickListener(v -> onLockButton());
        box.addView(lockBtn);
        lockNowBtn = new Button(this);
        lockNowBtn.setText("Lock again now");
        lockNowBtn.setOnClickListener(v -> { Prefs.lockNow(this); onResume(); });
        box.addView(lockNowBtn);
        adminBtn = new Button(this);
        adminBtn.setOnClickListener(v -> onAdminButton());
        box.addView(adminBtn);
        box.addView(note("Uninstall lock: Android won't let the app be uninstalled until this "
                + "is switched off, and switching it off needs your password."));

        stats = new TextView(this);
        stats.setTextSize(16);
        stats.setPadding(0, dp(28), 0, 0);
        box.addView(stats);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
    }

    private TextView heading(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(20);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(28), 0, dp(4));
        return t;
    }

    private TextView note(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.DKGRAY);
        t.setTextSize(14);
        t.setPadding(0, dp(4), 0, dp(8));
        return t;
    }

    private Switch toggle(String label, String key) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextSize(17);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setPadding(0, dp(12), 0, dp(12));
        s.setChecked(Prefs.get(this).getBoolean(key, true));
        s.setOnCheckedChangeListener((v, on) ->
                Prefs.get(this).edit().putBoolean(key, on).apply());
        toggles.add(s);
        return s;
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isServiceOn();
        status.setText(on ? "✅ Blocker is ON"
                : "❌ Blocker is OFF — tap below and enable \"Reel Blocker\"");
        status.setTextColor(on ? Color.rgb(0, 140, 60) : Color.rgb(200, 40, 40));
        openSettings.setText(on ? "Accessibility settings" : "Turn on in Accessibility settings");
        boolean isLocked = locked();
        for (Switch t : toggles) {
            t.setEnabled(!isLocked);
            t.setAlpha(isLocked ? 0.5f : 1f);
        }
        if (!Prefs.isProtected(this)) {
            lockStatus.setText("Protection is OFF. Anyone can uninstall or switch off the app.");
            lockBtn.setText("Set password & turn on protection");
            lockNowBtn.setVisibility(android.view.View.GONE);
        } else if (isLocked) {
            lockStatus.setText("🔒 Protection is ON. Uninstalling, stopping or changing settings "
                    + "needs the password.");
            lockBtn.setText("Unlock with password");
            lockNowBtn.setVisibility(android.view.View.GONE);
        } else {
            lockStatus.setText("🔓 Unlocked for a few minutes. You can change settings now. "
                    + "Tap \"Lock again now\" when you're done.");
            lockBtn.setText("Remove protection completely");
            lockNowBtn.setVisibility(android.view.View.VISIBLE);
        }
        adminBtn.setText(adminOn() ? "Uninstall lock: ON (tap to switch off)"
                : "Turn on uninstall lock");

        stats.setText("Reels/Shorts blocked today: " + Prefs.today(this)
                + "  (all time: " + Prefs.get(this).getInt(Prefs.TOTAL, 0) + ")"
                + "\nAdult content blocked today: " + Prefs.adultToday(this)
                + "  (all time: " + Prefs.get(this).getInt(Prefs.ADULT_TOTAL, 0) + ")");
    }

    private boolean locked() {
        return Prefs.isProtected(this) && !Prefs.isUnlocked(this);
    }

    private void openPassword(int mode) {
        Intent i = new Intent(this, PasswordActivity.class);
        i.putExtra(PasswordActivity.EXTRA_MODE, mode);
        startActivity(i);
    }

    private void onLockButton() {
        if (!Prefs.isProtected(this)) {
            openPassword(PasswordActivity.MODE_SETUP);
        } else if (locked()) {
            openPassword(PasswordActivity.MODE_UNLOCK);
        } else {
            Prefs.removeProtection(this);
            Toast.makeText(this, "Protection removed", Toast.LENGTH_SHORT).show();
            onResume();
        }
    }

    private ComponentName admin() {
        return new ComponentName(this, AdminReceiver.class);
    }

    private boolean adminOn() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(admin());
    }

    private void onAdminButton() {
        if (!adminOn() && locked()) {
            Toast.makeText(this, "Unlock with your password first", Toast.LENGTH_SHORT).show();
            openPassword(PasswordActivity.MODE_UNLOCK);
        } else if (!adminOn()) {
            Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin());
            i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Stops Reel Blocker from being uninstalled quickly.");
            startActivity(i);
        } else if (locked()) {
            openPassword(PasswordActivity.MODE_UNLOCK);
        } else {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            if (dpm != null) dpm.removeActiveAdmin(admin());
            Toast.makeText(this, "Uninstall lock switched off", Toast.LENGTH_SHORT).show();
            lockBtn.postDelayed(this::onResume, 500);
        }
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
