package com.iqbal.reelblock;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Set the protection password, or enter it to unlock changes for 10 minutes. */
public class PasswordActivity extends Activity {

    static final String EXTRA_MODE = "mode";
    static final int MODE_UNLOCK = 0;
    static final int MODE_SETUP = 1;

    private static final int GOLD = Color.rgb(212, 175, 95);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_UNLOCK);
        if (mode == MODE_UNLOCK && !Prefs.isProtected(this)) {
            finish();
            return;
        }

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(28), dp(80), dp(28), dp(40));
        col.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(6, 40, 34), Color.rgb(3, 22, 19)}));

        TextView icon = new TextView(this);
        icon.setText("🔒");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        col.addView(icon);

        TextView title = new TextView(this);
        title.setText(mode == MODE_SETUP ? "Set protection password" : "Reel Blocker is protected");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(16), 0, dp(8));
        col.addView(title);

        TextView info = new TextView(this);
        info.setText(mode == MODE_SETUP
                ? "Best: hand your phone to someone you trust (brother, friend) and let them "
                + "type a password you don't know. After this, uninstalling, stopping or "
                + "switching off Reel Blocker needs this password."
                : "To uninstall, stop or change Reel Blocker, enter the password. "
                + "Changes stay unlocked for " + Prefs.UNLOCK_MINUTES + " minutes.");
        info.setTextColor(Color.argb(210, 245, 238, 220));
        info.setTextSize(15);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 0, 0, dp(24));
        col.addView(info);

        EditText pw1 = field("Password");
        col.addView(pw1);
        EditText pw2 = field("Type password again");
        if (mode == MODE_SETUP) col.addView(pw2);

        Button go = new Button(this);
        go.setText(mode == MODE_SETUP ? "Turn on protection" : "Unlock");
        go.setTextColor(Color.rgb(20, 30, 25));
        go.setAllCaps(false);
        go.setTextSize(17);
        GradientDrawable btn = new GradientDrawable();
        btn.setColor(GOLD);
        btn.setCornerRadius(dp(14));
        go.setBackground(btn);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        lp.topMargin = dp(20);
        col.addView(go, lp);

        go.setOnClickListener(v -> {
            String a = pw1.getText().toString();
            if (mode == MODE_SETUP) {
                if (a.length() < 6) {
                    toast("Use at least 6 characters");
                    return;
                }
                if (!a.equals(pw2.getText().toString())) {
                    toast("Passwords don't match");
                    return;
                }
                Prefs.setPassword(this, a);
                // Give 10 minutes so the uninstall lock can be switched on right now.
                Prefs.unlockFor10Min(this);
                toast("Protection is ON. Now tap Activate on the next screen.");
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName admin =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && !dpm.isAdminActive(admin)) {
                    android.content.Intent i = new android.content.Intent(
                            android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    i.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                    i.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Stops Reel Blocker from being uninstalled from anywhere.");
                    startActivity(i);
                }
                finish();
            } else {
                if (Prefs.checkPassword(this, a)) {
                    Prefs.unlockFor10Min(this);
                    toast("Unlocked for " + Prefs.UNLOCK_MINUTES + " minutes");
                    finish();
                } else {
                    pw1.setText("");
                    toast("Wrong password");
                }
            }
        });

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(col);
        setContentView(sv);
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.argb(120, 255, 255, 255));
        e.setTextColor(Color.WHITE);
        e.setTextSize(18);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(40, 255, 255, 255));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.argb(90, 212, 175, 95));
        e.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        e.setLayoutParams(lp);
        return e;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
