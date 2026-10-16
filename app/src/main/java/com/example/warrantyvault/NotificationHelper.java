package com.example.warrantyvault;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationHelper {

    private static final String CHANNEL_ID = "warranty_reminders";
    private static final String CHANNEL_NAME = "Warranty Reminders";
    private static final int NOTIFICATION_ID = 101;

    static final String PREFS_NAME = "WarrantyVaultNotifications";
    static final String KEY_NOTIFS_ENABLED = "notifications_enabled";
    private static final String KEY_LAST_DAY = "last_reminder_day";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY = 50;

    /** One received notification, shown in the Notifications screen. */
    public static class Item {
        public String title, message;
        public long time;
    }

    public static List<Item> getHistory(Context context) {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_HISTORY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Item it = new Item();
                it.title = o.optString("t");
                it.message = o.optString("m");
                it.time = o.optLong("ts");
                out.add(it);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void clearHistory(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_HISTORY).apply();
    }

    private static void record(Context context, String title, String text) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            JSONArray fresh = new JSONArray();
            fresh.put(new JSONObject().put("t", title).put("m", text).put("ts", System.currentTimeMillis()));
            for (int i = 0; i < old.length() && fresh.length() < MAX_HISTORY; i++) fresh.put(old.get(i));
            prefs.edit().putString(KEY_HISTORY, fresh.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Reminders for expiring product warranties");
            nm.createNotificationChannel(channel);
        }
    }

    private static void post(Context context, String title, String text) {
        if (!canPost(context)) return;
        ensureChannel(context);

        Intent intent = new Intent(context, MyProductsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    /**
     * Called whenever the dashboard loads: if reminders are enabled and some warranties expire within
     * 30 days, shows ONE summary notification (at most once per day).
     */
    public static void checkAndNotify(Context context, List<Product> products) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_NOTIFS_ENABLED, true)) return;

        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        if (today.equals(prefs.getString(KEY_LAST_DAY, ""))) return;

        int count = 0;
        Product first = null;
        for (Product p : products) {
            if (WarrantyUtils.EXPIRING.equalsIgnoreCase(p.getWarrantyStatus())) {
                if (first == null) first = p;
                count++;
            }
        }
        if (count == 0 || first == null) return;

        String title;
        String text;
        if (count == 1) {
            title = "Warranty Expiring Soon!";
            text = first.getProductName() + " — " + WarrantyUtils.describe(first.getWarrantyExpiry()).toLowerCase(Locale.ROOT)
                    + " (" + first.getWarrantyExpiry() + ")";
        } else {
            title = count + " warranties expiring soon";
            text = first.getProductName() + " and " + (count - 1) + " more expire within 30 days. Tap to review.";
        }
        record(context, title, text);   // always kept in the in-app list
        post(context, title, text);     // phone notification only if allowed
        prefs.edit().putString(KEY_LAST_DAY, today).apply();
    }
}
