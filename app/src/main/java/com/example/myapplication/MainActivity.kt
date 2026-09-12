package com.example.localledger

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.localledger.ui.screens.HomeScreen
import com.example.localledger.ui.screens.PendingScreen
import com.example.localledger.ui.screens.SettingsScreen
import com.example.localledger.ui.screens.StatisticsScreen
import com.example.localledger.ui.screens.TransactionsScreen
import com.example.localledger.ui.theme.LocalLedgerTheme
import com.example.localledger.data.database.AppDatabase
import com.example.localledger.data.preferences.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class AppDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val destinations = listOf(
    AppDestination("home", "首页", Icons.Default.Home),
    AppDestination("transactions", "账单", Icons.Default.ReceiptLong),
    AppDestination("statistics", "统计", Icons.Default.BarChart),
    AppDestination("pending", "待确认", Icons.Default.List),
    AppDestination("settings", "设置", Icons.Default.Settings)
)

class MainActivity : FragmentActivity() {
    private val localLedgerApplication get() = application as LocalLedgerApplication
    private var authenticating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LocalLedgerApp(localLedgerApplication.database, localLedgerApplication.preferences) }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            if (localLedgerApplication.preferences.appLockEnabled.first()) authenticate()
        }
    }

    private fun authenticate() {
        if (authenticating) return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            finish()
            return
        }
        authenticating = true
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authenticating = false
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authenticating = false
                finish()
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("解锁 LocalLedger")
                .setSubtitle("验证后查看本地账本")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }
}

@Composable
private fun LocalLedgerApp(database: AppDatabase, preferences: AppPreferences) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LocalLedgerTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        paddingValues = paddingValues,
                        database = database,
                        preferences = preferences,
                        onOpenPending = { navController.navigate("pending") },
                        onOpenTransactions = { navController.navigate("transactions") }
                    )
                }
                composable("pending") {
                    PendingScreen(
                        paddingValues = paddingValues,
                        database = database,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("transactions") { TransactionsScreen(paddingValues, database) }
                composable("statistics") { StatisticsScreen(paddingValues, database) }
                composable("settings") { SettingsScreen(paddingValues, database, preferences) }
            }
        }
    }
}
