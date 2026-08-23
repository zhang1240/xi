package com.example.localledger.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.core.app.NotificationManagerCompat
import android.content.Intent
import android.provider.Settings
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.PI
import kotlin.math.sin
import com.example.localledger.data.database.AccountEntity
import com.example.localledger.data.database.AppDatabase
import com.example.localledger.data.database.CategoryEntity
import com.example.localledger.data.database.TransactionEntity
import com.example.localledger.data.database.TransactionCandidateEntity
import com.example.localledger.data.database.TransactionStatus
import com.example.localledger.data.database.TransactionType
import com.example.localledger.data.backup.BackupManager
import com.example.localledger.data.preferences.AppPreferences
import com.example.localledger.accessibility.AccessibilityPermission
import com.example.localledger.notification.LedgerNotificationNotifier
import com.example.localledger.service.LedgerForegroundService
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth

@Composable
fun HomeScreen(paddingValues: PaddingValues, database: AppDatabase, preferences: AppPreferences) {
    val context = LocalContext.current
    val transactions by database.transactionDao().observeAll().collectAsState(initial = emptyList())
    val accounts by database.accountDao().observeActive().collectAsState(initial = emptyList())
    val accountNames = accounts.associate { it.id to it.name }
    val monthlyBudgetMinor by preferences.monthlyBudgetMinor.collectAsState(initial = 0L)
    val pendingCandidates by database.candidateDao().observePending().collectAsState(initial = emptyList())
    var showEntry by remember { mutableStateOf(false) }
    var showPaymentBreakdown by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val monthTransactions = transactions.filter { isCurrentMonth(it.timestamp) }
    val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val refunds = monthTransactions.filter { it.type == TransactionType.REFUND }.sumOf { it.amountMinor }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            LocalLedgerTopBar("LocalLedger", action = {
                IconButton(onClick = { showPaymentBreakdown = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "按支付方式查看")
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showEntry = true },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建账单")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("本月概览", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("本月支出", formatMoney(expense), Modifier.weight(1f))
                SummaryCard("本月收入", formatMoney(income + refunds), Modifier.weight(1f))
            }
            MonthlyBudgetCard(expense, monthlyBudgetMinor) { showBudgetDialog = true }
            Text("最近账单", style = MaterialTheme.typography.titleMedium)
            AnimatedVisibility(
                visible = pendingCandidates.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("待确认（${pendingCandidates.size}）", style = MaterialTheme.typography.titleMedium)
                    pendingCandidates.take(3).forEach { candidate ->
                        PendingCandidateRow(candidate, accountNames) {
                            scope.launch {
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
                                LedgerNotificationNotifier.postRecorded(
                                    context = context,
                                    amountMinor = candidate.amountMinor,
                                    merchant = candidate.merchant,
                                    type = candidate.type,
                                    status = TransactionStatus.CONFIRMED,
                                    notificationId = candidate.id.hashCode(),
                                    requestPromotion = false,
                                    paymentMethod = paymentMethodLabel(candidate.sourcePackage, candidate.accountId, accountNames[candidate.accountId])
                                )
                            }
                        }
                    }
                }
            }
            if (transactions.isEmpty()) {
                Text("暂无账单。点击右下角 + 开始手动添加。")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(232.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions.take(20), key = { it.id }) { transaction ->
                        TransactionRow(transaction, accountNames)
                    }
                }
            }
        }
    }
    if (showEntry) {
        ManualEntryDialog(database = database, onDismiss = { showEntry = false })
    }
    if (showPaymentBreakdown) {
        PaymentMethodBreakdownSheet(monthTransactions, accountNames) { showPaymentBreakdown = false }
    }
    if (showBudgetDialog) {
        MonthlyBudgetDialog(
            currentBudgetMinor = monthlyBudgetMinor,
            onDismiss = { showBudgetDialog = false },
            onSave = { value ->
                scope.launch { preferences.setMonthlyBudgetMinor(value) }
                showBudgetDialog = false
            }
        )
    }
}

