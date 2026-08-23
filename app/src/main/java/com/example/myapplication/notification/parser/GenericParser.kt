package com.example.localledger.notification.parser

import com.example.localledger.data.database.TransactionType
import com.example.localledger.notification.NotificationNormalizer

open class GenericParser : PaymentParser {
    private val currencyAmountPattern = Regex("(?:¥|￥)\\s*(\\d+(?:[.,]\\d{1,2})?)")
    private val yuanAmountPattern = Regex("(\\d+(?:[.,]\\d{1,2})?)\\s*元")
    private val explicitAmountPattern = Regex("(?:金额|实付|应付|合计|收款金额|到账金额)\\s*[:：]?\\s*(?:¥|￥)?\\s*(\\d+(?:[.,]\\d{1,2})?)")
    private val decimalPaymentPattern = Regex("(?:支付|付款|收款|到账|消费|扣款|转账)[^0-9]{0,8}(\\d+[.,]\\d{1,2})")

    override fun parse(title: String?, text: String?, packageName: String): ParsedPayment? {
        val normalized = NotificationNormalizer.normalize(listOfNotNull(title, text).joinToString(" "))
        val paymentSignal = listOf("支付", "收款", "到账", "退款", "转账", "付款", "消费", "扣款")
            .any { normalized.contains(it) }
        if (!paymentSignal) return null
        val amount = extractAmount(normalized)
            ?: return null
        val isIncome = normalized.contains("收款") || normalized.contains("到账") || normalized.contains("收入")
        val isRefund = normalized.contains("退款")
        val merchant = Regex("(?:收款方|付款方|商户|向)[:： ]*([^，,。\\n]+)").find(normalized)?.groupValues?.get(1)?.trim()
        return ParsedPayment(
            amountMinor = amount.movePointRight(2).longValueExact(),
            merchant = merchant,
            transactionType = when {
                isRefund -> TransactionType.REFUND
                isIncome -> TransactionType.INCOME
                else -> TransactionType.EXPENSE
            },
            paymentMethod = packageName,
            confidence = if (merchant != null) 75 else 55
        )
    }

    private fun extractAmount(normalized: String): java.math.BigDecimal? {
        val raw = explicitAmountPattern.find(normalized)?.groupValues?.get(1)
            ?: yuanAmountPattern.find(normalized)?.groupValues?.get(1)
            ?: currencyAmountPattern.findAll(normalized).lastOrNull()?.groupValues?.get(1)
            ?: decimalPaymentPattern.find(normalized)?.groupValues?.get(1)
            ?: return null
        return raw.replace(",", ".").toBigDecimalOrNull()
    }
}
