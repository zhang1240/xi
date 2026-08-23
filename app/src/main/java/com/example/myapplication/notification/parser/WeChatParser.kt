package com.example.localledger.notification.parser

class WeChatParser : GenericParser() {
    override fun parse(title: String?, text: String?, packageName: String): ParsedPayment? {
        return super.parse(title, text, packageName)?.copy(paymentMethod = "WECHAT", confidence = 85)
    }
}
