package com.example.localledger.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.core.app.NotificationManagerCompat
import android.content.Intent
import android.content.Context
import android.provider.Settings
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
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
import com.example.localledger.ui.theme.ledgerColors
import com.example.localledger.ui.theme.isReducedMotion
import com.example.localledger.service.LedgerForegroundService
import com.example.localledger.service.BackgroundKeepAlive
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.util.Locale

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    database: AppDatabase,
    preferences: AppPreferences,
    onOpenPending: () -> Unit,
    onOpenTransactions: () -> Unit
) {
    val colors = ledgerColors()
    val transactions by database.transactionDao().observeAll().collectAsState(initial = emptyList())
    val accounts by database.accountDao().observeActive().collectAsState(initial = emptyList())
    val accountNames = accounts.associate { it.id to it.name }
    val categories by database.categoryDao().observeActive().collectAsState(initial = emptyList())
    val merchantRules by database.merchantRuleDao().observeAll().collectAsState(initial = emptyList())
    val categoryById = categories.associate { it.id to it.name }
    val categoryByMerchant = merchantRules.map { rule ->
        rule.merchant to (rule.categoryId?.let { categoryById[it] } ?: "")
    }.toMap()
    val monthlyBudgetMinor by preferences.monthlyBudgetMinor.collectAsState(initial = 0L)
    val pendingCandidates by database.candidateDao().observePending().collectAsState(initial = emptyList())
    var showEntry by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val monthTransactions = transactions.filter { isCurrentMonth(it.timestamp) }
    val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val refunds = monthTransactions.filter { it.type == TransactionType.REFUND }.sumOf { it.amountMinor }
    val todayExpense = monthTransactions.filter { it.type == TransactionType.EXPENSE && isToday(it.timestamp) }.sumOf { it.amountMinor }
    val todayIncome = monthTransactions.filter { (it.type == TransactionType.INCOME || it.type == TransactionType.REFUND) && isToday(it.timestamp) }.sumOf { it.amountMinor }
    val daysElapsed = LocalDate.now().dayOfMonth.coerceAtLeast(1)
    val dailyAvg = expense / daysElapsed
    val todayDate = LocalDate.now()
    val recentExpenseByDay = (6 downTo 0).map { offset ->
        val date = todayDate.minusDays(offset.toLong())
        date to transactions.filter { it.type == TransactionType.EXPENSE && localDateOf(it.timestamp) == date }
            .sumOf { it.amountMinor }
    }
    val grouped = monthTransactions
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toList()
        .sortedByDescending { it.first }
    val fabInteraction = remember { MutableInteractionSource() }
    val fabPressed by fabInteraction.collectIsPressedAsState()
    val reduceMotion = isReducedMotion()
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed && !reduceMotion) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f),
        label = "fab-scale"
    )

    Scaffold(
        modifier = Modifier
            .padding(paddingValues)
            .background(colors.pageBackground),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEntry = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("记一笔") },
                containerColor = colors.fabContainer,
                contentColor = colors.fabContent,
                interactionSource = fabInteraction,
                modifier = Modifier.graphicsLayer { scaleX = fabScale; scaleY = fabScale }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
        ) {
            item {
                HomeHeader(count = transactions.size)
            }
            item {
                BillSummaryCards(
                    monthlyExpense = expense,
                    todayExpense = todayExpense,
                    todayIncome = todayIncome,
                    dailyAvg = dailyAvg,
                    recentExpenseByDay = recentExpenseByDay
                )
            }
            item {
                MonthlyBudgetCard(expense, monthlyBudgetMinor) { showBudgetDialog = true }
            }
            if (pendingCandidates.isNotEmpty()) {
                item {
                    PendingEntryCard(count = pendingCandidates.size, onOpen = onOpenPending)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${YearMonth.now().monthValue} 月账单", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onOpenTransactions) { Text("全部") }
                }
            }
            if (grouped.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "本月还没有账单",
                        description = "开启通知监听后可自动记账，也可以点右下角记一笔。",
                        actionLabel = "记一笔",
                        onAction = { showEntry = true }
                    )
                }
            } else {
                grouped.forEach { (date, groupTxns) ->
                    val dayExpense = groupTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
                    val dayIncome = groupTxns.filter { it.type == TransactionType.INCOME || it.type == TransactionType.REFUND }.sumOf { it.amountMinor }
                    item(key = "date-${date.toEpochDay()}") {
                        BillDateHeader(dateLabel(date), dayExpense, dayIncome)
                    }
                    itemsIndexed(groupTxns, key = { _, txn -> txn.id }) { index, txn ->
                        BillRow(
                            transaction = txn,
                            categoryName = categoryNameFor(txn, categoryById, categoryByMerchant),
                            accountNames = accountNames,
                            isLast = index == groupTxns.lastIndex
                        )
                    }
                }
            }
        }
    }
    if (showEntry) {
        ManualEntryDialog(database = database, onDismiss = { showEntry = false })
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
private fun HomeHeader(count: Int) {
    val colors = ledgerColors()
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Text("我的账单", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = colors.ink)
        Text("$count 条记账", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BillSummaryCards(
    monthlyExpense: Long,
    todayExpense: Long,
    todayIncome: Long,
    dailyAvg: Long,
    recentExpenseByDay: List<Pair<LocalDate, Long>>
) {
    val colors = ledgerColors()
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.weight(1f).fillMaxHeight()) {
            Column(
                Modifier.padding(14.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(formatMoney(monthlyExpense), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colors.ink)
                    Text("本月总支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryLine("今日支出", formatMoney(todayExpense), colors.expense)
                    SummaryLine("今日收入", formatMoney(todayIncome), colors.income)
                }
            }
        }
        Card(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatMoney(dailyAvg), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colors.ink)
                Text("本月日均支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                DailyExpenseBars(recentExpenseByDay)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = color)
    }
}

/**
 * 近 7 天支出柱状图。
 *
 * 之前是 31 根柱子挤在一张卡片里：每根 3dp 宽、没有日期轴、空天完全空白，
 * 用户无法把它对应到任何一天。改成 7 根 + 星期标签，宁可少画也要能读。
 */
@Composable
private fun DailyExpenseBars(recentExpenseByDay: List<Pair<LocalDate, Long>>) {
    val colors = ledgerColors()
    val maxValue = recentExpenseByDay.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        recentExpenseByDay.forEach { (date, amount) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 柱轨始终绘制，空天呈现为浅槽而非空白，避免"没有数据"被误读成"没有渲染"。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.iconShell.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val fraction = (amount.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                    if (amount > 0L) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (date == LocalDate.now()) colors.accent
                                    else colors.accent.copy(alpha = 0.55f)
                                )
                        )
                    }
                }
                Text(
                    weekdayLabel(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BillDateHeader(dateLabel: String, expenseMinor: Long, incomeMinor: Long) {
    val colors = ledgerColors()
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Text(
            "支出：${formatMoney(expenseMinor)}  收入：${formatMoney(incomeMinor)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BillRow(
    transaction: TransactionEntity,
    categoryName: String,
    accountNames: Map<String, String>,
    isLast: Boolean
) {
    val colors = ledgerColors()
    val isIncomeDirection = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.REFUND
    val sign = if (isIncomeDirection) "+" else "-"
    val merchant = transaction.merchant
        ?: paymentMethodLabel(transaction.sourcePackage, transaction.accountId, accountNames[transaction.accountId])
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(colors.iconShell),
                contentAlignment = Alignment.Center
            ) {
                Text(categoryIconChar(categoryName), style = MaterialTheme.typography.titleSmall, color = colors.onIconShell)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(categoryName, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                Text(
                    "${formatTime(transaction.timestamp)} | ${sourceLabel(transaction.source)} | $merchant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "$sign${formatMoney(transaction.amountMinor)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isIncomeDirection) colors.income else colors.expense
            )
        }
        if (!isLast) {
            HorizontalDivider(
                Modifier.padding(start = 50.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

private fun categoryIconChar(name: String): String = when (name) {
    "餐饮" -> "餐"
    "娱乐" -> "娱"
    "购物" -> "购"
    "交通" -> "行"
    "转账" -> "转"
    "生活缴费" -> "缴"
    else -> name.take(1).ifBlank { "账" }
}

private fun categoryNameFor(
    transaction: TransactionEntity,
    categoryById: Map<String, String>,
    categoryByMerchant: Map<String, String>
): String {
    transaction.categoryId?.let { categoryById[it] }?.takeIf { it.isNotBlank() }?.let { return it }
    transaction.merchant?.let { merchant ->
        categoryByMerchant[merchant]?.takeIf { it.isNotBlank() }?.let { return it }
        return inferCategoryByMerchant(merchant)
    }
    return "其他"
}

private fun inferCategoryByMerchant(merchant: String): String {
    val m = merchant.lowercase()
    return when {
        listOf("美团", "饿了么", "麦当劳", "肯德基", "瑞幸", "星巴克", "奶茶", "餐", "食", "饭").any { m.contains(it) } -> "餐饮"
        listOf("steam", "游戏", "视频", "腾讯视频", "网易云", "音乐", "影院", "电影").any { m.contains(it) } -> "娱乐"
        listOf("淘宝", "京东", "拼多多", "天猫", "唯品会", "商城", "超市", "便利店").any { m.contains(it) } -> "购物"
        listOf("滴滴", "高德", "出行", "打车", "地铁", "公交", "12306", "铁路", "航空", "加油").any { m.contains(it) } -> "交通"
        listOf("电", "水", "燃气", "话费", "宽带", "移动", "联通", "电信").any { m.contains(it) } -> "生活缴费"
        listOf("转账", "收款", "红包", "还款", "退款", "零钱", "余额", "钱包").any { m.contains(it) } || (m.contains("*") && (m.contains("(") || m.contains(")"))) -> "转账"
        else -> "其他"
    }
}

private fun formatTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))

private fun dateLabel(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("M月d日"))

private fun localDateOf(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

private fun isToday(timestamp: Long): Boolean = localDateOf(timestamp) == LocalDate.now()

/** 支出迷你柱状图的日期轴标签；今天显示"今"而不是误导性的"日"。 */
private fun weekdayLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "今"
    LocalDate.now().minusDays(1) -> "昨"
    else -> when (date.dayOfWeek.value) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        else -> "日"
    }
}

@Composable
fun PendingScreen(
    paddingValues: PaddingValues,
    database: AppDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val candidates by database.candidateDao().observePending().collectAsState(initial = emptyList())
    val accounts by database.accountDao().observeActive().collectAsState(initial = emptyList())
    val accountNames = accounts.associate { it.id to it.name }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            LocalLedgerTopBar("待确认", action = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            })
        }
    ) { innerPadding ->
        if (candidates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.CheckCircleOutline,
                    title = "全部处理完了",
                    description = "低置信度的支付会进入这里，确认后会正式入账。"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
            ) {
                items(candidates, key = { it.id }) { candidate ->
                    PendingCandidateRow(
                        candidate = candidate,
                        accountNames = accountNames,
                        onConfirm = { scope.launch { confirmCandidate(context, database, candidate) } },
                        onIgnore = { scope.launch { ignoreCandidate(context, database, candidate) } }
                    )
                }
            }
        }
    }
}

private suspend fun ignoreCandidate(context: Context, database: AppDatabase, candidate: TransactionCandidateEntity) {
    database.candidateDao().updateStatus(candidate.id, TransactionStatus.IGNORED)
    val notificationId = candidate.deduplicationKey?.hashCode() ?: candidate.id.hashCode()
    NotificationManagerCompat.from(context).cancel(notificationId)
}

private suspend fun confirmCandidate(context: Context, database: AppDatabase, candidate: TransactionCandidateEntity) {
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
    val notificationId = candidate.deduplicationKey?.hashCode() ?: candidate.id.hashCode()
    NotificationManagerCompat.from(context).cancel(notificationId)
    LedgerNotificationNotifier.postRecorded(
        context = context,
        amountMinor = candidate.amountMinor,
        merchant = candidate.merchant,
        type = candidate.type,
        status = TransactionStatus.CONFIRMED,
        notificationId = notificationId,
        requestPromotion = true,
        paymentMethod = candidate.sourcePackage
    )
}

@Composable
fun TransactionsScreen(paddingValues: PaddingValues, database: AppDatabase) {
    val colors = ledgerColors()
    val transactions by database.transactionDao().observeAll().collectAsState(initial = emptyList())
    val accounts by database.accountDao().observeActive().collectAsState(initial = emptyList())
    val accountNames = accounts.associate { it.id to it.name }
    var showEntry by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = { LocalLedgerTopBar("账单") },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("全部账单", style = MaterialTheme.typography.titleLarge, color = colors.ink)
                Button(onClick = { showEntry = true }) { Text("新建") }
            }
            val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
            val totalIncome = transactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.REFUND }.sumOf { it.amountMinor }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("总支出", formatMoney(totalExpense), Modifier.weight(1f))
                SummaryCard("总收入", formatMoney(totalIncome), Modifier.weight(1f))
            }
            if (transactions.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ReceiptLong,
                    title = "还没有交易记录",
                    description = "点新建手动记一笔，或在设置里开启通知监听与无障碍记账，让微信、支付宝的支付自动入账。",
                    actionLabel = "记一笔",
                    onAction = { showEntry = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
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
            text = { Text("删除后不会影响账户和分类设置。删除后可在底部撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete = null
                        scope.launch {
                            database.transactionDao().deleteById(transaction.id)
                            val result = snackbarHostState.showSnackbar(
                                message = "已删除 ${formatMoney(transaction.amountMinor)}",
                                actionLabel = "撤销",
                                withDismissAction = true
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                database.transactionDao().upsert(transaction)
                            }
                        }
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { transactionToDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
fun StatisticsScreen(paddingValues: PaddingValues, database: AppDatabase) {
    val transactions by database.transactionDao().observeAll().collectAsState(initial = emptyList())
    val categories by database.categoryDao().observeActive().collectAsState(initial = emptyList())
    val monthTransactions = transactions.filter { isCurrentMonth(it.timestamp) }
    val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val refunds = monthTransactions.filter { it.type == TransactionType.REFUND }.sumOf { it.amountMinor }
    val categoryNames = categories.associate { it.id to it.name }
    val categoryTotals = categoryBreakdown(monthTransactions, categoryNames)

    Scaffold(modifier = Modifier.padding(paddingValues), topBar = {
        LocalLedgerTopBar("统计")
    }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("本月统计", style = MaterialTheme.typography.titleLarge, color = ledgerColors().ink)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("收入与退款", formatMoney(income + refunds), Modifier.weight(1f))
                SummaryCard("支出", formatMoney(expense), Modifier.weight(1f))
            }
            Text("按类别", style = MaterialTheme.typography.titleMedium)
            if (categoryTotals.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.BarChart,
                    title = "本月暂无支出",
                    description = "有支出后，这里会按类别显示占比。"
                )
            } else {
                categoryTotals.forEach { entry ->
                    CategoryBreakdownRow(name = entry.name, amountMinor = entry.amountMinor, ratio = if (expense > 0L) entry.amountMinor.toFloat() / expense.toFloat() else 0f)
                }
            }
        }
    }
}

private data class CategoryBreakdownEntry(val name: String, val amountMinor: Long)

private fun categoryBreakdown(transactions: List<TransactionEntity>, categoryNames: Map<String, String>): List<CategoryBreakdownEntry> {
    // 按显示名聚合：自动记账用 "category-名称" 形式的 id，用户手建的分类是 UUID，
    // 同名分类若按 id 分组会被拆成多行。UI 层做兜底聚合，id 空间的根治留给数据层。
    val totals = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { transaction ->
            val name = transaction.categoryId?.let { categoryNames[it] }
                ?: transaction.categoryId?.removePrefix("category-")
                ?: "其他"
            if (name.isBlank()) "其他" else name
        }
        .map { (name, list) -> CategoryBreakdownEntry(name, list.sumOf { it.amountMinor }) }
    return totals.sortedByDescending { it.amountMinor }
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
    var batteryOptimizationIgnored by remember { mutableStateOf(BackgroundKeepAlive.isIgnoringBatteryOptimizations(context)) }
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
                batteryOptimizationIgnored = BackgroundKeepAlive.isIgnoringBatteryOptimizations(context)
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
            Text("权限与自动记账", style = MaterialTheme.typography.titleMedium)
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
            Text(if (accessibilityEnabled) "无障碍自动记账：已开启" else "无障碍自动记账：未开启")
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                Text(if (accessibilityEnabled) "管理无障碍权限" else "开启无障碍权限")
            }
            Text("作为主来源读取微信和支付宝支付界面，高可信交易自动记账并推送，低可信进入待确认。")
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
                    Text("显示为正在运行的服务；开启无障碍时自动保持运行", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = foregroundServiceEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            runCatching {
                                ContextCompat.startForegroundService(context, Intent(context, LedgerForegroundService::class.java))
                            }.onSuccess {
                                scope.launch { preferences.setForegroundServiceEnabled(true) }
                                BackgroundKeepAlive.scheduleHeartbeat(context)
                            }.onFailure {
                                scope.launch {
                                    preferences.setForegroundServiceEnabled(false)
                                    backupMessage = "后台服务启动失败：${it.message ?: "系统拒绝启动"}"
                                }
                            }
                        } else {
                            context.stopService(Intent(context, LedgerForegroundService::class.java))
                            scope.launch { preferences.setForegroundServiceEnabled(false) }
                            if (AccessibilityPermission.isEnabled(context)) {
                                BackgroundKeepAlive.scheduleHeartbeat(context)
                            } else {
                                BackgroundKeepAlive.cancelHeartbeat(context)
                            }
                        }
                    }
                )
            }
            Text("后台保活", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("忽略电池优化")
                    Text("防止系统在后台限制记账服务，影响自动记账可靠性", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { BackgroundKeepAlive.requestIgnoreBatteryOptimizations(context) }) {
                    Text(if (batteryOptimizationIgnored) "已忽略" else "去开启")
                }
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
    var revealed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                singleLine = true,
                // 备份文件是全量账本，密码必须遮蔽，否则肩上偷窥即可拿到解密口令。
                visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { revealed = !revealed }) {
                        Icon(
                            imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (revealed) "隐藏密码" else "显示密码"
                        )
                    }
                }
            )
        },
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
            // 键盘弹出时 AlertDialog 内容会溢出：这里必须可滚动，否则"保存"按钮被顶出屏幕。
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("金额（元）") },
                    singleLine = true,
                    // 记账 App 的最高频输入：必须直接弹数字键盘，弹全键盘是最刺眼的缺失。
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    textStyle = MaterialTheme.typography.headlineSmall
                )
                OutlinedTextField(merchant, { merchant = it }, modifier = Modifier.fillMaxWidth(), label = { Text("商户") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next))
                OutlinedTextField(account, { account = it }, modifier = Modifier.fillMaxWidth(), label = { Text("账户") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next))
                if (type == TransactionType.TRANSFER) {
                    OutlinedTextField(toAccount, { toAccount = it }, modifier = Modifier.fillMaxWidth(), label = { Text("转入账户") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next))
                }
                OutlinedTextField(category, { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text("分类") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next))
                OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注（可选）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done))
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
    val colors = ledgerColors()
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            // 不做 AnimatedContent：金额随每次 DAO 推送变化，逐次淡入属于对高频变化的打扰。
            Text(value, style = MaterialTheme.typography.titleLarge, color = colors.ink)
        }
    }
}

