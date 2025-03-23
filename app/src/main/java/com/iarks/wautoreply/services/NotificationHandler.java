package com.iarks.wautoreply.services;

import android.service.notification.StatusBarNotification;

public interface NotificationHandler {
    boolean canHandle(StatusBarNotification sbn); // Checks if this handler should process the notification
    void handleNotification(StatusBarNotification sbn); // Processes the notification
}
