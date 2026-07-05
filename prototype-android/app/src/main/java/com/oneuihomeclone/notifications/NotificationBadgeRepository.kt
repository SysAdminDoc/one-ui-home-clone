package com.oneuihomeclone.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationBadgeRepository {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    fun updateFromActiveNotifications(notifications: Array<StatusBarNotification>?) {
        _counts.value = notifications
            .orEmpty()
            .asSequence()
            .map(StatusBarNotification::getPackageName)
            .filter(String::isNotBlank)
            .groupingBy { it }
            .eachCount()
    }

    fun clear() {
        _counts.value = emptyMap()
    }
}

fun isNotificationBadgeAccessGranted(context: Context): Boolean {
    val appContext = context.applicationContext
    val listener = notificationListenerComponent(appContext)
    val enabledListeners = Settings.Secure.getString(
        appContext.contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()
    return enabledListeners
        .split(':')
        .mapNotNull(ComponentName::unflattenFromString)
        .any { component ->
            component.packageName == listener.packageName &&
                component.className == listener.className
        }
}

fun notificationBadgeSettingsIntent(): Intent =
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun notificationListenerComponent(context: Context): ComponentName =
    ComponentName(context.applicationContext, LauncherNotificationListener::class.java)
