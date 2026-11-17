package com.iqbal.reelblock;

import java.util.Locale;
import java.util.regex.Pattern;

/** Detects adult websites and adult words in URLs, search boxes and visible text. */
final class AdultFilter {

    // Adult website names (matched anywhere in a URL or text, e.g. "m.xvideos.com").
    private static final String[] SITES = {
            "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "spankbang",
            "brazzers", "onlyfans", "chaturbate", "stripchat", "bongacams", "livejasmin",
            "cam4", "myfreecams", "rule34", "nhentai", "hanime", "eporner", "tnaflix",
            "porntrex", "motherless", "fapello", "thothub", "erome", "sxyprn", "hqporner",
            "porn300", "pornpics", "imagefap", "literotica", "xossip", "desixnxx",
            "antarvasna", "fsiblog", "mydesi", "indianporn", "kamababa", "freesexyindians",
            "aagmaal", "masahub", "ullu.app", "porn.com", "xxx.com", "sex.com", "playboy.com",
            "fansly", "manyvids", "clips4sale", "redgifs", "porngifs", "thisvid", "beeg",
            "txxx", "hclips", "porndig", "4tube", "porntube", "tube8", "drtuber", "sunporno"
    };

    // Adult words, matched as whole words only (so "Essex" or "class" never trigger).
    private static final String[] WORDS = {
            "porn", "porno", "pornography", "xxx", "nsfw", "hentai", "nude", "nudes", "nudity",
            "boobs", "tits", "pussy", "dick pic", "milf", "blowjob", "handjob",
            "cumshot", "creampie", "gangbang", "threesome", "bdsm", "fetish", "camgirl",
            "cam girl", "sex video", "sex videos", "sexvideo", "sex chat", "sex movie",
            "sex scene", "sex tape", "sextape", "hot sex", "xxx video", "adult video",
            "blue film", "bf video", "sexy video", "sexy videos", "erotic", "erotica",
            "striptease", "stripper", "only fans", "18+ video", "uncensored", "onlyfans leak",
            "chudai", "nangi", "desi sex", "desi mms",
            "mms video", "bhabhi sex", "hot bhabhi", "aunty sex", "masala video", "ullu web series"
    };

    private static final Pattern WORD_PATTERN;
    static {
        StringBuilder sb = new StringBuilder("(?<![a-z0-9])(");
        for (int i = 0; i < WORDS.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(Pattern.quote(WORDS[i]));
        }
        sb.append(")(?![a-z0-9])");
        WORD_PATTERN = Pattern.compile(sb.toString());
    }

    /** Returns true if the text looks adult. */
    static boolean isAdult(CharSequence raw) {
        if (raw == null || raw.length() < 3) return false;
        String t = raw.toString().toLowerCase(Locale.ROOT);
        for (String s : SITES) {
            if (t.contains(s)) return true;
        }
        // Treat URL separators as spaces so "sex-video" and "sex_video" match "sex video".
        String spaced = t.replace('-', ' ').replace('_', ' ').replace('+', ' ')
                .replace("%20", " ");
        return WORD_PATTERN.matcher(spaced).find();
    }

    private AdultFilter() { }
}
