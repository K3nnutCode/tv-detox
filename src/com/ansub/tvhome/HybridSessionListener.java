package com.ansub.tvhome;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Permission bridge only. Deliberately never reads or stores notifications. */
public final class HybridSessionListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification notification) { }
    @Override public void onNotificationRemoved(StatusBarNotification notification) { }
}
