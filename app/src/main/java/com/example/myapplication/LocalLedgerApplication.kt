package com.example.localledger

import android.app.Application
import com.example.localledger.data.database.AppDatabase
import com.example.localledger.data.preferences.AppPreferences
import com.example.localledger.notification.LedgerNotificationNotifier

class LocalLedgerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferences: AppPreferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        LedgerNotificationNotifier.createChannel(this)
    }
}
