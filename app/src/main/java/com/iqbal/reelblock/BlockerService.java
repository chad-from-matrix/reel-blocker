package com.iqbal.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockerService extends AccessibilityService {

    static final String IG = "com.instagram.android";
    static final String YT = "com.google.android.youtube";
    static final String FB = "com.facebook.katana";
    static final String FB_LITE = "com.facebook.lite";

    // Screen IDs of the Reels / Shorts players.
    // If an app update breaks blocking, these are the lines to update.
    static final String[] IG_REEL_IDS = {
            "clips_viewer_view_pager",
            "clips_viewer_fragment_container"
    };
    static final String[] YT_SHORTS_IDS = {
            "reel_recycler",
            "reel_player_page_container",
            "reel_watch_player"
    };
    static final String[] FB_REEL_IDS = {
            "reels_viewer_container",
            "video_home_reels_container"
    };

    // Apps where adult content is blocked: browsers + social media only.
    static final Set<String> ADULT_WATCH = new HashSet<>();
    static {
        ADULT_WATCH.add("com.android.chrome");
        ADULT_WATCH.add("com.chrome.beta");
        ADULT_WATCH.add("com.sec.android.app.sbrowser");
        ADULT_WATCH.add("org.mozilla.firefox");
        ADULT_WATCH.add("org.mozilla.focus");
        ADULT_WATCH.add("com.brave.browser");
        ADULT_WATCH.add("com.microsoft.emmx");
        ADULT_WATCH.add("com.opera.browser");
        ADULT_WATCH.add("com.opera.mini.native");
        ADULT_WATCH.add("com.UCMobile.intl");
        ADULT_WATCH.add("com.duckduckgo.mobile.android");
        ADULT_WATCH.add("com.mi.globalbrowser");
        ADULT_WATCH.add("com.vivo.browser");
        ADULT_WATCH.add("com.heytap.browser");
        ADULT_WATCH.add("com.kiwibrowser.browser");
        ADULT_WATCH.add("com.google.android.googlequicksearchbox");
        ADULT_WATCH.add(IG);
        ADULT_WATCH.add(YT);
        ADULT_WATCH.add(FB);
        ADULT_WATCH.add(FB_LITE);
        ADULT_WATCH.add("com.twitter.android");
        ADULT_WATCH.add("com.reddit.frontpage");
        ADULT_WATCH.add("org.telegram.messenger");
        ADULT_WATCH.add("com.zhiliaoapp.musically");
        ADULT_WATCH.add("com.snapchat.android");
        ADULT_WATCH.add("com.pinterest");
        ADULT_WATCH.add("com.tumblr");
        ADULT_WATCH.add("com.vk.vkvideo");
        ADULT_WATCH.add("in.mohalla.sharechat");
        ADULT_WATCH.add("in.mohalla.video");
        ADULT_WATCH.add("com.instagram.lite");
    }

    // System screens where the app could be uninstalled, stopped or switched off.
    static final Set<String> SYSTEM_PKGS = new HashSet<>();
    static {
        SYSTEM_PKGS.add("com.android.settings");
        SYSTEM_PKGS.add("com.google.android.packageinstaller");
        SYSTEM_PKGS.add("com.android.packageinstaller");
        SYSTEM_PKGS.add("com.samsung.android.packageinstaller");
        SYSTEM_PKGS.add("com.google.android.permissioncontroller");
        SYSTEM_PKGS.add("com.android.permissioncontroller");
        SYSTEM_PKGS.add("com.oplus.safecenter");
        SYSTEM_PKGS.add("com.coloros.safecenter");
        SYSTEM_PKGS.add("com.oplus.securitypermission");
        SYSTEM_PKGS.add("com.miui.securitycenter");
        SYSTEM_PKGS.add("com.samsung.android.lool");
        SYSTEM_PKGS.add("com.google.android.apps.wellbeing");
        SYSTEM_PKGS.add("com.android.vending");
    }

    // Words that mean someone is trying to remove or disable the app.
    private static final String[] TAMPER_WORDS = {
            "uninstall", "force stop", "deactivate", "use reel blocker", "stop reel blocker",
            "turn off reel blocker", "clear data", "clear storage", "disable",
            "blocks instagram reels"
    };

    private static final int MAX_NODES = 1200;

    private final Handler main = new Handler(Looper.getMainLooper());
    private long lastCheck = 0;
    private long lastBlock = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (pkg.equals(getPackageName())) return;

        // Uninstall protection: guard Settings / uninstall screens for this app.
        if (SYSTEM_PKGS.contains(pkg)) {
            if (Prefs.isProtected(this) && !Prefs.isUnlocked(this)) guardSystemScreen(event, pkg);
            return;
        }

        SharedPreferences p = Prefs.get(this);
        // Adult blocking only runs in browsers and social media apps.
        // Claude, Quran apps, WhatsApp and everything else are never touched.
        boolean adultOn = p.getBoolean(Prefs.ADULT_ON, true) && ADULT_WATCH.contains(pkg);

        // Fast path: text the user is typing (search boxes, URL bars) in any app.
        if (adultOn && event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            List<CharSequence> texts = event.getText();
            if (texts != null) {
                for (CharSequence t : texts) {
                    if (AdultFilter.isAdult(t)) {
                        blockAdult(pkg, SystemClock.elapsedRealtime());
                        return;
                    }
                }
            }
        }

        String[] reelIds = null;
        if (IG.equals(pkg) && p.getBoolean(Prefs.IG_ON, true)) reelIds = IG_REEL_IDS;
        else if (YT.equals(pkg) && p.getBoolean(Prefs.YT_ON, true)) reelIds = YT_SHORTS_IDS;
        else if ((FB.equals(pkg) || FB_LITE.equals(pkg)) && p.getBoolean(Prefs.FB_ON, true))
            reelIds = FB_REEL_IDS;

        boolean scanAdult = adultOn;

        if (reelIds == null && !scanAdult) return;

        // Events fire constantly; check at most ~3 times a second.
        long now = SystemClock.elapsedRealtime();
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && now - lastCheck < 350) return;
        lastCheck = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !pkg.equals(root.getPackageName().toString())) return;

        if (reelIds != null && hasVisible(root, pkg, reelIds)) {
            blockReel(pkg, now);
            return;
        }
        if (scanAdult && screenIsAdult(root)) {
            blockAdult(pkg, now);
        }
    }

    private boolean hasVisible(AccessibilityNodeInfo root, String pkg, String[] ids) {
        for (String id : ids) {
            List<AccessibilityNodeInfo> nodes =
                    root.findAccessibilityNodeInfosByViewId(pkg + ":id/" + id);
            if (nodes == null) continue;
            for (AccessibilityNodeInfo n : nodes) {
                if (n != null && n.isVisibleToUser()) return true;
            }
        }
        return false;
    }

    /** Reads visible text on screen (URL bar, titles, captions, web page text). */
    private boolean screenIsAdult(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int seen = 0;
        while (!queue.isEmpty() && seen < MAX_NODES) {
            AccessibilityNodeInfo n = queue.poll();
            if (n == null) continue;
            seen++;
            if (AdultFilter.isAdult(n.getText())
                    || AdultFilter.isAdult(n.getContentDescription())) {
                return true;
            }
            int c = n.getChildCount();
            for (int i = 0; i < c; i++) {
                AccessibilityNodeInfo child = n.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return false;
    }

    private long lastGuardCheck = 0;

    private void guardSystemScreen(AccessibilityEvent event, String pkg) {
        long now = SystemClock.elapsedRealtime();
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && now - lastGuardCheck < 300) return;
        lastGuardCheck = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !pkg.equals(root.getPackageName().toString())) return;

        String text = collectText(root);
        if (!text.contains("reel blocker") && !text.contains("blocks instagram reels")) return;
        for (String w : TAMPER_WORDS) {
            if (text.contains(w)) {
                blockTamper(now);
                return;
            }
        }
    }

    private String collectText(AccessibilityNodeInfo root) {
        StringBuilder sb = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int seen = 0;
        while (!queue.isEmpty() && seen < MAX_NODES) {
            AccessibilityNodeInfo n = queue.poll();
            if (n == null) continue;
            seen++;
            if (n.getText() != null) sb.append(n.getText()).append(' ');
            if (n.getContentDescription() != null) sb.append(n.getContentDescription()).append(' ');
            int c = n.getChildCount();
            for (int i = 0; i < c; i++) {
                AccessibilityNodeInfo child = n.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return sb.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private void blockTamper(long now) {
        if (now - lastBlock < 1000) return;
        lastBlock = now;
        performGlobalAction(GLOBAL_ACTION_BACK);
        main.postDelayed(() -> {
            performGlobalAction(GLOBAL_ACTION_HOME);
            try {
                Intent i = new Intent(this, PasswordActivity.class);
                i.putExtra(PasswordActivity.EXTRA_MODE, PasswordActivity.MODE_UNLOCK);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "Reel Blocker is protected", Toast.LENGTH_SHORT).show();
            }
        }, 200);
    }

    private void blockReel(String pkg, long now) {
        if (now - lastBlock < 800) return; // avoid double Back
        lastBlock = now;
        performGlobalAction(GLOBAL_ACTION_BACK);
        Prefs.addBlock(this);
        showReminder(false);
    }

    private void blockAdult(String pkg, long now) {
        if (now - lastBlock < 1200) return;
        lastBlock = now;
        // Back leaves the page; the reminder screen then sends you Home.
        performGlobalAction(GLOBAL_ACTION_BACK);
        Prefs.addAdultBlock(this);
        showReminder(true);
    }

    private void showReminder(boolean adult) {
        main.postDelayed(() -> {
            try {
                Intent i = new Intent(this, BlockActivity.class);
                i.putExtra(BlockActivity.EXTRA_ADULT, adult);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(i);
            } catch (Exception e) {
                if (adult) performGlobalAction(GLOBAL_ACTION_HOME);
                Toast.makeText(this, adult ? "Adult content blocked" : "Reels blocked",
                        Toast.LENGTH_SHORT).show();
            }
        }, 150);
    }

    @Override
    public void onInterrupt() { }
}
