package com.iqbal.reelblock;

import android.accessibilityservice.AccessibilityService;
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

    static final Set<String> BROWSERS = new HashSet<>();
    static {
        BROWSERS.add("com.android.chrome");
        BROWSERS.add("com.chrome.beta");
        BROWSERS.add("com.sec.android.app.sbrowser");
        BROWSERS.add("org.mozilla.firefox");
        BROWSERS.add("org.mozilla.focus");
        BROWSERS.add("com.brave.browser");
        BROWSERS.add("com.microsoft.emmx");
        BROWSERS.add("com.opera.browser");
        BROWSERS.add("com.opera.mini.native");
        BROWSERS.add("com.UCMobile.intl");
        BROWSERS.add("com.duckduckgo.mobile.android");
        BROWSERS.add("com.mi.globalbrowser");
        BROWSERS.add("com.vivo.browser");
        BROWSERS.add("com.heytap.browser");
        BROWSERS.add("com.kiwibrowser.browser");
        BROWSERS.add("com.google.android.googlequicksearchbox");
    }

    private static final int MAX_NODES = 1200;

    private final Handler main = new Handler(Looper.getMainLooper());
    private long lastCheck = 0;
    private long lastBlock = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (pkg.equals(getPackageName())) return;

        SharedPreferences p = Prefs.get(this);
        boolean adultOn = p.getBoolean(Prefs.ADULT_ON, true);

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

        boolean scanAdult = adultOn
                && (BROWSERS.contains(pkg) || IG.equals(pkg) || YT.equals(pkg)
                || FB.equals(pkg) || FB_LITE.equals(pkg)
                || "com.twitter.android".equals(pkg) || "com.reddit.frontpage".equals(pkg)
                || "org.telegram.messenger".equals(pkg) || "com.zhiliaoapp.musically".equals(pkg)
                || "com.snapchat.android".equals(pkg) || "com.pinterest".equals(pkg));

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

    private void blockReel(String pkg, long now) {
        if (now - lastBlock < 800) return; // avoid double Back
        lastBlock = now;
        performGlobalAction(GLOBAL_ACTION_BACK);
        Prefs.addBlock(this);
        String what = YT.equals(pkg) ? "Shorts" : "Reels";
        Toast.makeText(this, what + " blocked", Toast.LENGTH_SHORT).show();
    }

    private void blockAdult(String pkg, long now) {
        if (now - lastBlock < 1200) return;
        lastBlock = now;
        // Back first (leaves the page), then Home (gets out of the app).
        performGlobalAction(GLOBAL_ACTION_BACK);
        main.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_HOME), 250);
        Prefs.addAdultBlock(this);
        Toast.makeText(this, "Adult content blocked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInterrupt() { }
}
