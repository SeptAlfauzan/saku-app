package com.septaalfauzan.saku.notification

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings

object NotificationAccessManager {
    fun isListening(context: Context): Boolean {
        val flat = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull() ?: return false
        return flat.split(':').any { token ->
            ComponentName.unflattenFromString(token)?.packageName == context.packageName
        }
    }

    fun openSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}
