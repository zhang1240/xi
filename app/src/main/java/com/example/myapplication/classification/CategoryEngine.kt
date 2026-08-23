package com.example.localledger.classification

import com.example.localledger.data.database.MerchantRuleEntity

data class CategoryDecision(val categoryId: String, val confidence: Int)

class CategoryEngine {
    fun decide(merchant: String?, rule: MerchantRuleEntity?): CategoryDecision {
        rule?.categoryId?.let { return CategoryDecision(it, 100) }
        val name = merchant.orEmpty()
        val category = when {
            listOf("星巴克", "咖啡", "餐厅", "饭店", "奶茶", "便利店").any { name.contains(it) } -> "餐饮"
            listOf("地铁", "公交", "滴滴", "打车", "铁路", "航空").any { name.contains(it) } -> "交通"
            listOf("超市", "商场", "商城", "京东", "淘宝").any { name.contains(it) } -> "购物"
            listOf("医院", "药房", "诊所").any { name.contains(it) } -> "医疗"
            else -> "其他"
        }
        val confidence = if (category == "其他") 40 else 65
        return CategoryDecision(category, confidence)
    }
}
