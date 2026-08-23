package com.example.localledger.notification.parser

import com.example.localledger.data.database.TransactionType

data class ParsedPayment(
    val amountMinor: Long,
    val currency: String = "CNY",
    val merchant: String?,
    val transactionType: TransactionType,
    val paymentMethod: String?,
    val timestamp: Long? = null,
    val confidence: Int
)

interface PaymentParser {
    fun parse(title: String?, text: String?, packageName: String): ParsedPayment?
}
