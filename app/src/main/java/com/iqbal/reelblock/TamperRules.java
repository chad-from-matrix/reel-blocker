package com.iqbal.reelblock;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides whether ONE on-screen window is an attempt to uninstall, stop or switch off
 * Reel Blocker. Works on the window's VISIBLE text only (lower-case).
 *
 * Kept deliberately strict: a normal screen must never match, even if it shows the
 * "Reel Blocker" icon or name. Pure Java so it can be tested off the phone.
 */
final class TamperRules {

    static final String NAME = "reel blocker";
    /** Our own Accessibility description; appears on the Accessibility page for this app. */
    static final String SERVICE_MARK = "blocks instagram reels";

    // 'Uninstall "Reel Blocker"?' / 'Uninstalling "Reel Blocker" will also delete its data'
    private static final Pattern UNINSTALL_NAMED =
            Pattern.compile("uninstall(ing)?\\W{0,4}reel blocker");
    // 'Stop Reel Blocker?' / 'Turn off Reel Blocker?' / 'Disable Reel Blocker?'
    private static final Pattern STOP_NAMED =
            Pattern.compile("(stop|turn off|disable|deactivate)\\W{0,4}reel blocker");

    /** Apps that are never guarded (so they can never be blocked by mistake). */
    static final Set<String> NEVER_GUARD = new HashSet<>();
    static {
        NEVER_GUARD.add("com.iqbal.reelblock");
        NEVER_GUARD.add("com.anthropic.claude");
        NEVER_GUARD.add("com.android.systemui");
    }

    /** Settings-type system apps, where more screens count as tampering. */
    static boolean isSettingsLike(String pkg) {
        if (pkg == null) return false;
        return pkg.equals("com.android.settings")
                || pkg.contains("packageinstaller")
                || pkg.contains("permissioncontroller")
                || pkg.contains("securitycenter")
                || pkg.contains("safecenter")
                || pkg.contains("securitypermission")
                || pkg.endsWith(".settings");
    }

    /**
     * @param t            visible text of a single window, lower-case
     * @param settingsLike true if that window belongs to a Settings-type app
     */
    static boolean isTamper(String t, boolean settingsLike) {
        if (t == null) return false;
        boolean named = t.contains(NAME);
        boolean serviceScreen = t.contains(SERVICE_MARK);
        if (!named && !serviceScreen) return false;

        // 1) Uninstall confirmation pop-up for THIS app (home screen, app search, Settings, Play Store).
        if (named && t.contains("cancel")
                && (UNINSTALL_NAMED.matcher(t).find() || t.contains("want to uninstall this app"))) {
            return true;
        }
        if (!settingsLike) return false;

        // 2) Settings screens that belong to THIS app.
        if (serviceScreen) return true;                                  // its Accessibility page
        if (STOP_NAMED.matcher(t).find()) return true;                   // "Stop Reel Blocker?"
        if (t.contains("use reel blocker")) return true;                 // Accessibility toggle
        if (named && t.contains("force stop")) return true;              // its App info page
        if (named && t.contains("deactivate")) return true;              // uninstall-lock page
        if (named && (t.contains("clear data") || t.contains("clear storage"))) return true;
        return false;
    }

    private TamperRules() { }
}
