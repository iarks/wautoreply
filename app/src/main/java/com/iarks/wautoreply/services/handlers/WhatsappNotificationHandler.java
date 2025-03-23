package com.iarks.wautoreply.services.handlers;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.iarks.wautoreply.services.NotificationHandler;

public class WhatsappNotificationHandler implements NotificationHandler {
    private static final String TAG = "WAHandler";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String PREFS_NAME = "AutoReplyPrefs";
    private static final String KEY_LAST_REPLY = "last_reply_";
    private static final long COOLDOWN_MS = 15 * 60 * 1000; // 15 minutes

    private static final String FALLBACK_REPLY = "Hey! I don’t actively monitor WhatsApp anymore, so my replies might be delayed. You can reach me faster on Signal! 😊";

    private final Context context;

    public WhatsappNotificationHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandle(StatusBarNotification sbn) {
        return WHATSAPP_PACKAGE.equals(sbn.getPackageName());
    }

    @Override
    public void handleNotification(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification.extras == null || "msg".equalsIgnoreCase(notification.category))
            return;

        String sender = notification.extras.getString(Notification.EXTRA_TITLE);
        String message = notification.extras.getString(Notification.EXTRA_TEXT);
        if (sender == null || sender.equalsIgnoreCase("whatsapp") || sender.equalsIgnoreCase("you") || message == null) // ignore messages where the sender is whatsapp - this will happen when whatsapp clubs multiple notifications together
            return;

        var senderKey = notification.getShortcutId().split("@");

        if(senderKey.length > 0)
            sender = senderKey[0]; // use a more formal key

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastReplyTime = prefs.getLong(KEY_LAST_REPLY + sender, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastReplyTime < COOLDOWN_MS) {
            Log.d(TAG, "Skipping reply to " + sender + " (cooldown active)");
            return;
        }

        prefs.edit().putLong(KEY_LAST_REPLY + sender, currentTime).apply();
        Log.d(TAG, "Replying to " + sender + ": " + message);

        replyToNotification(notification, FALLBACK_REPLY);
    }

    private void replyToNotification(Notification notification, String replyMessage) {


        for (Notification.Action action : notification.actions) {
            if (action.getRemoteInputs() != null) {
                for (RemoteInput remoteInput : action.getRemoteInputs()) {
                    if (remoteInput.getResultKey() != null && "reply".equalsIgnoreCase(remoteInput.getLabel().toString())) {

                        // Create a reply bundle
                        Bundle replyBundle = new Bundle();
                        replyBundle.putCharSequence(remoteInput.getResultKey(), replyMessage);

                        // Create an intent to send the reply
                        Intent replyIntent = new Intent();
                        replyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        RemoteInput.addResultsToIntent(action.getRemoteInputs(), replyIntent, replyBundle);

                        try {
                            action.actionIntent.send(context, 0, replyIntent);
                            Log.d(TAG, "Reply sent successfully!");
                        } catch (PendingIntent.CanceledException e) {
                            Log.e(TAG, "Failed to send reply", e);
                        }
                        return;
                    }
                }
            }
        }
    }
}
