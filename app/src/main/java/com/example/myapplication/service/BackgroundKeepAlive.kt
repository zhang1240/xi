package com.example.localledger.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BackgroundKeepAlive {
    private const val HEARTBEAT_REQUEST_CODE = 7301
    const val HEARTBEAT_INTERVAL_MS = 15 * 60_000L

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        val pm = context.getSystemService(PowerManager::class.java) ?: return
        if (pm.isIgnoringBatteryOptimizations(context.packageName)) return
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:" + context.packageName)
                }
            )
        } catch (_: Throwable) {
            // 一些 ROM 可能拒绝该 intent，回退到应用详情页。
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:" + context.packageName)
                    }
                )
            } catch (_: Throwable) {
                // 忽略。
            }
        }
    }

    fun scheduleHeartbeat(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, LedgerHeartbeatReceiver::class.java).apply {
            action = LedgerHeartbeatReceiver.ACTION_HEARTBEAT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            HEARTBEAT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL_MS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelHeartbeat(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, LedgerHeartbeatReceiver::class.java).apply {
            action = LedgerHeartbeatReceiver.ACTION_HEARTBEAT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            HEARTBEAT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
