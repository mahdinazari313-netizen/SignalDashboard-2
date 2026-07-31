package com.example.signaldashboard.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The exact, fixed list of accepted MetaTrader timeframes and their durations.
 * Order below is smallest -> largest, which doubles as the "rank" used to
 * decide capsule placement (larger timeframe = placed nearer the outer edge).
 */
public final class TimeframeUtil {

    private static final Map<String, Integer> DURATION_MINUTES = new LinkedHashMap<>();

    static {
        DURATION_MINUTES.put("M1", 1);
        DURATION_MINUTES.put("M2", 2);
        DURATION_MINUTES.put("M3", 3);
        DURATION_MINUTES.put("M4", 4);
        DURATION_MINUTES.put("M5", 5);
        DURATION_MINUTES.put("M6", 6);
        DURATION_MINUTES.put("M10", 10);
        DURATION_MINUTES.put("M12", 12);
        DURATION_MINUTES.put("M15", 15);
        DURATION_MINUTES.put("M20", 20);
        DURATION_MINUTES.put("M30", 30);
        DURATION_MINUTES.put("H1", 60);
        DURATION_MINUTES.put("H2", 120);
        DURATION_MINUTES.put("H3", 180);
        DURATION_MINUTES.put("H4", 240);
        DURATION_MINUTES.put("H6", 360);
        DURATION_MINUTES.put("H8", 480);
        DURATION_MINUTES.put("H12", 720);
        DURATION_MINUTES.put("D1", 1440);
        DURATION_MINUTES.put("W1", 10080);
        DURATION_MINUTES.put("MN1", 43200);
    }

    private TimeframeUtil() {
    }

    public static boolean isValid(String tf) {
        return tf != null && DURATION_MINUTES.containsKey(tf);
    }

    public static long getDurationMillis(String tf) {
        Integer minutes = DURATION_MINUTES.get(tf);
        if (minutes == null) return 0L;
        return minutes * 60_000L;
    }

    /** Lower rank = shorter timeframe. -1 if unknown. */
    public static int rank(String tf) {
        int i = 0;
        for (String key : DURATION_MINUTES.keySet()) {
            if (key.equals(tf)) return i;
            i++;
        }
        return -1;
    }
}
