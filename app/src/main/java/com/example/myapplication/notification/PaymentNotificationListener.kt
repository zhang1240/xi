package com.example.localledger.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.localledger.LocalLedgerApplication

class PaymentNotificationListener : NotificationListenerService() {
    private val processor by lazy {
        NotificationProcessor(this, (application as LocalLedgerApplication).database)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        processor.process(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Removal does not erase an already-created candidate; deduplication handles repeats.
    }

    override fun onDestroy() {
        processor.close()
        super.onDestroy()
    }
}
