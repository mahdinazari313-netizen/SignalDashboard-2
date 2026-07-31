package com.example.signaldashboard;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks every symbol ever seen in a notification, forever (until uninstall).
 * This set is independent from active signals: a symbol stays here even after
 * all of its signals expire, so future signals for it keep being accepted.
 */
public class SymbolManager {

    private static final String PREFS_NAME = "symbol_prefs";
    private static final String KEY_SYMBOLS = "known_symbols";

    private static SymbolManager instance;

    private SharedPreferences prefs;
    private final Set<String> symbols = new HashSet<>();

    private SymbolManager() {
    }

    public static synchronized SymbolManager getInstance() {
        if (instance == null) {
            instance = new SymbolManager();
        }
        return instance;
    }

    public synchronized void init(Context context) {
        if (prefs != null) return;
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(KEY_SYMBOLS, null);
        if (saved != null) {
            symbols.addAll(saved);
        }
    }

    public synchronized void addSymbol(String symbol) {
        if (symbols.add(symbol)) {
            save();
        }
    }

    public synchronized Set<String> getSymbols() {
        return new HashSet<>(symbols);
    }

    private void save() {
        // Always write a fresh copy; SharedPreferences string sets must not be mutated in place.
        prefs.edit().putStringSet(KEY_SYMBOLS, new HashSet<>(symbols)).apply();
    }
}
