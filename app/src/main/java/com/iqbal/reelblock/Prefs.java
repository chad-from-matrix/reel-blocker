package com.iqbal.reelblock;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class Prefs {
    static final String IG_ON = "ig_on";
    static final String YT_ON = "yt_on";
    static final String TOTAL = "total";
    static final String TODAY = "today";
    static final String TODAY_DATE = "today_date";

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static void addBlock(Context c) {
        SharedPreferences p = get(c);
        String d = todayKey();
        int today = d.equals(p.getString(TODAY_DATE, "")) ? p.getInt(TODAY, 0) : 0;
        p.edit()
                .putInt(TOTAL, p.getInt(TOTAL, 0) + 1)
                .putInt(TODAY, today + 1)
                .putString(TODAY_DATE, d)
                .apply();
    }

    static int today(Context c) {
        SharedPreferences p = get(c);
        return todayKey().equals(p.getString(TODAY_DATE, "")) ? p.getInt(TODAY, 0) : 0;
    }

    private Prefs() { }
}
