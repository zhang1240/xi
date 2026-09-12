package com.example.localledger.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM transactions WHERE sourcePackage = :sourcePackage AND timestamp BETWEEN :from AND :to AND amountMinor = :amountMinor LIMIT 1")
    suspend fun findRecent(sourcePackage: String, from: Long, to: Long, amountMinor: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE deduplicationKey = :key LIMIT 1")
    suspend fun findByDeduplicationKey(key: String): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp")
    suspend fun getAll(): List<TransactionEntity>

    @Query("UPDATE transactions SET merchant = :merchant WHERE id = :id")
    suspend fun fillMerchantById(id: String, merchant: String?): Int

    @Query("UPDATE transactions SET merchant = :merchant WHERE sourcePackage = :sourcePackage AND amountMinor = :amountMinor AND timestamp BETWEEN :from AND :to AND merchant IS NULL")
    suspend fun fillMerchantForRecent(sourcePackage: String, amountMinor: Long, from: Long, to: Long, merchant: String): Int

    @Query("SELECT * FROM transactions WHERE sourcePackage = :sourcePackage AND amountMinor = :amountMinor AND timestamp BETWEEN :from AND :to ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentBySourceAmount(sourcePackage: String, amountMinor: Long, from: Long, to: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE sourcePackage = :sourcePackage AND amountMinor = :amountMinor AND type = :type AND timestamp BETWEEN :from AND :to AND (merchant = :merchant OR merchant IS NULL OR :merchant IS NULL) ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentMatching(sourcePackage: String, amountMinor: Long, type: TransactionType, merchant: String?, from: Long, to: Long): TransactionEntity?

}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY name")
    fun observeActive(): Flow<List<AccountEntity>>

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE name = :name AND isArchived = 0 LIMIT 1")
    suspend fun findByName(name: String): AccountEntity?

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :id")
    suspend fun archiveById(id: String)

    @Query("SELECT * FROM accounts ORDER BY name")
    suspend fun getAll(): List<AccountEntity>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY name")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE name = :name AND isArchived = 0 LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("UPDATE categories SET isArchived = 1 WHERE id = :id")
    suspend fun archiveById(id: String)

    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun getAll(): List<CategoryEntity>
}

@Dao
interface CandidateDao {
    @Query("SELECT * FROM transaction_candidates WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun observePending(): Flow<List<TransactionCandidateEntity>>

    @Upsert
    suspend fun upsert(candidate: TransactionCandidateEntity)

    @Query("SELECT * FROM transaction_candidates WHERE deduplicationKey = :key LIMIT 1")
    suspend fun findByDeduplicationKey(key: String): TransactionCandidateEntity?

    @Query("UPDATE transaction_candidates SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: TransactionStatus)

    @Query("SELECT * FROM transaction_candidates ORDER BY timestamp")
    suspend fun getAll(): List<TransactionCandidateEntity>

    @Query("UPDATE transaction_candidates SET merchant = :merchant WHERE id = :id")
    suspend fun fillMerchantById(id: String, merchant: String?): Int

    @Query("UPDATE transaction_candidates SET merchant = :merchant WHERE sourcePackage = :sourcePackage AND amountMinor = :amountMinor AND timestamp BETWEEN :from AND :to AND merchant IS NULL")
    suspend fun fillMerchantForRecent(sourcePackage: String, amountMinor: Long, from: Long, to: Long, merchant: String): Int

    @Query("SELECT * FROM transaction_candidates WHERE sourcePackage = :sourcePackage AND amountMinor = :amountMinor AND timestamp BETWEEN :from AND :to ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentBySourceAmount(sourcePackage: String, amountMinor: Long, from: Long, to: Long): TransactionCandidateEntity?

    @Query("SELECT * FROM transaction_candidates WHERE sourcePackage = :sourcePackage AND amountMinor = :amountMinor AND type = :type AND timestamp BETWEEN :from AND :to AND (merchant = :merchant OR merchant IS NULL OR :merchant IS NULL) ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentMatching(sourcePackage: String, amountMinor: Long, type: TransactionType, merchant: String?, from: Long, to: Long): TransactionCandidateEntity?

    @Query("SELECT * FROM transaction_candidates WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TransactionCandidateEntity?

}

@Dao
interface MerchantRuleDao {
    @Query("SELECT * FROM merchant_rules ORDER BY merchant")
    fun observeAll(): Flow<List<MerchantRuleEntity>>

    @Upsert
    suspend fun upsert(rule: MerchantRuleEntity)

    @Query("SELECT * FROM merchant_rules ORDER BY merchant")
    suspend fun getAll(): List<MerchantRuleEntity>

    @Query("SELECT * FROM merchant_rules WHERE merchant = :merchant LIMIT 1")
    suspend fun findByMerchant(merchant: String): MerchantRuleEntity?

    @Query("DELETE FROM merchant_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}
