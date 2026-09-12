package com.example.localledger.notification

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.room.withTransaction
import com.example.localledger.data.database.AccountEntity
import com.example.localledger.data.database.AppDatabase
import com.example.localledger.data.database.CategoryEntity
import com.example.localledger.data.database.TransactionCandidateEntity
import com.example.localledger.data.database.TransactionEntity
import com.example.localledger.data.database.TransactionStatus
import com.example.localledger.classification.CategoryEngine
import com.example.localledger.notification.parser.ParsedPayment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.ArrayDeque

class NotificationProcessor(private val context: Context, private val database: AppDatabase) {
    private data class PersistResult(
        val status: TransactionStatus,
        val candidateId: String?,
        val shouldNotify: Boolean
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sourceDetector = SourceDetector()
    private val categoryEngine = CategoryEngine()
    private val accessibilityNotificationTimes = ArrayDeque<Long>()

    fun process(notification: StatusBarNotification): ParsedPayment? {
        val extras = notification.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = buildString {
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let(::append)
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let {
                if (isNotEmpty()) append('\n')
                append(it)
            }
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n")?.let {
                if (isNotEmpty()) append('\n')
                append(it)
            }
        }.ifBlank { null }
        return processText(
            notification.packageName,
            title,
            text,
            notification.postTime,
            notification.key,
            "NOTIFICATION",
            notification.postTime / 60_000L
        )
    }

    fun processText(
        packageName: String,
        title: String?,
        text: String?,
        timestamp: Long,
        sourceId: String,
        source: String,
        deduplicationBucket: Long = timestamp / 60_000L,
        allowCreate: Boolean = true
    ): ParsedPayment? {
        if (source == "ACCESSIBILITY" && !hasCompletedTransactionSignal(title, text)) return null
        val parser = sourceDetector.parserFor(packageName)
        val parsed = parser.parse(title, text, packageName) ?: return null
        val stableSourceId = if (source == "ACCESSIBILITY") "ACCESSIBILITY" else sourceId
        val key = hashKey(
            packageName = packageName,
            sourceId = stableSourceId,
            deduplicationBucket = deduplicationBucket,
            parsed = parsed,
            includeMerchant = source != "ACCESSIBILITY"
        )
        val appLabel = sourceAppLabel(packageName)
        scope.launch {
            val result = persist(packageName, parsed, timestamp, source, key, allowCreate) ?: return@launch
            if (!result.shouldNotify) return@launch
            if (source == "ACCESSIBILITY" && !allowAccessibilityNotification(System.currentTimeMillis())) return@launch
            LedgerNotificationNotifier.postRecorded(
                context = context,
                amountMinor = parsed.amountMinor,
                merchant = parsed.merchant,
                type = parsed.transactionType,
                status = result.status,
                notificationId = key.hashCode(),
                requestPromotion = true,
                paymentMethod = parsed.paymentMethod,
                appLabel = appLabel,
                candidateId = result.candidateId
            )
        }
        return parsed
    }

