package com.example.localledger.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.localledger.R
import com.example.localledger.data.database.TransactionStatus
import com.example.localledger.data.database.TransactionType

object LedgerNotificationNotifier {
    private const val CHANNEL_ID = "ledger_updates"
    private const val SERVICE_CHANNEL_ID = "ledger_service"
    private const val CHANNEL_NAME = "记账动态"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "显示本地记账处理状态"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "后台服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示 LocalLedger 后台服务运行状态"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
    }

    fun buildServiceNotification(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_localledger)
            .setContentTitle("LocalLedger 正在运行")
            .setContentText("本地通知监听和备选记账服务已启用")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun postRecorded(
        context: Context,
        amountMinor: Long,
        merchant: String?,
        type: TransactionType,
        status: TransactionStatus,
        notificationId: Int,
        requestPromotion: Boolean = false,
        paymentMethod: String? = null,
        appLabel: String? = null,
        candidateId: String? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val isPending = status == TransactionStatus.PENDING
        val actionLabel = if (isPending) "待确认" else "已记账"
        val sign = if (type == TransactionType.INCOME || type == TransactionType.REFUND) "+" else "-"
        val content = "$actionLabel ${sign}${formatMoney(amountMinor)} · ${merchant ?: paymentMethodLabel(paymentMethod)}"
        val builder = baseBuilder(context, requestPromotion)
            .setSmallIcon(R.drawable.ic_stat_localledger)
            .setContentTitle("${appLabel ?: "LocalLedger"} · LocalLedger")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)

        if (isPending && candidateId != null) {
            val confirmIntent = Intent(context, TransactionActionReceiver::class.java).apply {
                this.action = TransactionActionReceiver.ACTION_CONFIRM
                putExtra(TransactionActionReceiver.EXTRA_CANDIDATE_ID, candidateId)
            }
            val confirmPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_stat_localledger,
                    "确认",
                    confirmPendingIntent
                ).build()
            )
        }

        // Android 16+ may promote this short-lived ongoing update; older versions show it normally.
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun postProcessing(context: Context, notificationId: Int, requestPromotion: Boolean = false, appLabel: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val content = "正在解析通知并写入本地账本"
        val builder = baseBuilder(context, requestPromotion)
            .setSmallIcon(R.drawable.ic_stat_localledger)
            .setContentTitle("${appLabel ?: "LocalLedger"} · LocalLedger")
            .setContentText("正在记账…")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun baseBuilder(context: Context, requestPromotion: Boolean): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setRequestPromotedOngoing(requestPromotion)
            .setTimeoutAfter(6_000L)
    }

    private fun formatMoney(minor: Long): String = "¥%.2f".format(minor / 100.0)

    private fun paymentMethodLabel(paymentMethod: String?): String = when (paymentMethod) {
        "WECHAT" -> "微信"
        "ALIPAY" -> "支付宝"
        "MANUAL" -> "手动添加"
        else -> "其他支付"
    }
}
