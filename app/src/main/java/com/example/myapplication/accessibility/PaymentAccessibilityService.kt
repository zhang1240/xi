package com.example.localledger.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.localledger.notification.LedgerNotificationNotifier
import com.example.localledger.service.BackgroundKeepAlive
import com.example.localledger.service.LedgerForegroundService
import android.os.SystemClock
import com.example.localledger.LocalLedgerApplication
import com.example.localledger.notification.NotificationProcessor
import java.security.MessageDigest
import java.util.ArrayDeque

class PaymentAccessibilityService : AccessibilityService() {
    private val lastFingerprintByPackage = mutableMapOf<String, String>()
    private val lastProcessedAtByPackage = mutableMapOf<String, Long>()
    private val eventTimesByPackage = mutableMapOf<String, ArrayDeque<Long>>()
    private var visitedNodes = 0
    private val processor by lazy {
        NotificationProcessor(this, (application as LocalLedgerApplication).database)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in TARGET_PACKAGES) return
        // 单个窗口解析失败绝不能导致无障碍服务崩溃，否则系统会反复禁用权限。
        try {
            handleWindowText(packageName)
        } catch (_: Throwable) {
            // 忽略本次窗口，保持服务存活。
        }
    }

    private fun handleWindowText(packageName: String) {
        val root = rootInActiveWindow ?: return
        val text = extractText(root)
        val now = SystemClock.uptimeMillis()
        if (text.isNotBlank() && hasCompletedTransactionSignal(text)) {
            val textFingerprint = MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray())
                .joinToString("") { "%02x".format(it) }
            if (lastFingerprintByPackage[packageName] == textFingerprint) return
            if (now - (lastProcessedAtByPackage[packageName] ?: 0L) < EVENT_THROTTLE_MS) return
            if (!allowRate(packageName, now)) return
            lastFingerprintByPackage[packageName] = textFingerprint
            lastProcessedAtByPackage[packageName] = now
            processor.processText(
                packageName = packageName,
                title = null,
                text = text,
                timestamp = System.currentTimeMillis(),
                sourceId = "window:${root.windowId}:$textFingerprint",
                source = "ACCESSIBILITY",
                deduplicationBucket = System.currentTimeMillis() / 120_000L,
                allowCreate = true
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 无障碍连接即保活：启动前台服务并注册心跳，避免进程被回收后漏记支付事件。
        BackgroundKeepAlive.scheduleHeartbeat(this)
        try {
            ContextCompat.startForegroundService(this, Intent(this, LedgerForegroundService::class.java))
        } catch (_: Throwable) {
            // 系统可能限制后台启动前台服务，由心跳稍后重试。
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // 服务被系统解除绑定（用户关闭或厂商回收）时提醒，便于用户重新开启。
        runCatching { LedgerNotificationNotifier.postAccessibilityDisabled(this) }
        return super.onUnbind(intent)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        processor.close()
        super.onDestroy()
    }

    private fun extractText(root: AccessibilityNodeInfo): String {
        val values = ArrayList<String>(24)
        visitedNodes = 0
        collectText(root, values)
        return values.distinct().joinToString(" ").take(MAX_TEXT_LENGTH)
    }

    private fun collectText(node: AccessibilityNodeInfo, values: MutableList<String>) {
        if (visitedNodes >= MAX_TEXT_NODES) return
        visitedNodes++
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(values::add)
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(values::add)
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectText(child, values)
        }
    }

    private fun hasCompletedTransactionSignal(text: String): Boolean {
        return COMPLETED_TRANSACTION_SIGNALS.any { text.contains(it) }
    }

    private fun allowRate(packageName: String, now: Long): Boolean {
        val events = eventTimesByPackage.getOrPut(packageName) { ArrayDeque() }
        while (events.isNotEmpty() && now - events.peekFirst() >= RATE_WINDOW_MS) {
            events.removeFirst()
        }
        if (events.size >= MAX_EVENTS_PER_WINDOW) return false
        events.addLast(now)
        return true
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 6000
        private const val MAX_TEXT_NODES = 64
        private const val EVENT_THROTTLE_MS = 1_500L
        private const val RATE_WINDOW_MS = 10_000L
        private const val MAX_EVENTS_PER_WINDOW = 4
        private val COMPLETED_TRANSACTION_SIGNALS = listOf(
            "支付成功", "付款成功", "交易成功", "收款成功", "退款成功", "转账成功",
            "实付", "应付", "到账金额", "扣款成功", "已支付", "已收款"
        )
        val TARGET_PACKAGES = setOf("com.tencent.mm", "com.eg.android.AlipayGphone")
    }
}