@Composable
fun TransactionsScreen(paddingValues: PaddingValues, database: AppDatabase) {
    val transactions by database.transactionDao().observeAll().collectAsState(initial = emptyList())
    val accounts by database.accountDao().observeActive().collectAsState(initial = emptyList())
    val accountNames = accounts.associate { it.id to it.name }
    var showEntry by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = { LocalLedgerTopBar("账单") }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("全部账单", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { showEntry = true }) { Text("新建") }
            }
            if (transactions.isEmpty()) {
                Text("还没有交易记录。")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionRow(transaction, accountNames) { transactionToDelete = transaction }
                    }
                }
            }
        }
    }
    if (showEntry) {
        ManualEntryDialog(database = database, onDismiss = { showEntry = false })
    }
    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("删除这笔账单？") },
            text = { Text("删除后不会影响账户和分类设置。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { database.transactionDao().deleteById(transaction.id) }
                    transactionToDelete = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { transactionToDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
fun StatisticsScreen(paddingValues: PaddingValues, database: AppDatabase) {
    val transactions by database.transactionDao().observeAll().collectAsState(initial = emptyList())
    val accounts by database.accountDao().observeActive().collectAsState(initial = emptyList())
    val accountNames = accounts.associate { it.id to it.name }
    val monthTransactions = transactions.filter { isCurrentMonth(it.timestamp) }
    val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val refunds = monthTransactions.filter { it.type == TransactionType.REFUND }.sumOf { it.amountMinor }
    var showPaymentBreakdown by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.padding(paddingValues), topBar = {
        LocalLedgerTopBar("统计", action = {
            IconButton(onClick = { showPaymentBreakdown = true }) {
                Icon(Icons.Default.Tune, contentDescription = "按支付方式查看")
            }
        })
    }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("本月统计", style = MaterialTheme.typography.titleLarge)
            SummaryCard("收入与退款", formatMoney(income + refunds), Modifier.fillMaxWidth())
            SummaryCard("支出", formatMoney(expense), Modifier.fillMaxWidth())
            Text("当前共有 ${monthTransactions.size} 笔本月交易。")
        }
    }
    if (showPaymentBreakdown) {
        PaymentMethodBreakdownSheet(monthTransactions, accountNames) { showPaymentBreakdown = false }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(paddingValues: PaddingValues, database: AppDatabase, preferences: AppPreferences) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var listenerEnabled by remember { mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) }
    var accessibilityEnabled by remember { mutableStateOf(AccessibilityPermission.isEnabled(context)) }
    var notificationsEnabled by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var backupAction by remember { mutableStateOf<BackupAction?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val backupManager = remember(database) { BackupManager(context, database) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsEnabled = granted }
    val appLockEnabled by preferences.appLockEnabled.collectAsState(initial = false)
    val foregroundServiceEnabled by preferences.foregroundServiceEnabled.collectAsState(initial = false)
    val accounts by database.accountDao().observeActive().collectAsState(initial = emptyList())
    val categories by database.categoryDao().observeActive().collectAsState(initial = emptyList())
    val merchantRules by database.merchantRuleDao().observeAll().collectAsState(initial = emptyList())
    var accountDraft by remember { mutableStateOf("") }
    var categoryDraft by remember { mutableStateOf("") }
    var ruleMerchantDraft by remember { mutableStateOf("") }
    var ruleCategoryDraft by remember { mutableStateOf("") }
    var ruleAccountDraft by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null && backupPassword.isNotBlank()) {
            val password = backupPassword.toCharArray()
            scope.launch {
                runCatching { backupManager.export(uri, password) }
                    .onSuccess { backupMessage = "备份已保存" }
                    .onFailure { backupMessage = "备份失败：${it.message ?: "未知错误"}" }
                password.fill('\u0000')
                backupPassword = ""
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && backupPassword.isNotBlank()) {
            val password = backupPassword.toCharArray()
            scope.launch {
                runCatching { backupManager.import(uri, password) }
                    .onSuccess { backupMessage = "备份已恢复" }
                    .onFailure { backupMessage = "恢复失败：密码错误或文件损坏" }
                password.fill('\u0000')
                backupPassword = ""
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                accessibilityEnabled = AccessibilityPermission.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Scaffold(modifier = Modifier.padding(paddingValues), topBar = { LocalLedgerTopBar("设置") }) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("设置", style = MaterialTheme.typography.titleLarge)
            Text(if (listenerEnabled) "通知监听：已开启" else "通知监听：未授权")
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                Text(if (listenerEnabled) "管理通知监听权限" else "开启通知监听权限")
            }
            Text("收到微信或支付宝通知后，高置信度交易会自动记账，低置信度交易进入待确认。")
            Text(if (notificationsEnabled) "记账通知：已允许" else "记账通知：未允许")
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsEnabled) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }
            }) { Text(if (notificationsEnabled) "管理记账通知" else "允许记账通知") }
            Button(onClick = {
                val notificationId = (System.currentTimeMillis() and 0x7fffffff).toInt()
                LedgerNotificationNotifier.postProcessing(
                    context = context,
                    notificationId = notificationId,
                    requestPromotion = true,
                    appLabel = "LocalLedger"
                )
                scope.launch {
                    delay(800L)
                    LedgerNotificationNotifier.postRecorded(
                        context = context,
                        amountMinor = 1000L,
                        merchant = "实况通知测试",
                        type = TransactionType.EXPENSE,
                        status = TransactionStatus.CONFIRMED,
                        notificationId = notificationId,
                        requestPromotion = true,
                        paymentMethod = "MANUAL",
                        appLabel = "LocalLedger"
                    )
                }
            }) { Text("尝试实况通知") }
            Text(if (accessibilityEnabled) "无障碍备选：已开启" else "无障碍备选：未开启")
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                Text(if (accessibilityEnabled) "管理无障碍权限" else "开启无障碍备选")
            }
            Text("默认关闭。仅补全已有通知交易中的商户名，不单独新建账单。")
            Text("账户与分类", style = MaterialTheme.typography.titleMedium)
            ManagementInput(
                value = accountDraft,
                onValueChange = { accountDraft = it },
                label = "新增账户",
                onAdd = {
                    val name = accountDraft.trim()
                    if (name.isNotBlank()) scope.launch {
                        if (database.accountDao().findByName(name) == null) {
                            database.accountDao().upsert(AccountEntity(name = name))
                        }
                        accountDraft = ""
                    }
                }
            )
            accounts.forEach { account ->
                ManagementRow(account.name) { scope.launch { database.accountDao().archiveById(account.id) } }
            }
            ManagementInput(
                value = categoryDraft,
                onValueChange = { categoryDraft = it },
                label = "新增分类",
                onAdd = {
                    val name = categoryDraft.trim()
                    if (name.isNotBlank()) scope.launch {
                        if (database.categoryDao().findByName(name) == null) {
                            database.categoryDao().upsert(CategoryEntity(name = name))
                        }
                        categoryDraft = ""
                    }
                }
            )
            categories.forEach { category ->
                ManagementRow(category.name) { scope.launch { database.categoryDao().archiveById(category.id) } }
            }
            Text("商户规则", style = MaterialTheme.typography.titleMedium)
            ManagementInput(
                value = ruleMerchantDraft,
                onValueChange = { ruleMerchantDraft = it },
                label = "商户名称",
                onAdd = {
                    val merchant = ruleMerchantDraft.trim()
                    val categoryName = ruleCategoryDraft.trim()
                    val accountName = ruleAccountDraft.trim()
                    if (merchant.isNotBlank() && categoryName.isNotBlank() && accountName.isNotBlank()) scope.launch {
                        val category = database.categoryDao().findByName(categoryName)
                        val account = database.accountDao().findByName(accountName)
                        if (category != null && account != null) {
                            database.merchantRuleDao().upsert(
                                com.example.localledger.data.database.MerchantRuleEntity(
                                    merchant = merchant,
                                    categoryId = category.id,
                                    accountId = account.id
                                )
                            )
                            ruleMerchantDraft = ""
                            ruleCategoryDraft = ""
                            ruleAccountDraft = ""
                        }
                    }
                }
            )
            OutlinedTextField(ruleCategoryDraft, { ruleCategoryDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("规则分类") }, singleLine = true)
            OutlinedTextField(ruleAccountDraft, { ruleAccountDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("规则账户") }, singleLine = true)
            merchantRules.forEach { rule ->
                ManagementRow(rule.merchant) { scope.launch { database.merchantRuleDao().deleteById(rule.id) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { backupAction = BackupAction.EXPORT }) { Text("导出加密备份") }
                Button(onClick = { backupAction = BackupAction.IMPORT }) { Text("恢复备份") }
            }
            backupMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("应用锁")
                Switch(
                    checked = appLockEnabled,
                    onCheckedChange = { enabled -> scope.launch { preferences.setAppLockEnabled(enabled) } }
                )
            }
            Text("开启后，应用回到前台需要系统生物识别或锁屏凭据。")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("后台服务")
                    Text("显示为正在运行的服务并保持本地处理状态", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = foregroundServiceEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            runCatching {
                                ContextCompat.startForegroundService(context, Intent(context, LedgerForegroundService::class.java))
                            }.onSuccess {
                                scope.launch { preferences.setForegroundServiceEnabled(true) }
                            }.onFailure {
                                scope.launch {
                                    preferences.setForegroundServiceEnabled(false)
                                    backupMessage = "后台服务启动失败：${it.message ?: "系统拒绝启动"}"
                                }
                            }
                        } else {
                            context.stopService(Intent(context, LedgerForegroundService::class.java))
                            scope.launch { preferences.setForegroundServiceEnabled(false) }
                        }
                    }
                )
            }
        }
    }
    if (backupAction != null) {
        PasswordDialog(
            title = if (backupAction == BackupAction.EXPORT) "设置备份密码" else "输入备份密码",
            onDismiss = { backupAction = null; backupPassword = "" },
            onConfirm = { password ->
                backupPassword = password
                if (backupAction == BackupAction.EXPORT) {
                    exportLauncher.launch("LocalLedger_backup.lledger")
                } else {
                    importLauncher.launch(arrayOf("application/octet-stream", "application/*", "*/*"))
                }
                backupAction = null
            }
        )
    }
}

