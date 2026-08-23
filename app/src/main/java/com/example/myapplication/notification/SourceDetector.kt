package com.example.localledger.notification

import com.example.localledger.notification.parser.AlipayParser
import com.example.localledger.notification.parser.GenericParser
import com.example.localledger.notification.parser.PaymentParser
import com.example.localledger.notification.parser.WeChatParser

class SourceDetector {
    fun parserFor(packageName: String): PaymentParser = when (packageName) {
        WECHAT_PACKAGE -> WeChatParser()
        ALIPAY_PACKAGE -> AlipayParser()
        else -> GenericParser()
    }

    companion object {
        const val WECHAT_PACKAGE = "com.tencent.mm"
        const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
    }
}
