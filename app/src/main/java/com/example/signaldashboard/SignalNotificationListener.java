package com.example.signaldashboard;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.preference.PreferenceManager;

import com.example.signaldashboard.model.Signal;
import com.example.signaldashboard.util.TimeframeUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures MetaTrader notifications and turns matching text into Signals.
 * Expected format (parentheses OR single quotes around symbol,timeframe):
 * (SYMBOL,TIMEFRAME) ABCD DIRECTION Signal-[YYYY.MM.DD HH:MM:SS]-ABCD-A X-
 */
public class SignalNotificationListener extends NotificationListenerService {

    private static final Pattern SIGNAL_PATTERN = Pattern.compile(
            "[(']([A-Za-z0-9._]+),([A-Za-z0-9]+)[)']\\s*ABCD\\s+(Buy|Sell)(?:\\s+OnClose)?.*?" +
                    "\\[(\\d{4}\\.\\d{2}\\.\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})\\]",
            Pattern.DOTALL);

    @Override
    public void onCreate() {
        super.onCreate();
        SignalManager.getInstance().init(getApplicationContext());
        SymbolManager.getInstance().init(getApplicationContext());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        String text = extractText(sbn.getNotification());
        if (text == null) return;

        Matcher m = SIGNAL_PATTERN.matcher(text);
        if (!m.find()) return; // malformed -> silently ignored

        String symbol = m.group(1);
        String timeframe = m.group(2);
        String direction = m.group(3);
        String timestampStr = m.group(4);

        if (!TimeframeUtil.isValid(timeframe)) return; // not in the exact allowed list -> ignored

        long receiveTime;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
            Date parsed = format.parse(timestampStr);
            if (parsed == null) return;
            receiveTime = parsed.getTime();
        } catch (ParseException e) {
            return;
        }

        int candleCount = getCandleCount();
        long validityMillis = TimeframeUtil.getDurationMillis(timeframe) * candleCount;
        long expiryTime = receiveTime + validityMillis;

        if (expiryTime <= System.currentTimeMillis()) return; // already expired on arrival

        Signal signal = new Signal(symbol, timeframe, direction, receiveTime, expiryTime);

        SymbolManager.getInstance().addSymbol(symbol);
        SignalManager.getInstance().addSignal(signal);

        handleAlerts(symbol);

        sendBroadcast(new Intent(SignalForegroundService.ACTION_SIGNALS_UPDATED));
    }

    private void handleAlerts(String symbol) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String mode = prefs.getString("alert_mode", "Confluence");
        SignalManager sm = SignalManager.getInstance();

        if ("Single".equals(mode)) {
            if (sm.canAlert(symbol + "_single", 30_000L)) {
                NotificationHelper.sendConfluenceAlert(getApplicationContext(), symbol, "New");
            }
            return;
        }

        // Confluence mode: 2+ same-direction timeframes active simultaneously.
        if (sm.hasBuyConfluence(symbol) && sm.canAlert(symbol + "_Buy", 30_000L)) {
            NotificationHelper.sendConfluenceAlert(getApplicationContext(), symbol, "Buy");
        }
        if (sm.hasSellConfluence(symbol) && sm.canAlert(symbol + "_Sell", 30_000L)) {
            NotificationHelper.sendConfluenceAlert(getApplicationContext(), symbol, "Sell");
        }
    }

    private int getCandleCount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String raw = prefs.getString("candle_count", "5");
        try {
            int val = Integer.parseInt(raw);
            return val > 0 ? val : 5;
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    private String extractText(Notification notification) {
        Bundle extras = notification.extras;
        if (extras == null) return null;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append(title).append(' ');
        if (bigText != null) sb.append(bigText);
        else if (text != null) sb.append(text);

        return sb.length() > 0 ? sb.toString() : null;
    }
}
