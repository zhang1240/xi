package com.example.localledger.notification.parser

import com.example.localledger.data.database.TransactionType
import com.example.localledger.notification.NotificationNormalizer

class AlipayParser : GenericParser() {
    override fun parse(title: String?, text: String?, packageName: String): ParsedPayment? {
        val normalized = NotificationNormalizer.normalize(listOfNotNull(title, text).joinToString(" "))
        val parsed = super.parse(title, text, packageName) ?: return null
        val isExpense = normalized.contains("支出") || normalized.contains("付款") && normalized.contains("你有一笔")
        val isIncome = normalized.contains("收款通知") || normalized.contains("向你付款") || normalized.contains("收款")
        val merchant = Regex("^(.+?)通过扫码向你付款").find(normalized)?.groupValues?.get(1)?.trim()
        return parsed.copy(
            merchant = merchant,
            transactionType = when {
                isExpense -> TransactionType.EXPENSE
                isIncome -> TransactionType.INCOME
                else -> parsed.transactionType
            },
            paymentMethod = "ALIPAY",
            confidence = if (merchant != null) 90 else 70
        )
    }
}
