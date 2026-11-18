package com.iqbal.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static final int MAX_NODES = 1200;
    private static final int MAX_GUARD_NODES = 800;

    // Loop protection: if one (non-Settings) app trips the guard this often,
    // stop guarding that app for a while so the phone can never get stuck.
    private static final int LOOP_HITS = 3;
    private static final long LOOP_WINDOW_MS = 60_000;
    private static final long LOOP_PAUSE_MS = 10 * 60_000;

    private final Handler main = new Handler(Looper.getMainLooper());
    private long lastCheck = 0;
    private long lastBlock = 0;
    private long lastGuardSettings = 0;
    private long lastGuardOther = 0;
    private long lastTamper = 0;
    private final Map<String, ArrayDeque<Long>> tamperHits = new HashMap<>();
    private final Map<String, Long> pausedUntil = new HashMap<>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            handle(event);
        } catch (Exception ignored) {
            // Never let one bad screen crash the blocker.
        }
    }

    private void handle(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (pkg.equals(getPackageName())) return;

        // ---- Uninstall protection ----
        if (Prefs.isProtected(this) && !Prefs.isUnlocked(this)) {
            if (guard(event, pkg)) return;
        }
        if (TamperRules.isSettingsLike(pkg)) return;

        // ---- Reels / Shorts and adult content ----
        SharedPreferences p = Prefs.get(this);
        // Adult blocking only runs in browsers and social media apps.
        boolean adultOn = p.getBoolean(Prefs.ADULT_ON, true) && ADULT_WATCH.contains(pkg);

        // Fast path: text being typed in a search box / URL bar.
        if (adultOn && event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            List<CharSequence> texts = event.getText();
            if (texts != null) {
                for (CharSequence t : texts) {
                    if (AdultFilter.isAdult(t)) {
                        blockAdult(SystemClock.elapsedRealtime());
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

        if (reelIds == null && !adultOn) return;

        // Events fire constantly; check at most ~3 times a second.
        long now = SystemClock.elapsedRealtime();
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && now - lastCheck < 350) return;
        lastCheck = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !pkg.equals(root.getPackageName().toString())) return;

        if (reelIds != null && hasVisible(root, pkg, reelIds)) {
            blockReel(now);
            return;
        }
        if (adultOn && screenIsAdult(root)) {
            blockAdult(now);
        }
    }

    // ===================== Uninstall protection =====================

    /** Checks each on-screen window separately. Returns true if it blocked something. */
    private boolean guard(AccessibilityEvent event, String pkg) {
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return false;

        long now = SystemClock.elapsedRealtime();
        boolean settingsEvent = TamperRules.isSettingsLike(pkg);
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // Content changes are frequent: Settings every 350 ms, other apps every 700 ms.
            if (settingsEvent) {
                if (now - lastGuardSettings < 350) return false;
                lastGuardSettings = now;
            } else {
                if (now - lastGuardOther < 700) return false;
                lastGuardOther = now;
            }
        }

        List<AccessibilityWindowInfo> wins = null;
        try {
            wins = getWindows();
        } catch (Exception ignored) { }

        if (wins == null || wins.isEmpty()) {
            return checkWindow(getRootInActiveWindow(), now);
        }
        for (AccessibilityWindowInfo w : wins) {
            int wt = w.getType();
            if (wt == AccessibilityWindowInfo.TYPE_INPUT_METHOD
                    || wt == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) continue;
            if (checkWindow(w.getRoot(), now)) return true;
        }
        return false;
    }

    private boolean checkWindow(AccessibilityNodeInfo root, long now) {
        if (root == null || root.getPackageName() == null) return false;
        String wp = root.getPackageName().toString();
        if (TamperRules.NEVER_GUARD.contains(wp) || wp.equals(getPackageName())) return false;
        Long until = pausedUntil.get(wp);
        if (until != null && now < until) return false;

        String text = visibleText(root, MAX_GUARD_NODES);
        boolean settingsLike = TamperRules.isSettingsLike(wp);
        if (!TamperRules.isTamper(text, settingsLike)) return false;

        if (!settingsLike && tripsLoopProtection(wp, now)) return false;
        blockTamper(now);
        return true;
    }

    /** Records a hit; returns true (and pauses that app) if it looks like a loop. */
    private boolean tripsLoopProtection(String wp, long now) {
        ArrayDeque<Long> hits = tamperHits.get(wp);
        if (hits == null) {
            hits = new ArrayDeque<>();
            tamperHits.put(wp, hits);
        }
        while (!hits.isEmpty() && now - hits.peekFirst() > LOOP_WINDOW_MS) hits.pollFirst();
        hits.addLast(now);
        if (hits.size() > LOOP_HITS) {
            pausedUntil.put(wp, now + LOOP_PAUSE_MS);
            hits.clear();
            return true;
        }
        return false;
    }

    private void blockTamper(long now) {
        if (now - lastTamper < 1000) return;
        lastTamper = now;
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

    // ===================== Screen reading =====================

    /** Text of nodes the user can actually see. Hidden views are ignored. */
    private String visibleText(AccessibilityNodeInfo root, int maxNodes) {
        StringBuilder sb = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int seen = 0;
        while (!queue.isEmpty() && seen < maxNodes) {
            AccessibilityNodeInfo n = queue.poll();
            if (n == null) continue;
            seen++;
            if (!n.isVisibleToUser()) continue; // skip hidden views and everything inside them
            if (n.getText() != null) sb.append(n.getText()).append(' ');
            if (n.getContentDescription() != null) sb.append(n.getContentDescription()).append(' ');
            int c = n.getChildCount();
            for (int i = 0; i < c; i++) {
                AccessibilityNodeInfo child = n.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
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

    /** Visible text only (URL bar, titles, captions, web page text). */
    private boolean screenIsAdult(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int seen = 0;
        while (!queue.isEmpty() && seen < MAX_NODES) {
            AccessibilityNodeInfo n = queue.poll();
            if (n == null) continue;
            seen++;
            if (!n.isVisibleToUser()) continue;
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

    // ===================== Blocking actions =====================

    private void blockReel(long now) {
        if (now - lastBlock < 800) return; // avoid double Back
        lastBlock = now;
        performGlobalAction(GLOBAL_ACTION_BACK);
        Prefs.addBlock(this);
        showReminder(false);
    }

    private void blockAdult(long now) {
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