private enum class BackupAction { EXPORT, IMPORT }

@Composable
private fun ManagementInput(value: String, onValueChange: (String) -> Unit, label: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            singleLine = true
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "新增")
        }
    }
}

@Composable
private fun ManagementRow(label: String, onArchive: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Delete, contentDescription = "归档")
            }
        }
    )
}

@Composable
private fun PasswordDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("密码") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (password.isNotBlank()) onConfirm(password) }) { Text("继续") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ManualEntryDialog(database: AppDatabase, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("现金") }
    var toAccount by remember { mutableStateOf("微信钱包") }
    var category by remember { mutableStateOf("其他") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建账单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TransactionType.EXPENSE to "支出",
                        TransactionType.INCOME to "收入",
                        TransactionType.TRANSFER to "转账",
                        TransactionType.REFUND to "退款",
                        TransactionType.ADJUSTMENT to "调整"
                    ).forEach { (candidateType, label) ->
                        FilterChip(selected = type == candidateType, onClick = { type = candidateType }, label = { Text(label) })
                    }
                }
                OutlinedTextField(amount, { amount = it }, modifier = Modifier.fillMaxWidth(), label = { Text("金额（元）") }, singleLine = true)
                OutlinedTextField(merchant, { merchant = it }, modifier = Modifier.fillMaxWidth(), label = { Text("商户") }, singleLine = true)
                OutlinedTextField(account, { account = it }, modifier = Modifier.fillMaxWidth(), label = { Text("账户") }, singleLine = true)
                if (type == TransactionType.TRANSFER) {
                    OutlinedTextField(toAccount, { toAccount = it }, modifier = Modifier.fillMaxWidth(), label = { Text("转入账户") }, singleLine = true)
                }
                OutlinedTextField(category, { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text("分类") }, singleLine = true)
                OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注（可选）") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = amount.toBigDecimalOrNull()?.movePointRight(2)?.longValueExactOrNull()
                if (minor == null || minor <= 0L) {
                    error = "请输入大于 0 的有效金额"
                    return@TextButton
                }
                val notificationId = System.currentTimeMillis().toInt()
                LedgerNotificationNotifier.postProcessing(context, notificationId, requestPromotion = true)
                scope.launch {
                    val accountName = account.trim().ifBlank { "现金" }
                    val accountEntity = database.accountDao().findByName(accountName)
                        ?: AccountEntity(name = accountName)
                    val toAccountEntity = if (type == TransactionType.TRANSFER) {
                        val toAccountName = toAccount.trim().ifBlank { "微信钱包" }
                        database.accountDao().findByName(toAccountName)
                            ?: AccountEntity(name = toAccountName)
                    } else {
                        null
                    }
                    val categoryName = category.trim().ifBlank { "其他" }
                    val categoryEntity = database.categoryDao().findByName(categoryName)
                        ?: CategoryEntity(name = categoryName)
                    database.accountDao().upsert(accountEntity)
                    toAccountEntity?.let { database.accountDao().upsert(it) }
                    database.categoryDao().upsert(categoryEntity)
                    database.transactionDao().upsert(
                        TransactionEntity(
                            amountMinor = minor,
                            type = type,
                            accountId = accountEntity.id,
                            toAccountId = toAccountEntity?.id,
                            merchant = merchant.trim().ifBlank { null },
                            categoryId = categoryEntity.id,
                            timestamp = System.currentTimeMillis(),
                            source = "MANUAL",
                            note = note.trim().ifBlank { null }
                        )
                    )
                    LedgerNotificationNotifier.postRecorded(
                        context = context,
                        amountMinor = minor,
                        merchant = merchant.trim().ifBlank { null },
                        type = type,
                        status = TransactionStatus.CONFIRMED,
                        notificationId = notificationId,
                        requestPromotion = true,
                        paymentMethod = "MANUAL"
                    )
                    onDismiss()
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun BigDecimal.longValueExactOrNull(): Long? = try { longValueExact() } catch (_: ArithmeticException) { null }

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            AnimatedContent(targetState = value, label = "summary-value") { animatedValue ->
                Text(animatedValue, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity, accountNames: Map<String, String>, onDelete: (() -> Unit)? = null) {
    val sign = if (transaction.type == TransactionType.INCOME || transaction.type == TransactionType.REFUND) "+" else "-"
    val icon = when (transaction.type) {
        TransactionType.INCOME, TransactionType.REFUND -> Icons.Default.ArrowUpward
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
        else -> Icons.Default.ArrowDownward
    }
    val rowHeight = 72.dp
    Card(Modifier.fillMaxWidth().height(rowHeight)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        transaction.merchant ?: paymentMethodLabel(transaction.sourcePackage, transaction.accountId, accountNames[transaction.accountId]),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        sourceLabel(transaction.source),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$sign${formatMoney(transaction.amountMinor)}", style = MaterialTheme.typography.titleMedium)
                onDelete?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Delete, contentDescription = "删除账单")
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingCandidateRow(candidate: TransactionCandidateEntity, accountNames: Map<String, String>, onConfirm: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(candidate.merchant ?: paymentMethodLabel(candidate.sourcePackage, candidate.accountId, accountNames[candidate.accountId])); Text("识别置信度 ${candidate.confidence}%") }
            Button(onClick = onConfirm) { Text("确认") }
        }
    }
}

