package com.iqbal.reelblock;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/** Device admin: Android won't uninstall the app until this is switched off first. */
public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Switching this off removes Reel Blocker's uninstall lock.";
    }
}
