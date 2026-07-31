package com.example.signaldashboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    public static final String CHANNEL_FOREGROUND = "foreground_channel";
    public static final String CHANNEL_ALERT = "alert_channel";
    public static final int FOREGROUND_NOTIFICATION_ID = 1001;

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel foreground = new NotificationChannel(
                CHANNEL_FOREGROUND, "Dashboard Service", NotificationManager.IMPORTANCE_LOW);
        foreground.setShowBadge(false);
        nm.createNotificationChannel(foreground);

        NotificationChannel alert = new NotificationChannel(
                CHANNEL_ALERT, "Signal Alerts", NotificationManager.IMPORTANCE_HIGH);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        alert.setSound(sound, null);
        alert.enableVibration(true);
        alert.setVibrationPattern(new long[]{0, 300, 200, 300});
        nm.createNotificationChannel(alert);
    }

    public static Notification buildForegroundNotification(Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
                .setContentTitle("Signal Dashboard")
                .setContentText("Monitoring MetaTrader signals")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public static void sendConfluenceAlert(Context context, String symbol, String direction) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALERT)
                .setContentTitle(symbol)
                .setContentText(direction + " signal confluence detected")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 300, 200, 300});

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int id = (symbol + "_" + direction).hashCode();
            nm.notify(id, builder.build());
        }
    }
}
