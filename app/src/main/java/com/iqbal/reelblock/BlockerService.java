package com.iqbal.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

public class BlockerService extends AccessibilityService {

    static final String IG = "com.instagram.android";
    static final String YT = "com.google.android.youtube";

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

    private long lastCheck = 0;
    private long lastBlock = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();

        SharedPreferences p = Prefs.get(this);
        String[] ids;
        if (IG.equals(pkg)) {
            if (!p.getBoolean(Prefs.IG_ON, true)) return;
            ids = IG_REEL_IDS;
        } else if (YT.equals(pkg)) {
            if (!p.getBoolean(Prefs.YT_ON, true)) return;
            ids = YT_SHORTS_IDS;
        } else {
            return;
        }

        // Content-change events fire constantly; check at most ~4 times a second.
        long now = SystemClock.elapsedRealtime();
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && now - lastCheck < 250) return;
        lastCheck = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !pkg.equals(root.getPackageName().toString())) return;

        if (hasVisible(root, pkg, ids)) block(pkg, now);
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

    private void block(String pkg, long now) {
        if (now - lastBlock < 800) return; // avoid double Back
        lastBlock = now;
        performGlobalAction(GLOBAL_ACTION_BACK);
        Prefs.addBlock(this);
        Toast.makeText(this,
                IG.equals(pkg) ? "Reels blocked" : "Shorts blocked",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInterrupt() { }
}
