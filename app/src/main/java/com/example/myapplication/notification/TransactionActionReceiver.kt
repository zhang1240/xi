package com.example.localledger.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.localledger.LocalLedgerApplication
import com.example.localledger.data.database.TransactionEntity
import com.example.localledger.data.database.TransactionStatus
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CONFIRM) return
        val candidateId = intent.getStringExtra(EXTRA_CANDIDATE_ID) ?: return
        val pendingResult = goAsync()
        val app = context.applicationContext as LocalLedgerApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = app.database
                val candidate = database.candidateDao().findById(candidateId) ?: return@launch
                database.withTransaction {
                    if (candidate.status == TransactionStatus.PENDING) {
                        database.transactionDao().upsert(
                            TransactionEntity(
                                id = candidate.id,
                                amountMinor = candidate.amountMinor,
                                currency = candidate.currency,
                                type = candidate.type,
                                status = TransactionStatus.CONFIRMED,
                                accountId = candidate.accountId,
                                merchant = candidate.merchant,
                                categoryId = candidate.categoryId,
                                timestamp = candidate.timestamp,
                                source = candidate.source,
                                sourcePackage = candidate.sourcePackage,
                                confidence = candidate.confidence,
                                deduplicationKey = candidate.deduplicationKey
                            )
                        )
                        database.candidateDao().updateStatus(candidate.id, TransactionStatus.CONFIRMED)
                    }
                }
                LedgerNotificationNotifier.postRecorded(
                    context = context,
                    amountMinor = candidate.amountMinor,
                    merchant = candidate.merchant,
                    type = candidate.type,
                    status = TransactionStatus.CONFIRMED,
                    notificationId = candidate.id.hashCode(),
                    requestPromotion = true,
                    paymentMethod = candidate.sourcePackage
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM = "com.example.localledger.action.CONFIRM_TRANSACTION"
        const val EXTRA_CANDIDATE_ID = "candidate_id"
    }
}