@Composable
private fun PaymentMethodSummaryRow(transactions: List<TransactionEntity>, accountNames: Map<String, String>) {
    val methods = listOf("微信", "支付宝")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        methods.forEach { method ->
            val expense = transactions.filter { it.type == TransactionType.EXPENSE && paymentMethodLabel(it.sourcePackage, it.accountId, accountNames[it.accountId]) == method }.sumOf { it.amountMinor }
            val income = transactions.filter { (it.type == TransactionType.INCOME || it.type == TransactionType.REFUND) && paymentMethodLabel(it.sourcePackage, it.accountId, accountNames[it.accountId]) == method }.sumOf { it.amountMinor }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(method, style = MaterialTheme.typography.labelLarge)
                    Text("支出 ${formatMoney(expense)}", style = MaterialTheme.typography.bodySmall)
                    Text("收入 ${formatMoney(income)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodBreakdownSheet(transactions: List<TransactionEntity>, accountNames: Map<String, String>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("按支付方式", style = MaterialTheme.typography.titleLarge)
            Text("微信和支付宝的收入、支出明细", style = MaterialTheme.typography.bodySmall)
            PaymentMethodSummaryRow(transactions, accountNames)
        }
    }
}

@Composable
private fun MonthlyBudgetCard(expenseMinor: Long, budgetMinor: Long, onEdit: () -> Unit) {
    val progress = if (budgetMinor > 0L) {
        (expenseMinor.toDouble() / budgetMinor.toDouble()).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }
    val overBudget = budgetMinor > 0L && expenseMinor > budgetMinor
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("本月支出进度", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (budgetMinor > 0L) "${formatMoney(expenseMinor)} / ${formatMoney(budgetMinor)}" else "设置本月总额后开始计算",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onEdit) { Text(if (budgetMinor > 0L) "修改" else "设置") }
            }
            WaveProgress(progress = progress, warning = overBudget)
            if (overBudget) {
                Text("已超出 ${formatMoney(expenseMinor - budgetMinor)}", color = MaterialTheme.colorScheme.error)
            } else if (budgetMinor > 0L) {
                Text("剩余 ${formatMoney(budgetMinor - expenseMinor)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun WaveProgress(progress: Float, warning: Boolean) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
        label = "monthly-budget-progress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "wave-motion")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing)
        ),
        label = "wave-offset"
    )
    val waveColor = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    val trackColor = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val remainderColor = if (warning) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f)
    }
    Canvas(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
    ) {
        drawRect(trackColor)
        val left = 16.dp.toPx()
        val right = size.width - 16.dp.toPx()
        val lineWidth = (right - left).coerceAtLeast(1f)
        val progressX = left + lineWidth * animatedProgress
        val centerY = size.height / 2f
        val amplitude = 3.5.dp.toPx()
        val stroke = 3.dp.toPx()
        // Four full cycles fit into the first quarter of the track.
        val waveCount = 16.0

        drawLine(
            remainderColor,
            Offset(progressX, centerY),
            Offset(right, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val path = Path().apply {
            moveTo(left, centerY)
            val step = 1f
            val movingStart = left + 8.dp.toPx()
            val movingEnd = progressX - 8.dp.toPx()
            var x = movingStart
            while (movingEnd > movingStart && x <= movingEnd) {
                val phase = ((x - left) / lineWidth) * (PI * waveCount * 2.0) + waveOffset * (PI * 2.0)
                val y = centerY + sin(phase).toFloat() * amplitude
                lineTo(x, y)
                x += step
            }
            lineTo(progressX, centerY)
        }
        drawPath(
            path,
            waveColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawCircle(waveColor.copy(alpha = 0.55f), radius = 3.dp.toPx(), center = Offset(left, centerY))
        drawCircle(waveColor, radius = 5.dp.toPx(), center = Offset(progressX, centerY))
        drawCircle(remainderColor, radius = 3.dp.toPx(), center = Offset(right, centerY))
    }
}

@Composable
private fun MonthlyBudgetDialog(currentBudgetMinor: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var amount by remember {
        mutableStateOf(if (currentBudgetMinor > 0L) "%.2f".format(currentBudgetMinor / 100.0) else "")
    }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本月总额") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("每月自动按当月支出重新计算，可随时修改。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("金额（元）") },
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = amount.toBigDecimalOrNull()?.movePointRight(2)?.longValueExactOrNull()
                if (minor == null || minor < 0L) {
                    error = "请输入有效金额"
                } else {
                    onSave(minor)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun paymentMethodLabel(sourcePackage: String?, accountId: String?, accountName: String?): String = when {
    accountName?.contains("微信") == true -> "微信"
    accountName?.contains("支付宝") == true -> "支付宝"
    sourcePackage == "com.tencent.mm" || accountId == "auto-WECHAT" -> "微信"
    sourcePackage == "com.eg.android.AlipayGphone" || accountId == "auto-ALIPAY" -> "支付宝"
    else -> "其他支付"
}

private fun sourceLabel(source: String): String = when (source) {
    "MANUAL" -> "手动添加"
    "NOTIFICATION" -> "通知"
    "ACCESSIBILITY" -> "无障碍"
    else -> source
}

private fun formatMoney(minor: Long): String = "¥%.2f".format(minor / 100.0)

private fun isCurrentMonth(timestamp: Long): Boolean {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return YearMonth.from(date) == YearMonth.now()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalLedgerTopBar(title: String, action: (@Composable () -> Unit)? = null) {
    TopAppBar(title = { Text(title) }, actions = { action?.invoke() })
}
