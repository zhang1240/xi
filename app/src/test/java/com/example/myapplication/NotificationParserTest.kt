package com.example.localledger

import com.example.localledger.data.database.TransactionType
import com.example.localledger.notification.NotificationNormalizer
import com.example.localledger.notification.parser.AlipayParser
import com.example.localledger.notification.parser.WeChatParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NotificationParserTest {
    @Test
    fun normalizerUnifiesCurrencyAndWhitespace() {
        assertEquals("¥25.80 支付成功", NotificationNormalizer.normalize("￥25.80\t支付成功"))
    }

    @Test
    fun wechatExpenseExtractsAmountAndMerchant() {
        val result = WeChatParser().parse("微信支付", "支付成功 ¥25.80 收款方：XX便利店", "com.tencent.mm")
        assertNotNull(result)
        assertEquals(2580L, result?.amountMinor)
        assertEquals("XX便利店", result?.merchant)
        assertEquals(TransactionType.EXPENSE, result?.transactionType)
    }

    @Test
    fun alipayIncomeExtractsAmount() {
        val result = AlipayParser().parse("支付宝到账", "收款 ¥100.00 付款方：客户", "com.eg.android.AlipayGphone")
        assertNotNull(result)
        assertEquals(10000L, result?.amountMinor)
        assertEquals(TransactionType.INCOME, result?.transactionType)
    }

    @Test
    fun ignoresItemCountAndUsesCurrencyAmount() {
        val result = WeChatParser().parse(
            "微信支付",
            "购买成功 2条商品，实付￥10.00，收款方：便利店",
            "com.tencent.mm"
        )
        assertNotNull(result)
        assertEquals(1000L, result?.amountMinor)
    }

    @Test
    fun rejectsUnqualifiedCountWithoutAmountSignal() {
        val result = WeChatParser().parse(
            "微信支付",
            "购买成功 2条商品",
            "com.tencent.mm"
        )
        assertEquals(null, result)
    }

    @Test
    fun alipayTransactionReminderParsesExpense() {
        val result = AlipayParser().parse(
            "交易提醒",
            "你有一笔5.00元的支出，点此查看详情。",
            "com.eg.android.AlipayGphone"
        )
        assertNotNull(result)
        assertEquals(500L, result?.amountMinor)
        assertEquals(TransactionType.EXPENSE, result?.transactionType)
        assertEquals(null, result?.merchant)
    }

    @Test
    fun alipayScanCollectionParsesIncomeAndPayer() {
        val result = AlipayParser().parse(
            "收款通知",
            "晓燕通过扫码向你付款5.00元",
            "com.eg.android.AlipayGphone"
        )
        assertNotNull(result)
        assertEquals(500L, result?.amountMinor)
        assertEquals(TransactionType.INCOME, result?.transactionType)
        assertEquals("晓燕", result?.merchant)
    }

    @Test
    fun windowTextPrefersExplicitPaidAmount() {
        val result = WeChatParser().parse(
            "微信支付",
            "订单已支付 ¥50.00 优惠 ¥5.00 实付 ¥45.00 收款方：便利店",
            "com.tencent.mm"
        )
        assertNotNull(result)
        assertEquals(4500L, result?.amountMinor)
        assertEquals(TransactionType.EXPENSE, result?.transactionType)
    }

    @Test
    fun alipayWindowReminderParsesYuanAmount() {
        val result = AlipayParser().parse(
            "交易提醒",
            "你有一笔12.30元的支出，点此查看详情。",
            "com.eg.android.AlipayGphone"
        )
        assertNotNull(result)
        assertEquals(1230L, result?.amountMinor)
        assertEquals(TransactionType.EXPENSE, result?.transactionType)
    }
}
