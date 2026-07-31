package com.example.signaldashboard;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.signaldashboard.model.Signal;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns all ACTIVE signals in memory, persists them to SharedPreferences as a
 * JSON array, and purges expired ones on every access so they never
 * accumulate. Also tracks per-symbol manual dismiss state and per-key alert
 * cooldowns (used both for the 30s anti-spam guard and the reminder repeat).
 */
public class SignalManager {

    private static final String PREFS_NAME = "signal_prefs";
    private static final String KEY_SIGNALS = "active_signals";

    private static SignalManager instance;

    private final List<Signal> signals = new ArrayList<>();
    private final Set<String> dismissedSymbols = new HashSet<>();
    private final Map<String, Long> lastAlertTime = new HashMap<>();

    private SharedPreferences prefs;

    private SignalManager() {
    }

    public static synchronized SignalManager getInstance() {
        if (instance == null) {
            instance = new SignalManager();
        }
        return instance;
    }

    public synchronized void init(Context context) {
        if (prefs != null) return;
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
        purgeExpired();
    }

    private void load() {
        signals.clear();
        String json = prefs.getString(KEY_SIGNALS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                signals.add(Signal.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            signals.clear();
        }
    }

    private void save() {
        JSONArray arr = new JSONArray();
        for (Signal s : signals) {
            arr.put(s.toJson());
        }
        prefs.edit().putString(KEY_SIGNALS, arr.toString()).apply();
    }

    /** Removes every signal whose expiry has passed, from memory AND storage. */
    public synchronized void purgeExpired() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (int i = signals.size() - 1; i >= 0; i--) {
            if (signals.get(i).isExpired(now)) {
                signals.remove(i);
                changed = true;
            }
        }
        if (changed) save();
    }

    /**
     * Adds a new signal. If an active signal already exists for the same
     * symbol + timeframe, it is replaced (a fresh candle result on that
     * timeframe supersedes the previous one).
     */
    public synchronized void addSignal(Signal newSignal) {
        for (int i = signals.size() - 1; i >= 0; i--) {
            Signal s = signals.get(i);
            if (s.symbol.equals(newSignal.symbol) && s.timeframe.equals(newSignal.timeframe)) {
                signals.remove(i);
            }
        }
        signals.add(newSignal);
        // A fresh signal un-dismisses the card so it reappears.
        dismissedSymbols.remove(newSignal.symbol);
        save();
    }

    public synchronized List<Signal> getActiveSignalsForSymbol(String symbol) {
        purgeExpired();
        List<Signal> result = new ArrayList<>();
        for (Signal s : signals) {
            if (s.symbol.equals(symbol)) result.add(s);
        }
        return result;
    }

    /** Symbols with at least one active, non-dismissed signal, oldest receive time first. */
    public synchronized List<String> getVisibleSymbolsSortedByOldest() {
        purgeExpired();
        final Map<String, Long> oldestReceiveTime = new HashMap<>();
        for (Signal s : signals) {
            if (dismissedSymbols.contains(s.symbol)) continue;
            Long current = oldestReceiveTime.get(s.symbol);
            if (current == null || s.receiveTime < current) {
                oldestReceiveTime.put(s.symbol, s.receiveTime);
            }
        }
        List<String> result = new ArrayList<>(oldestReceiveTime.keySet());
        Collections.sort(result, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return Long.compare(oldestReceiveTime.get(a), oldestReceiveTime.get(b));
            }
        });
        return result;
    }

    public synchronized void dismissSymbol(String symbol) {
        dismissedSymbols.add(symbol);
    }

    public synchronized boolean hasBuyConfluence(String symbol) {
        int buy = 0;
        for (Signal s : getActiveSignalsForSymbol(symbol)) {
            if ("Buy".equals(s.direction)) buy++;
        }
        return buy >= 2;
    }

    public synchronized boolean hasSellConfluence(String symbol) {
        int sell = 0;
        for (Signal s : getActiveSignalsForSymbol(symbol)) {
            if ("Sell".equals(s.direction)) sell++;
        }
        return sell >= 2;
    }

    /**
     * Generic cooldown check shared by the 30s anti-spam guard (listener) and
     * the N-minute reminder repeat (foreground service). Returns true (and
     * records "now") only if intervalMillis has elapsed since the last alert
     * sent under this key.
     */
    public synchronized boolean canAlert(String key, long intervalMillis) {
        long now = System.currentTimeMillis();
        Long last = lastAlertTime.get(key);
        if (last == null || now - last >= intervalMillis) {
            lastAlertTime.put(key, now);
            return true;
        }
        return false;
    }

    public synchronized Set<String> getAllSymbolsInMemory() {
        purgeExpired();
        Set<String> set = new HashSet<>();
        for (Signal s : signals) set.add(s.symbol);
        return set;
    }
}
