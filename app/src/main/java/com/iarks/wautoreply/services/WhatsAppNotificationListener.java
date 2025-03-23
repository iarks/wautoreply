package com.iarks.wautoreply.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.iarks.wautoreply.R;
import com.iarks.wautoreply.services.handlers.WhatsappNotificationHandler;

public class WhatsAppNotificationListener extends NotificationListenerService {
    private static final String TAG = "WAListener";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";

    private static final String reply = "automated reply from ";

    @Override
    public void onListenerConnected() {
        Log.d(TAG, "Notification Listener Connected!");
        startForegroundService();
    }

    @Override
    public void onListenerDisconnected() {
        Log.d(TAG, "Service Disconnected! Restarting...");
        requestRebind(new ComponentName(this, WhatsAppNotificationListener.class));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!WHATSAPP_PACKAGE.equals(sbn.getPackageName())) {
            return; // Ignore non-WhatsApp notifications
        }

        Notification notification = sbn.getNotification();
        if (notification.extras != null) {
            String message = notification.extras.getString(Notification.EXTRA_TEXT);
            String sender = notification.extras.getString(Notification.EXTRA_TITLE);

            Log.d(TAG, "New WhatsApp Message: " + sender + ": " + message);

            if (message != null && sender != null) {
                Log.d(TAG, "New WhatsApp Message: " + sender + ": " + message);

                // Forward the message to Accessibility Service

                replyToNotification(sbn);
            }
        }
    }

    private void replyToNotification(StatusBarNotification sbn) {
        new WhatsappNotificationHandler(this).handleNotification(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "whatsapp_auto_reply_service";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "WhatsApp Auto-Reply Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }

            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("WhatsApp Auto-Reply is Active")
                    .setContentText("Listening for WhatsApp messages")
                    .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure you have an icon in res/drawable
                    .build();

            startForeground(1, notification);
        }
    }
}
