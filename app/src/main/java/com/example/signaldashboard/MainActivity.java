package com.example.signaldashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signaldashboard.adapter.SignalCardAdapter;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 501;

    private SignalCardAdapter adapter;
    private BroadcastReceiver updateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SignalManager.getInstance().init(getApplicationContext());
        SymbolManager.getInstance().init(getApplicationContext());

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SignalCardAdapter(this, new SignalCardAdapter.OnCardDismissListener() {
            @Override
            public void onDismiss(String symbol) {
                SignalManager.getInstance().dismissSymbol(symbol);
                refreshData();
            }
        });
        recyclerView.setAdapter(adapter);

        ensureNotificationAccess();
        requestPostNotificationsIfNeeded();

        ContextCompat.startForegroundService(this, new Intent(this, SignalForegroundService.class));

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshData();
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(SignalForegroundService.ACTION_SIGNALS_UPDATED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateReceiver, filter);
        }
        refreshData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(updateReceiver);
    }

    private void refreshData() {
        List<String> symbols = SignalManager.getInstance().getVisibleSymbolsSortedByOldest();
        adapter.submitSymbols(symbols);
    }

    private void ensureNotificationAccess() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean granted = enabled != null && enabled.contains(getPackageName());
        if (!granted) {
            Toast.makeText(this, "Please enable notification access for Signal Dashboard", Toast.LENGTH_LONG).show();
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }
    }

    private void requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
