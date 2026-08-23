package com.example.localledger.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import com.example.localledger.notification.LedgerNotificationNotifier
import com.example.localledger.LocalLedgerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LedgerForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        try {
            LedgerNotificationNotifier.createChannel(this)
            val notification = LedgerNotificationNotifier.buildServiceNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(SERVICE_NOTIFICATION_ID, notification)
            }
        } catch (_: RuntimeException) {
            CoroutineScope(Dispatchers.IO).launch {
                (application as LocalLedgerApplication).preferences.setForegroundServiceEnabled(false)
            }
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val SERVICE_NOTIFICATION_ID = 7101
    }
}
