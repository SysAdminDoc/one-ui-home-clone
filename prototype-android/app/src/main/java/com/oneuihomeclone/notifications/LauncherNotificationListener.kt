package com.oneuihomeclone.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class LauncherNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        publishActiveCounts()
    }

    override fun onListenerDisconnected() {
        NotificationBadgeRepository.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishActiveCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishActiveCounts()
    }

    private fun publishActiveCounts() {
        val notifications = runCatching { activeNotifications }
            .onFailure { cause ->
                Log.w(TAG, "Notification badge count refresh failed (${cause.javaClass.simpleName})")
            }
            .getOrNull()
        NotificationBadgeRepository.updateFromActiveNotifications(notifications)
    }

    private companion object {
        private const val TAG = "OneUiHome/badges"
    }
}
