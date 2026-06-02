package com.accounting.newapp.capture

import com.accounting.newapp.data.MerchantRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizerTest {
    @Test
    fun matchesBuiltInFoodKeyword() {
        assertEquals("餐饮", Categorizer.categoryFor("麦当劳", "支付成功 33元"))
    }

    @Test
    fun learnedRuleWins() {
        val rules = listOf(MerchantRuleEntity(pattern = "星巴克", category = "商务"))

        assertEquals("商务", Categorizer.categoryFor("星巴克", "咖啡 支付成功 28元", rules))
    }

    @Test
    fun fallsBackToOther() {
        assertEquals("其他", Categorizer.categoryFor("未知商户", "支付成功 18元"))
    }
}

