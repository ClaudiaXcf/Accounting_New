package com.accounting.newapp.capture

import org.junit.Assert.*
import org.junit.Test

class BankSmsParserTest {

    // === 招商银行 ===

    @Test
    fun parsesCmbConsumptionSms() {
        val sms = "【招商银行】您账户1234于06月03日15:25消费人民币20.00元，商户：拼多多。"
        val result = BankSmsParser.parse(sms, "95555")!!
        assertEquals(2000L, result.amountCents)
        assertEquals("拼多多", result.merchant)
        assertEquals("招商银行", result.sourceApp)
        assertEquals(0.92f, result.confidence)
    }

    @Test
    fun parsesCmbConsumptionSmsWithoutSender() {
        val sms = "【招商银行】您账户5678于06月03日20:10消费人民币158.50元，商户：美团外卖。"
        val result = BankSmsParser.parse(sms)!!
        assertEquals(15850L, result.amountCents)
        assertEquals("美团外卖", result.merchant)
        assertEquals("招商银行", result.sourceApp)
    }

    // === 工商银行 ===

    @Test
    fun parsesIcbcSms() {
        val sms = "【工商银行】您尾号8888卡06月03日15:30消费支出20.00元，商户：美团。"
        val result = BankSmsParser.parse(sms, "95588")!!
        assertEquals(2000L, result.amountCents)
        assertEquals("美团", result.merchant)
        assertEquals("工商银行", result.sourceApp)
    }

    // === 建设银行 ===

    @Test
    fun parsesCcbSms() {
        val sms = "【建设银行】您尾号6666的储蓄卡06月03日10:00消费支出35.50元，商户：瑞幸咖啡。"
        val result = BankSmsParser.parse(sms, "95533")!!
        assertEquals(3550L, result.amountCents)
        assertEquals("瑞幸咖啡", result.merchant)
        assertEquals("建设银行", result.sourceApp)
    }

    // === 微信支付 ===

    @Test
    fun parsesWechatPaySms() {
        val sms = "【微信支付】微信支付凭证：您于2024年06月03日15:20向商户\"瑞幸咖啡\"支付了￥20.00"
        val result = BankSmsParser.parse(sms, "95017")!!
        assertEquals(2000L, result.amountCents)
        assertEquals("瑞幸咖啡", result.merchant)
        assertEquals("微信支付", result.sourceApp)
    }

    // === 支付宝 ===

    @Test
    fun parsesAlipaySms() {
        val sms = "【支付宝】付款成功20.00元(商户:美团)"
        val result = BankSmsParser.parse(sms, "1069")!!
        assertEquals(2000L, result.amountCents)
        assertEquals("美团", result.merchant)
        assertEquals("支付宝", result.sourceApp)
    }

    // === 退款短信 ===

    @Test
    fun rejectsRefundSms() {
        val sms = "【招商银行】您账户1234于06月03日16:00退款人民币20.00元，商户：拼多多。"
        assertNull(BankSmsParser.parse(sms))
    }

    @Test
    fun rejectsRefundKeyword() {
        val sms = "【招商银行】您账户1234已退回人民币20.00元。"
        assertNull(BankSmsParser.parse(sms))
    }

    // === 非交易短信 ===

    @Test
    fun rejectsBalanceInquirySms() {
        val sms = "【招商银行】您账户1234余额查询：可用余额2,000,000.00元。"
        assertNull(BankSmsParser.parse(sms))
    }

    @Test
    fun rejectsVerificationCodeSms() {
        val sms = "【招商银行】您正在登录，验证码：123456，10分钟内有效。"
        assertNull(BankSmsParser.parse(sms))
    }

    @Test
    fun rejectsMarketingSms() {
        val sms = "【招商银行】招行信用卡优惠活动，全场满100减20！快来参加吧。"
        assertNull(BankSmsParser.parse(sms))
    }

    // === 边界情况 ===

    @Test
    fun rejectsCompletelyUnrelatedSms() {
        val sms = "【快递100】您的包裹已送达驿站，取件码5432。"
        assertNull(BankSmsParser.parse(sms))
    }

    @Test
    fun rejectsLargeAmount() {
        val sms = "【招商银行】您账户1234于06月03日15:25消费人民币2000000.00元，商户：测试。"
        assertNull(BankSmsParser.parse(sms))
    }

    @Test
    fun extractsMinimumAmountWhenMultiplePresent() {
        val sms = "【招商银行】您账户1234余额￥2000000.00，06月03日消费￥20.00元，商户：测试。"
        val result = BankSmsParser.parse(sms)!!
        // Should pick the spending amount (20.00) not the balance (2000000.00)
        // "消费" keyword pattern takes priority over generic ¥ pattern
        assertEquals(2000L, result.amountCents)
    }

    @Test
    fun rejectsSmsWithoutBankPrefix() {
        val sms = "您账户消费20.00元，商户：测试。"
        assertNull(BankSmsParser.parse(sms))
    }

    @Test
    fun rejectsSmsWithUnknownPrefix() {
        val sms = "【某某贷款】您借款消费20.00元。"
        assertNull(BankSmsParser.parse(sms))
    }

    // === 金额提取 ===

    @Test
    fun extractsAmountCentsFromYuanNotation() {
        assertEquals(2000L, BankSmsParser.extractAmountCents("消费20.00元"))
    }

    @Test
    fun extractsAmountCentsFromYenPrefix() {
        assertEquals(2000L, BankSmsParser.extractAmountCents("支付￥20.00"))
    }

    @Test
    fun extractsAmountCentsFromKeywordPattern() {
        assertEquals(2000L, BankSmsParser.extractAmountCents("消费人民币20.00元"))
    }

    @Test
    fun returnsNullForNoAmount() {
        assertNull(BankSmsParser.extractAmountCents("这是一条没有金额的短信"))
    }

    // === 银行前缀提取 ===

    @Test
    fun extractsKnownBankPrefix() {
        assertEquals("招商银行", BankSmsParser.extractBankPrefix("【招商银行】您账户..."))
    }

    @Test
    fun returnsNullForUnknownPrefix() {
        assertNull(BankSmsParser.extractBankPrefix("【某某贷款】您账户..."))
    }

    @Test
    fun returnsNullForNoPrefix() {
        assertNull(BankSmsParser.extractBankPrefix("没有前缀的短信"))
    }

    // === 农业银行 ===

    @Test
    fun parsesAbcSms() {
        val sms = "【农业银行】您尾号1234账户于06月03日消费支出50.00元，商户：滴滴出行。"
        val result = BankSmsParser.parse(sms, "95599")!!
        assertEquals(5000L, result.amountCents)
        assertEquals("滴滴出行", result.merchant)
        assertEquals("农业银行", result.sourceApp)
    }

    // === 收入类排除 ===

    @Test
    fun rejectsIncomeSms() {
        val sms = "【招商银行】您账户1234于06月03日收入人民币5000.00元，对方：某某公司。"
        assertNull(BankSmsParser.parse(sms))
    }

    @Test
    fun rejectsTransferInSms() {
        val sms = "【工商银行】您尾号8888卡06月03日转入人民币100.00元。"
        assertNull(BankSmsParser.parse(sms))
    }
}