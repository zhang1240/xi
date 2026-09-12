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

class LedgerHeartbeatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_HEARTBEAT) return
        val app = context.applicationContext as LocalLedgerApplication
        CoroutineScope(Dispatchers.IO).launch {
            val foregroundEnabled = app.preferences.foregroundServiceEnabled.first()
            val accessibilityOn = AccessibilityPermission.isEnabled(context)
            val shouldRun = foregroundEnabled || accessibilityOn
            if (shouldRun) {
                BackgroundKeepAlive.scheduleHeartbeat(context)
            } else {
                BackgroundKeepAlive.cancelHeartbeat(context)
                return@launch
            }
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LedgerForegroundService::class.java)
                )
            } catch (_: Throwable) {
                // 后台启动前台服务可能被系统临时拒绝，下一轮心跳会再次尝试。
            }
        }
    }

    companion object {
        const val ACTION_HEARTBEAT = "com.example.localledger.action.HEARTBEAT"
    }
}