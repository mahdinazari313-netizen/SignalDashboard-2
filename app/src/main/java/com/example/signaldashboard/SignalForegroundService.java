package com.example.signaldashboard;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.Set;

/** Background lifecycle: expiry maintenance, confluence reminders and UI refresh. */
public class SignalForegroundService extends Service {

    public static final String ACTION_SIGNALS_UPDATED = "com.example.signaldashboard.SIGNALS_UPDATED";
    private static final long TICK_INTERVAL_MS = 5_000L;

    private Handler handler;
    private Runnable tickRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        SignalManager.getInstance().init(getApplicationContext());
        SymbolManager.getInstance().init(getApplicationContext());
        NotificationHelper.createChannels(getApplicationContext());

        handler = new Handler(Looper.getMainLooper());
        tickRunnable = new Runnable() {
            @Override public void run() {
                SignalManager.getInstance().purgeExpired();
                checkReminders();
                sendBroadcast(new Intent(ACTION_SIGNALS_UPDATED));
                handler.postDelayed(this, TICK_INTERVAL_MS);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                NotificationHelper.buildForegroundNotification(getApplicationContext()));
        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);
        return START_STICKY;
    }

    private void checkReminders() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String mode = prefs.getString("alert_mode", "Confluence");
        if (!"Confluence".equals(mode)) return;

        int reminderMinutes;
        try {
            reminderMinutes = Integer.parseInt(prefs.getString("reminder_minutes", "10"));
        } catch (NumberFormatException e) {
            reminderMinutes = 10;
        }
        if (reminderMinutes <= 0) return;

        long reminderMillis = reminderMinutes * 60_000L;
        SignalManager sm = SignalManager.getInstance();
        Set<String> symbols = sm.getAllSymbolsInMemory();

        for (String symbol : symbols) {
            if (sm.hasBuyConfluence(symbol) && sm.canAlert(symbol + "_Buy", reminderMillis)) {
                NotificationHelper.sendConfluenceAlert(getApplicationContext(), symbol, "Buy");
            }
            if (sm.hasSellConfluence(symbol) && sm.canAlert(symbol + "_Sell", reminderMillis)) {
                NotificationHelper.sendConfluenceAlert(getApplicationContext(), symbol, "Sell");
            }
        }
    }

    @Override public void onDestroy() {
        if (handler != null && tickRunnable != null) handler.removeCallbacks(tickRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override public IBinder onBind(Intent intent) { return null; }
}
