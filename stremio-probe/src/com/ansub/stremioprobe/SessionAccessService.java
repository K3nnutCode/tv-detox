package com.ansub.stremioprobe;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Only a permission bridge for MediaSessionManager.getActiveSessions().
 * Never reads, records, forwards or processes notification data.
 * Users must explicitly enable or revoke this service in TV settings.
 */
public final class SessionAccessService extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        // Intentionally ignored; all app data other than Stremio's media session is out of scope.
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        // Intentionally ignored.
    }
}
