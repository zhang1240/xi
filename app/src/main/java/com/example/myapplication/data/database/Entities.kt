package com.example.localledger.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType { EXPENSE, INCOME, TRANSFER, REFUND, ADJUSTMENT }
enum class TransactionStatus { DETECTED, PENDING, CONFIRMED, EDITED, IGNORED, DUPLICATE, FAILED, ARCHIVED }

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val currency: String = "CNY",
    val openingBalanceMinor: Long = 0L,
    val isArchived: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null,
    val icon: String? = null,
    val isArchived: Boolean = false
)

@Entity(tableName = "transactions", indices = [Index(value = ["deduplicationKey"], unique = true)])
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amountMinor: Long,
    val currency: String = "CNY",
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    val accountId: String?,
    val toAccountId: String? = null,
    val merchant: String?,
    val categoryId: String?,
    val timestamp: Long,
    val source: String,
    val sourcePackage: String? = null,
    val confidence: Int? = null,
    val deduplicationKey: String? = null,
    val note: String? = null,
    val relatedTransactionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transaction_candidates", indices = [Index(value = ["deduplicationKey"], unique = true)])
data class TransactionCandidateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amountMinor: Long,
    val currency: String = "CNY",
    val type: TransactionType,
    val merchant: String?,
    val accountId: String?,
    val categoryId: String?,
    val timestamp: Long,
    val source: String,
    val sourcePackage: String?,
    val confidence: Int,
    val deduplicationKey: String,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val merchant: String,
    val categoryId: String?,
    val accountId: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
