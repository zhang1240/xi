package com.example.localledger.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.localledger.data.database.AccountEntity
import com.example.localledger.data.database.AppDatabase
import com.example.localledger.data.database.CategoryEntity
import com.example.localledger.data.database.MerchantRuleEntity
import com.example.localledger.data.database.TransactionCandidateEntity
import com.example.localledger.data.database.TransactionEntity
import com.example.localledger.data.database.TransactionStatus
import com.example.localledger.data.database.TransactionType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupManager(private val context: Context, private val database: AppDatabase) {
    suspend fun export(uri: Uri, password: CharArray) {
        val payload = serialize()
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(payload)
        context.contentResolver.openOutputStream(uri)?.use { output ->
            DataOutputStream(output).use { data ->
                data.writeUTF("LOCALLEDGER")
                data.writeInt(1)
                data.writeInt(salt.size)
                data.write(salt)
                data.writeInt(iv.size)
                data.write(iv)
                data.writeInt(encrypted.size)
                data.write(encrypted)
            }
        } ?: error("无法打开备份文件")
    }

    suspend fun import(uri: Uri, password: CharArray) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("无法读取备份文件")
        require(bytes.size <= MAX_BACKUP_BYTES) { "备份文件过大" }
        val data = DataInputStream(ByteArrayInputStream(bytes))
        check(data.readUTF() == "LOCALLEDGER") { "不是 LocalLedger 备份文件" }
        check(data.readInt() == 1) { "不支持的备份版本" }
        val saltSize = data.readInt().also { require(it == 16) { "备份盐值长度无效" } }
        val salt = ByteArray(saltSize).also(data::readFully)
        val ivSize = data.readInt().also { require(it == 12) { "备份随机向量长度无效" } }
        val iv = ByteArray(ivSize).also(data::readFully)
        val encryptedSize = data.readInt().also { require(it in 1..MAX_BACKUP_BYTES) { "备份数据长度无效" } }
        val encrypted = ByteArray(encryptedSize).also(data::readFully)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        deserialize(cipher.doFinal(encrypted))
    }

    private suspend fun serialize(): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            val accounts = database.accountDao().getAll()
            data.writeInt(accounts.size)
            accounts.forEach { data.writeAccount(it) }
            val categories = database.categoryDao().getAll()
            data.writeInt(categories.size)
            categories.forEach { data.writeCategory(it) }
            val transactions = database.transactionDao().getAll()
            data.writeInt(transactions.size)
            transactions.forEach { data.writeTransaction(it) }
            val candidates = database.candidateDao().getAll()
            data.writeInt(candidates.size)
            candidates.forEach { data.writeCandidate(it) }
            val rules = database.merchantRuleDao().getAll()
            data.writeInt(rules.size)
            rules.forEach { data.writeRule(it) }
        }
        return output.toByteArray()
    }

    private suspend fun deserialize(payload: ByteArray) {
        val data = DataInputStream(ByteArrayInputStream(payload))
        database.withTransaction {
            repeat(readBoundedCount(data.readInt())) { database.accountDao().upsert(data.readAccount()) }
            repeat(readBoundedCount(data.readInt())) { database.categoryDao().upsert(data.readCategory()) }
            repeat(readBoundedCount(data.readInt())) { database.transactionDao().upsert(data.readTransaction()) }
            repeat(readBoundedCount(data.readInt())) { database.candidateDao().upsert(data.readCandidate()) }
            repeat(readBoundedCount(data.readInt())) { database.merchantRuleDao().upsert(data.readRule()) }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, 120_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private fun readBoundedCount(value: Int): Int = value.also { require(it in 0..100_000) { "备份记录数量无效" } }

    companion object {
        private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024
    }
}

private fun DataOutputStream.writeNullable(value: String?) { writeBoolean(value != null); if (value != null) writeUTF(value) }
private fun DataInputStream.readNullable(): String? = if (readBoolean()) readUTF() else null
private fun DataOutputStream.writeAccount(value: AccountEntity) { writeUTF(value.id); writeUTF(value.name); writeUTF(value.currency); writeLong(value.openingBalanceMinor); writeBoolean(value.isArchived) }
private fun DataInputStream.readAccount() = AccountEntity(readUTF(), readUTF(), readUTF(), readLong(), readBoolean())
private fun DataOutputStream.writeCategory(value: CategoryEntity) { writeUTF(value.id); writeUTF(value.name); writeNullable(value.parentId); writeNullable(value.icon); writeBoolean(value.isArchived) }
private fun DataInputStream.readCategory() = CategoryEntity(readUTF(), readUTF(), readNullable(), readNullable(), readBoolean())
private fun DataOutputStream.writeTransaction(value: TransactionEntity) { writeUTF(value.id); writeLong(value.amountMinor); writeUTF(value.currency); writeUTF(value.type.name); writeUTF(value.status.name); writeNullable(value.accountId); writeNullable(value.toAccountId); writeNullable(value.merchant); writeNullable(value.categoryId); writeLong(value.timestamp); writeUTF(value.source); writeNullable(value.sourcePackage); writeNullable(value.confidence?.toString()); writeNullable(value.note); writeNullable(value.deduplicationKey); writeNullable(value.relatedTransactionId); writeLong(value.createdAt); writeLong(value.updatedAt) }
private fun DataInputStream.readTransaction() = TransactionEntity(
    id = readUTF(), amountMinor = readLong(), currency = readUTF(), type = TransactionType.valueOf(readUTF()), status = TransactionStatus.valueOf(readUTF()),
    accountId = readNullable(), toAccountId = readNullable(), merchant = readNullable(), categoryId = readNullable(), timestamp = readLong(), source = readUTF(),
    sourcePackage = readNullable(), confidence = readNullable()?.toIntOrNull(), note = readNullable(), deduplicationKey = readNullable(), relatedTransactionId = readNullable(),
    createdAt = readLong(), updatedAt = readLong()
)
private fun DataOutputStream.writeCandidate(value: TransactionCandidateEntity) { writeUTF(value.id); writeLong(value.amountMinor); writeUTF(value.currency); writeUTF(value.type.name); writeNullable(value.merchant); writeNullable(value.accountId); writeNullable(value.categoryId); writeLong(value.timestamp); writeUTF(value.source); writeNullable(value.sourcePackage); writeInt(value.confidence); writeUTF(value.deduplicationKey); writeUTF(value.status.name); writeLong(value.createdAt) }
private fun DataInputStream.readCandidate() = TransactionCandidateEntity(readUTF(), readLong(), readUTF(), TransactionType.valueOf(readUTF()), readNullable(), readNullable(), readNullable(), readLong(), readUTF(), readNullable(), readInt(), readUTF(), TransactionStatus.valueOf(readUTF()), readLong())
private fun DataOutputStream.writeRule(value: MerchantRuleEntity) { writeUTF(value.id); writeUTF(value.merchant); writeNullable(value.categoryId); writeNullable(value.accountId); writeLong(value.createdAt); writeLong(value.updatedAt) }
private fun DataInputStream.readRule() = MerchantRuleEntity(readUTF(), readUTF(), readNullable(), readNullable(), readLong(), readLong())
