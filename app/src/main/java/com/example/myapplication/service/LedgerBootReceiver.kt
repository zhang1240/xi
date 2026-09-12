package com.example.localledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.localledger.LocalLedgerApplication
import com.example.localledger.accessibility.AccessibilityPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LedgerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as LocalLedgerApplication
        CoroutineScope(Dispatchers.IO).launch {
            val enabled = app.preferences.foregroundServiceEnabled.first()
            val accessibilityOn = AccessibilityPermission.isEnabled(context)
            if (!enabled && !accessibilityOn) return@launch
            BackgroundKeepAlive.scheduleHeartbeat(context)
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LedgerForegroundService::class.java)
                )
            } catch (_: Throwable) {
                // 某些厂商 ROM 对开机后台启动前台服务有限制，忽略即可。
            }
        }
    }
}