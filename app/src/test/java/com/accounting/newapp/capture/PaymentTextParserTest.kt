package com.accounting.newapp.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentTextParserTest {
    @Test
    fun parsesWechatPaymentAmount() {
        val capture = PaymentTextParser.parse("微信支付 支付成功 商户 星巴克 ¥12.30", "com.tencent.mm", 1000L)

        assertNotNull(capture)
        assertEquals(1230L, capture!!.amountCents)
        assertEquals("微信支付", capture.sourceApp)
        assertEquals("星巴克", capture.merchant)
    }

    @Test
    fun parsesYuanSuffixAmount() {
        val capture = PaymentTextParser.parse("支付宝 付款成功 收款方 便利店 88元", "com.eg.android.AlipayGphone", 1000L)

        assertNotNull(capture)
        assertEquals(8800L, capture!!.amountCents)
        assertEquals("支付宝", capture.sourceApp)
    }

    @Test
    fun ignoresRefundText() {
        val capture = PaymentTextParser.parse("退款成功 12.30元", "com.tencent.mm", 1000L)

        assertNull(capture)
    }
}