/**
 * 空状态。记账应用里这是最贵的引导位：一行"暂无数据"浪费了把用户推向权限页的机会，
 * 所以这里带图标 + 说明 + 一个明确的下一步动作。
 */
@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ledgerColors().iconShell.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = ledgerColors().ink)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.86f)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun RecentTransactionRow(transaction: TransactionEntity, accountNames: Map<String, String>, onDelete: (() -> Unit)? = null) {
    val sign = if (transaction.type == TransactionType.INCOME || transaction.type == TransactionType.REFUND) "+" else "-"
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                transaction.merchant ?: paymentMethodLabel(transaction.sourcePackage, transaction.accountId, accountNames[transaction.accountId]),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(sourceLabel(transaction.source), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$sign${formatMoney(transaction.amountMinor)}", style = MaterialTheme.typography.titleSmall)
            onDelete?.let {
                IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除账单", modifier = Modifier.size(18.dp))
                }
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
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
private fun PendingCandidateRow(
    candidate: TransactionCandidateEntity,
    accountNames: Map<String, String>,
    onConfirm: () -> Unit,
    onIgnore: () -> Unit
) {
    val sign = if (candidate.type == TransactionType.INCOME || candidate.type == TransactionType.REFUND) "+" else "-"
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    candidate.merchant ?: paymentMethodLabel(candidate.sourcePackage, candidate.accountId, accountNames[candidate.accountId]),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text("$sign${formatMoney(candidate.amountMinor)} · ${sourceLabel(candidate.source)}", style = MaterialTheme.typography.bodySmall)
                Text("识别置信度 ${candidate.confidence}%", style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onIgnore) { Text("忽略") }
                Button(onClick = onConfirm) { Text("确认") }
            }
        }
    }
}

