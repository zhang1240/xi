package com.example.localledger.notification

import java.text.Normalizer

object NotificationNormalizer {
    fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replace('￥', '¥')
            .replace(Regex("[\\t\\r]+"), " ")
            .replace(Regex(" +"), " ")
            .trim()
    }
}
