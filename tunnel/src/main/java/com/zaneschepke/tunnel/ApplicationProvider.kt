package com.zaneschepke.tunnel

import android.app.Notification
import android.app.PendingIntent
import android.content.Context

interface ApplicationProvider {
    val vpnInitNotification: Notification
    val proxyInitNotification: Notification
    val vpnNotificationId: Int
    val proxyNotificationId: Int

    /** Provide the pending intent for VpnService system integration */
    fun createVpnConfigurePendingIntent(context: Context): PendingIntent

    fun refreshTile(context: Context)
}
