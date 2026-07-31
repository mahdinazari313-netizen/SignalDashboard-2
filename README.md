# Signal Dashboard — AIDE Project

Pure Java, AndroidX-only Android app that reads MetaTrader notifications and
displays active trading signals. No third-party libraries, no broker
connection, no technical analysis.

## Setup in AIDE

1. In AIDE: **New Project → Android → Empty Activity** (or "Import Project"
   if AIDE offers a folder-import option), package `com.example.signaldashboard`.
2. Replace the generated files with the ones in this project, keeping the
   same relative paths:
   - `app/src/main/java/com/example/signaldashboard/...`
   - `app/src/main/res/...`
   - `app/src/main/AndroidManifest.xml`
3. Make sure these AndroidX dependencies are available to the project
   (AIDE resolves these via its Maven/Gradle dependency manager or bundled
   AARs, depending on your AIDE version):
   - `androidx.appcompat:appcompat`
   - `androidx.recyclerview:recyclerview`
   - `androidx.cardview:cardview`
   - `androidx.preference:preference`
   No other libraries are required — `org.json` is part of the Android SDK.
4. Add a launcher icon (`mipmap/ic_launcher`) via AIDE's asset wizard, or
   remove `android:icon` usage — the manifest currently has no explicit
   `android:icon` attribute so AIDE's default icon will be used automatically.
5. Build and run. On first launch the app will prompt you to grant
   **Notification Access** (required to read MetaTrader's notifications) and,
   on Android 13+, the **Post Notifications** runtime permission.

## Assumptions made where the spec left room for interpretation

- **Duplicate signals on the same symbol+timeframe**: a new signal for a
  timeframe that already has an active signal *replaces* it (a fresh candle
  result supersedes the old one) rather than stacking duplicates.
- **Dismiss behavior**: long-press dismiss is in-memory only (not persisted
  across process death), matching "if a new signal arrives, the card
  reappears" — the app doesn't need to remember a dismiss forever anyway
  since expiry will clear the underlying signals regardless.
- **Alert cooldown**: the 30s anti-spam guard and the N-minute reminder
  repeat share the same per-symbol-per-direction cooldown timestamp. This
  makes the reminder timer naturally reset on every alert (initial or
  repeat) and makes `reminder_minutes = 0` mean "no further alerts after the
  first" as specified.
- **UI refresh cadence**: the foreground service purges expired signals and
  broadcasts a UI-refresh every 5 seconds, and also on every new parsed
  notification. Each countdown circle animates itself every second
  independently (via its own Handler), so the countdown looks smooth without
  needing to rebuild the whole card list every second — this keeps CPU/battery
  use low per the performance goals.
- **No empty-state view**: `activity_main.xml` has no placeholder text or
  view for the empty case, since the spec explicitly requires the screen be
  completely empty when there are no active signals.
- **Symbol regex**: the symbol/timeframe extraction regex allows letters,
  digits, `.` and `_` in the symbol (covers common broker suffixes like
  `EURUSD.a`); adjust `SIGNAL_PATTERN` in `SignalNotificationListener.java`
  if your broker's symbol names use other characters.

## Files

```
app/src/main/java/com/example/signaldashboard/
  MainActivity.java
  SettingsActivity.java
  SettingsFragment.java
  SignalManager.java
  SymbolManager.java
  SignalNotificationListener.java
  SignalForegroundService.java
  NotificationHelper.java
  model/Signal.java
  adapter/SignalCardAdapter.java
  view/CountdownCircleView.java
  util/TimeframeUtil.java
app/src/main/res/layout/activity_main.xml
app/src/main/res/layout/activity_settings.xml
app/src/main/res/layout/item_signal_card.xml
app/src/main/res/layout/capsule_signal.xml
app/src/main/res/values/colors.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/arrays.xml
app/src/main/res/values/styles.xml
app/src/main/res/xml/preferences.xml
app/src/main/res/menu/menu_main.xml
app/src/main/AndroidManifest.xml
```
