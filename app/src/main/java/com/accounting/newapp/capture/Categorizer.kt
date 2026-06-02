package com.accounting.newapp.capture

import com.accounting.newapp.data.MerchantRuleEntity

object Categorizer {
    private val keywordCategories = mapOf(
        "餐饮" to listOf("餐", "饭", "咖啡", "茶", "奶茶", "外卖", "美团", "饿了么", "麦当劳", "肯德基"),
        "购物" to listOf("淘宝", "天猫", "京东", "拼多多", "超市", "便利店", "商场", "店铺"),
        "交通" to listOf("地铁", "公交", "打车", "滴滴", "高德", "铁路", "机票", "停车"),
        "娱乐" to listOf("电影", "游戏", "音乐", "视频", "会员", "演出"),
        "居家" to listOf("水费", "电费", "燃气", "物业", "家居", "宽带"),
        "医疗" to listOf("医院", "药", "门诊", "体检"),
    )

    fun categoryFor(merchant: String, rawText: String, rules: List<MerchantRuleEntity> = emptyList()): String {
        val haystack = "$merchant $rawText"
        rules.firstOrNull { rule ->
            haystack.contains(rule.pattern, ignoreCase = true)
        }?.let { return it.category }

        keywordCategories.forEach { (category, keywords) ->
            if (keywords.any { haystack.contains(it, ignoreCase = true) }) return category
        }

        return "其他"
    }
}