@Composable
private fun PendingEntryCard(count: Int, onOpen: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("待确认", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (count > 0) "有 $count 笔待处理，点此查看" else "暂无待处理账单",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(name: String, amountMinor: Long, ratio: Float) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(formatMoney(amountMinor), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            CategoryRatioBar(ratio.coerceIn(0f, 1f))
        }
    }
}

@Composable
private fun CategoryRatioBar(ratio: Float) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))) {
        drawRect(trackColor)
        drawRect(fillColor, size = androidx.compose.ui.geometry.Size(size.width * ratio, size.height))
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

/**
 * 预算波形进度条。
 *
 * 波浪只在进度变化时流动 600ms 然后停下。此前这里用 rememberInfiniteTransition
 * 让首页常驻 60fps 的 Canvas 重绘——对一款为了记账在后台拼命保活的应用，
 * 首页无限动画是自我矛盾；系统「移除动画」开关也必须被尊重。
 */
@Composable
private fun WaveProgress(progress: Float, warning: Boolean) {
    val colors = ledgerColors()
    val reduceMotion = isReducedMotion()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reduceMotion) tween(0) else spring(dampingRatio = 1f, stiffness = 700f),
        label = "monthly-budget-progress"
    )
    val phase = remember { Animatable(0f) }
    LaunchedEffect(progress, reduceMotion) {
        if (reduceMotion) {
            phase.snapTo(0f)
        } else {
            phase.snapTo(0f)
            phase.animateTo(1f, tween(durationMillis = 600, easing = LinearEasing))
        }
    }
    val waveOffset = phase.value
    val waveColor = if (warning) colors.onWarningContainer else colors.onProgressContainer
    val trackColor = if (warning) colors.warningContainer else colors.progressContainer
    val remainderColor = waveColor.copy(alpha = 0.28f)
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
            // 2px 步长：对 3.5dp 振幅的正弦完全足够，重绘成本减半。
            val step = 2f
            val movingStart = left + 8.dp.toPx()
            val movingEnd = progressX - 8.dp.toPx()
            var x = movingStart
            while (movingEnd > movingStart && x <= movingEnd) {
                val phaseAtX = ((x - left) / lineWidth) * (PI * waveCount * 2.0) + waveOffset * (PI * 2.0)
                val y = centerY + sin(phaseAtX).toFloat() * amplitude
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
        // 预填值必须用固定 Locale：默认 Locale 为德语时 "%.2f" 产出 "1234,56"，
        // 回填到输入框后 toBigDecimalOrNull() 解析失败，用户什么都没改也会报"请输入有效金额"。
        mutableStateOf(if (currentBudgetMinor > 0L) String.format(Locale.US, "%.2f", currentBudgetMinor / 100.0) else "")
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
                    singleLine = true,
                    // 预算不是每笔输入，保持普通数字键盘即可，不必放大字号。
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
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

// 金额格式固定英文分组习惯，不跟随设备 Locale：德语/法语环境下 %.2f 会把小数点
// 变成逗号，账单里 "¥1.234,56" 会被误读成一千二。
private val moneyFormatter = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))

private fun formatMoney(minor: Long): String = "¥" + moneyFormatter.format(minor / 100.0)

private fun isCurrentMonth(timestamp: Long): Boolean {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return YearMonth.from(date) == YearMonth.now()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalLedgerTopBar(title: String, action: (@Composable () -> Unit)? = null) {
    TopAppBar(title = { Text(title) }, actions = { action?.invoke() })
}