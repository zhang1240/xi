package com.example.localledger.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.SystemClock
import com.example.localledger.LocalLedgerApplication
import com.example.localledger.notification.NotificationProcessor
import java.security.MessageDigest
import java.util.ArrayDeque

class PaymentAccessibilityService : AccessibilityService() {
    private val lastFingerprintByPackage = mutableMapOf<String, String>()
    private val lastProcessedAtByPackage = mutableMapOf<String, Long>()
    private val eventTimesByPackage = mutableMapOf<String, ArrayDeque<Long>>()
    private val processor by lazy {
        NotificationProcessor(this, (application as LocalLedgerApplication).database)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in TARGET_PACKAGES) return
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
                deduplicationBucket = System.currentTimeMillis() / 300_000L,
                allowCreate = false
            )
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        processor.close()
        super.onDestroy()
    }

    private fun extractText(root: AccessibilityNodeInfo): String {
        val values = ArrayList<String>(24)
        collectText(root, values)
        return values.distinct().joinToString(" ").take(MAX_TEXT_LENGTH)
    }

    private fun collectText(node: AccessibilityNodeInfo, values: MutableList<String>) {
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(values::add)
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(values::add)
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collectText(child, values)
                child.recycle()
            }
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
