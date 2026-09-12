package com.example.localledger

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.localledger.data.database.AppDatabase
import com.example.localledger.data.preferences.AppPreferences
import com.example.localledger.accessibility.AccessibilityPermission
import com.example.localledger.notification.LedgerNotificationNotifier
import com.example.localledger.service.LedgerForegroundService
import com.example.localledger.service.BackgroundKeepAlive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LocalLedgerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferences: AppPreferences by lazy { AppPreferences(this) }
    @Volatile private var foregroundServiceAttempted = false

    override fun onCreate() {
        super.onCreate()
        LedgerNotificationNotifier.createChannel(this)
        tryStartForegroundService()
        scheduleHeartbeat()
    }

    private fun tryStartForegroundService() {
        if (foregroundServiceAttempted) return
        foregroundServiceAttempted = true
        CoroutineScope(Dispatchers.IO).launch {
            val enabled = preferences.foregroundServiceEnabled.first()
            val accessibilityOn = AccessibilityPermission.isEnabled(this@LocalLedgerApplication)
            if (!enabled && !accessibilityOn) return@launch
            try {
                ContextCompat.startForegroundService(
                    this@LocalLedgerApplication,
                    Intent(this@LocalLedgerApplication, LedgerForegroundService::class.java)
                )
            } catch (_: Throwable) {
                // Android 12+ 后台进程启动前台服务可能被系统拒绝，忽略即可。
            }
        }
    }

    private fun scheduleHeartbeat() {
        CoroutineScope(Dispatchers.IO).launch {
            val enabled = preferences.foregroundServiceEnabled.first()
            val accessibilityOn = AccessibilityPermission.isEnabled(this@LocalLedgerApplication)
            if (enabled || accessibilityOn) {
                BackgroundKeepAlive.scheduleHeartbeat(this@LocalLedgerApplication)
            }
        }
    }
}