package com.iqbal.reelblock;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class Prefs {
    static final String IG_ON = "ig_on";
    static final String YT_ON = "yt_on";
    static final String FB_ON = "fb_on";
    static final String ADULT_ON = "adult_on";
    static final String TOTAL = "total";
    static final String TODAY = "today";
    static final String ADULT_TOTAL = "adult_total";
    static final String ADULT_TODAY = "adult_today";
    static final String TODAY_DATE = "today_date";

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** Resets the "today" counters when the date changes. */
    private static void rollDay(SharedPreferences p) {
        String d = todayKey();
        if (!d.equals(p.getString(TODAY_DATE, ""))) {
            p.edit().putInt(TODAY, 0).putInt(ADULT_TODAY, 0).putString(TODAY_DATE, d).apply();
        }
    }

    static void addBlock(Context c) {
        SharedPreferences p = get(c);
        rollDay(p);
        p.edit()
                .putInt(TOTAL, p.getInt(TOTAL, 0) + 1)
                .putInt(TODAY, p.getInt(TODAY, 0) + 1)
                .apply();
    }

    static void addAdultBlock(Context c) {
        SharedPreferences p = get(c);
        rollDay(p);
        p.edit()
                .putInt(ADULT_TOTAL, p.getInt(ADULT_TOTAL, 0) + 1)
                .putInt(ADULT_TODAY, p.getInt(ADULT_TODAY, 0) + 1)
                .apply();
    }

    static int today(Context c) {
        SharedPreferences p = get(c);
        return todayKey().equals(p.getString(TODAY_DATE, "")) ? p.getInt(TODAY, 0) : 0;
    }

    static int adultToday(Context c) {
        SharedPreferences p = get(c);
        return todayKey().equals(p.getString(TODAY_DATE, "")) ? p.getInt(ADULT_TODAY, 0) : 0;
    }

    private Prefs() { }
}
