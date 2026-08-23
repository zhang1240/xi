package com.example.localledger.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.localLedgerDataStore by preferencesDataStore(name = "localledger_preferences")

class AppPreferences(private val context: Context) {
    private val autoRecordKey = booleanPreferencesKey("auto_record_enabled")
    private val appLockKey = booleanPreferencesKey("app_lock_enabled")
    private val foregroundServiceKey = booleanPreferencesKey("foreground_service_enabled")
    private val monthlyBudgetMinorKey = longPreferencesKey("monthly_budget_minor")

    val autoRecordEnabled = context.localLedgerDataStore.data.map { preferences ->
        preferences[autoRecordKey] ?: true
    }

    suspend fun setAutoRecordEnabled(enabled: Boolean) {
        context.localLedgerDataStore.edit { preferences -> preferences[autoRecordKey] = enabled }
    }

    val appLockEnabled = context.localLedgerDataStore.data.map { preferences ->
        preferences[appLockKey] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.localLedgerDataStore.edit { preferences -> preferences[appLockKey] = enabled }
    }

    val foregroundServiceEnabled = context.localLedgerDataStore.data.map { preferences ->
        preferences[foregroundServiceKey] ?: false
    }

    suspend fun setForegroundServiceEnabled(enabled: Boolean) {
        context.localLedgerDataStore.edit { preferences -> preferences[foregroundServiceKey] = enabled }
    }

    val monthlyBudgetMinor = context.localLedgerDataStore.data.map { preferences ->
        preferences[monthlyBudgetMinorKey] ?: 0L
    }

    suspend fun setMonthlyBudgetMinor(value: Long) {
        context.localLedgerDataStore.edit { preferences -> preferences[monthlyBudgetMinorKey] = value.coerceAtLeast(0L) }
    }
}