    private suspend fun persist(packageName: String, parsed: ParsedPayment, timestamp: Long, source: String, key: String, allowCreate: Boolean): PersistResult? {
        return database.withTransaction {
            // 统一去重：同一笔真实交易可能先被通知监听捕获，再被无障碍读取，或确认后再次被重新读取。
            // 只要在窗口内存在金额、类型、商户（可为空）都匹配的既存账单/候选，就只补全信息并静默，
            // 不再重复入库，也不重复推通知。被忽略的候选也会静默，避免反复打扰。
            val from = timestamp - 10 * 60_000L
            val to = timestamp + 10 * 60_000L
            val recentCandidate = database.candidateDao().findRecentMatching(
                packageName, parsed.amountMinor, parsed.transactionType, parsed.merchant, from, to
            )
            if (recentCandidate != null) {
                if (recentCandidate.merchant.isNullOrBlank() && !parsed.merchant.isNullOrBlank()) {
                    database.candidateDao().fillMerchantById(recentCandidate.id, parsed.merchant)
                }
                val shouldStayPending = recentCandidate.status == TransactionStatus.PENDING
                return@withTransaction PersistResult(
                    status = if (shouldStayPending) TransactionStatus.PENDING else TransactionStatus.CONFIRMED,
                    candidateId = if (shouldStayPending) recentCandidate.id else null,
                    shouldNotify = false
                )
            }
            val recentTransaction = database.transactionDao().findRecentMatching(
                packageName, parsed.amountMinor, parsed.transactionType, parsed.merchant, from, to
            )
            if (recentTransaction != null) {
                if (recentTransaction.merchant.isNullOrBlank() && !parsed.merchant.isNullOrBlank()) {
                    database.transactionDao().fillMerchantById(recentTransaction.id, parsed.merchant)
                }
                return@withTransaction PersistResult(TransactionStatus.CONFIRMED, null, shouldNotify = false)
            }
            if (!allowCreate) return@withTransaction null
            if (database.candidateDao().findByDeduplicationKey(key) != null ||
                database.transactionDao().findByDeduplicationKey(key) != null
            ) return@withTransaction null

            val accountId = "auto-${parsed.paymentMethod ?: packageName}"
            val rule = parsed.merchant?.let { database.merchantRuleDao().findByMerchant(it) }
            val categoryDecision = categoryEngine.decide(parsed.merchant, rule)
            val categoryId = rule?.categoryId ?: "category-${categoryDecision.categoryId}"
            database.accountDao().upsert(AccountEntity(id = accountId, name = accountName(parsed.paymentMethod)))
            database.categoryDao().upsert(CategoryEntity(id = categoryId, name = rule?.categoryId?.let { categoryDecision.categoryId } ?: categoryDecision.categoryId))

            // 微信/支付宝已深度适配：金额+方向解析成功即自动记账，保持无感；
            // 其它来源（银行、短信等）仍需要高置信且信息完整，否则进待确认，避免误记。
            val isAdapted = parsed.paymentMethod != null && parsed.paymentMethod in setOf("WECHAT", "ALIPAY")
            val autoConfirmThreshold = if (isAdapted) 70 else 80
            val canAutoConfirm = isAdapted && parsed.confidence >= autoConfirmThreshold
            val status = if (canAutoConfirm) TransactionStatus.CONFIRMED else TransactionStatus.PENDING
            val candidate = TransactionCandidateEntity(
                amountMinor = parsed.amountMinor,
                currency = parsed.currency,
                type = parsed.transactionType,
                merchant = parsed.merchant,
                accountId = accountId,
                categoryId = categoryId,
                timestamp = timestamp,
                source = source,
                sourcePackage = packageName,
                confidence = parsed.confidence,
                deduplicationKey = key,
                status = status
            )
            database.candidateDao().upsert(candidate)

            if (status == TransactionStatus.CONFIRMED) {
                database.transactionDao().upsert(
                    TransactionEntity(
                        amountMinor = parsed.amountMinor,
                        currency = parsed.currency,
                        type = parsed.transactionType,
                        status = TransactionStatus.CONFIRMED,
                        accountId = accountId,
                        merchant = parsed.merchant,
                        categoryId = categoryId,
                        timestamp = timestamp,
                        source = source,
                        sourcePackage = packageName,
                        confidence = parsed.confidence,
                        deduplicationKey = key
                    )
                )
            }
            PersistResult(status, if (status == TransactionStatus.PENDING) candidate.id else null, shouldNotify = true)
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun allowAccessibilityNotification(now: Long): Boolean {
        synchronized(accessibilityNotificationTimes) {
            while (accessibilityNotificationTimes.isNotEmpty() && now - accessibilityNotificationTimes.peekFirst() >= 10_000L) {
                accessibilityNotificationTimes.removeFirst()
            }
            if (accessibilityNotificationTimes.size >= 3) return false
            accessibilityNotificationTimes.addLast(now)
            return true
        }
    }

    private fun hashKey(
        packageName: String,
        sourceId: String,
        deduplicationBucket: Long,
        parsed: ParsedPayment,
        includeMerchant: Boolean
    ): String {
        val raw = listOf(
            packageName,
            sourceId,
            parsed.amountMinor,
            if (includeMerchant) parsed.merchant else null,
            parsed.transactionType,
            deduplicationBucket
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun accountName(method: String?): String = when (method) {
        "WECHAT" -> "微信钱包"
        "ALIPAY" -> "支付宝余额"
        else -> "待确认账户"
    }

    private fun hasCompletedTransactionSignal(title: String?, text: String?): Boolean {
        val value = listOfNotNull(title, text).joinToString(" ")
        return listOf(
            "支付成功", "付款成功", "交易成功", "收款成功", "退款成功", "转账成功",
            "实付", "应付", "到账金额", "扣款成功", "已支付", "已收款"
        ).any { value.contains(it) }
    }

    private fun sourceAppLabel(packageName: String): String = when (packageName) {
        SourceDetector.WECHAT_PACKAGE -> "微信"
        SourceDetector.ALIPAY_PACKAGE -> "支付宝"
        else -> "支付应用"
    }
}
