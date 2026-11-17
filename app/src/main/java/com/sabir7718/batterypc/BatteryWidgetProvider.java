package com.sabir7718.batterypc;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.widget.RemoteViews;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

import java.lang.reflect.Method;

public class BatteryWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_0 = "com.sabir7718.BAT_0";
    private static final String ACTION_1 = "com.sabir7718.BAT_1";
    private static final String ACTION_2 = "com.sabir7718.BAT_2";
    private static final String ACTION_3 = "com.sabir7718.BAT_3";
    private static final String ACTION_4 = "com.sabir7718.BAT_4";
    private static final String ACTION_5 = "com.sabir7718.BAT_5";
    private static final String ACTION_6 = "com.sabir7718.BAT_6";
    private static final String ACTION_7 = "com.sabir7718.BAT_7";
    private static final String ACTION_8 = "com.sabir7718.BAT_8";
    private static final String ACTION_9 = "com.sabir7718.BAT_9";

    private static final String ACTION_BACK = "com.sabir7718.BAT_BACK";
    private static final String ACTION_CLEAR = "com.sabir7718.BAT_CLEAR";
    private static final String ACTION_SET = "com.sabir7718.BAT_SET";
    private static final String ACTION_RESET = "com.sabir7718.BAT_RESET";

    private static final String PREFS = "battery_widget";
    private static final String KEY_INPUT = "input";
    private static final String KEY_LEVEL = "level";

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(
            Context context,
            AppWidgetManager manager,
            int widgetId) {

        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.battery_widget
        );

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String input = prefs.getString(KEY_INPUT, "");
        int level = prefs.getInt(KEY_LEVEL, 100);

        if (input.isEmpty()) {
            views.setTextViewText(
                    R.id.widget_display,
                    String.valueOf(level)
            );
        } else {
            views.setTextViewText(
                    R.id.widget_display,
                    input
            );
        }

        setButton(context, views, R.id.btn_0, ACTION_0);
        setButton(context, views, R.id.btn_1, ACTION_1);
        setButton(context, views, R.id.btn_2, ACTION_2);
        setButton(context, views, R.id.btn_3, ACTION_3);
        setButton(context, views, R.id.btn_4, ACTION_4);
        setButton(context, views, R.id.btn_5, ACTION_5);
        setButton(context, views, R.id.btn_6, ACTION_6);
        setButton(context, views, R.id.btn_7, ACTION_7);
        setButton(context, views, R.id.btn_8, ACTION_8);
        setButton(context, views, R.id.btn_9, ACTION_9);

        setButton(context, views, R.id.btn_back, ACTION_BACK);
        setButton(context, views, R.id.btn_clear, ACTION_CLEAR);
        setButton(context, views, R.id.btn_set, ACTION_SET);
        setButton(context, views, R.id.btn_widget_reset, ACTION_RESET);

        manager.updateAppWidget(widgetId, views);
    }

    private void setButton(
            Context context,
            RemoteViews views,
            int viewId,
            String action) {

        views.setOnClickPendingIntent(
                viewId,
                getPendingIntent(context, action)
        );
    }

    private PendingIntent getPendingIntent(
            Context context,
            String action) {

        Intent intent =
                new Intent(context, BatteryWidgetProvider.class);

        intent.setAction(action);

        return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        String action = intent.getAction();

        if (action == null) {
            return;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String input = prefs.getString(KEY_INPUT, "");

        if (action.startsWith("com.sabir7718.BAT_")) {

            String digit = getDigit(action);

            if (digit != null) {

                if (input.length() < 4) {
                    input += digit;
                }

                prefs.edit()
                        .putString(KEY_INPUT, input)
                        .apply();

            } else if (ACTION_BACK.equals(action)) {

                if (!input.isEmpty()) {
                    input = input.substring(0, input.length() - 1);
                }

                prefs.edit()
                        .putString(KEY_INPUT, input)
                        .apply();

            } else if (ACTION_CLEAR.equals(action)) {

                prefs.edit()
                        .putString(KEY_INPUT, "")
                        .apply();

            } else if (ACTION_SET.equals(action)) {

                if (!input.isEmpty()) {

                    try {
                        int value = Integer.parseInt(input);

                        if (value > 9999) {
                            value = 9999;
                        }

                        prefs.edit()
                                .putInt(KEY_LEVEL, value)
                                .putString(KEY_INPUT, "")
                                .apply();

                        executeShellCmd(
                                "dumpsys battery set level " + value
                        );

                    } catch (Exception ignored) {
                    }
                }

            } else if (ACTION_RESET.equals(action)) {

                prefs.edit()
                        .putInt(KEY_LEVEL, 100)
                        .putString(KEY_INPUT, "")
                        .apply();

                executeShellCmd(
                        "dumpsys battery reset"
                );
            }

            refreshAllWidgets(context);
        }
    }

    private String getDigit(String action) {

        if (ACTION_0.equals(action)) return "0";
        if (ACTION_1.equals(action)) return "1";
        if (ACTION_2.equals(action)) return "2";
        if (ACTION_3.equals(action)) return "3";
        if (ACTION_4.equals(action)) return "4";
        if (ACTION_5.equals(action)) return "5";
        if (ACTION_6.equals(action)) return "6";
        if (ACTION_7.equals(action)) return "7";
        if (ACTION_8.equals(action)) return "8";
        if (ACTION_9.equals(action)) return "9";

        return null;
    }

    private void refreshAllWidgets(Context context) {

        AppWidgetManager manager =
                AppWidgetManager.getInstance(context);

        ComponentName component =
                new ComponentName(
                        context,
                        BatteryWidgetProvider.class
                );

        int[] ids =
                manager.getAppWidgetIds(component);

        onUpdate(context, manager, ids);
    }

    private void executeShellCmd(String command) {

        new Thread(() -> {

            try {

                if (!Shizuku.pingBinder()) {
                    return;
                }

                if (Shizuku.checkSelfPermission()
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                Method method =
                        Shizuku.class.getDeclaredMethod(
                                "newProcess",
                                String[].class,
                                String[].class,
                                String.class
                        );

                method.setAccessible(true);

                String[] cmd = {
                        "sh",
                        "-c",
                        command
                };

                ShizukuRemoteProcess process =
                        (ShizukuRemoteProcess) method.invoke(
                                null,
                                cmd,
                                null,
                                null
                        );

                if (process != null) {
                    process.waitFor();
                }

            } catch (Exception ignored) {
            }

        }).start();
    }
}