package com.accounting.newapp.capture

/**
 * 银行/支付短信解析器
 *
 * 解析中国主流银行和支付 App 的扣款通知短信，提取金额、商户等信息。
 * 仅解析消费/支付类短信，自动跳过退款、余额查询、验证码、营销等非交易短信。
 */
object BankSmsParser {
    // 单笔消费合理上限：10万元（1,000,000,000 分）
    private const val MAX_REASONABLE_AMOUNT_CENTS = 10_000_000L

    // 已知银行/支付来源 sender 短号 → 名称映射
    val bankSenders = mapOf(
        "95555" to "招商银行",
        "95588" to "工商银行",
        "95533" to "建设银行",
        "95599" to "农业银行",
        "95566" to "中国银行",
        "95559" to "交通银行",
        "95568" to "中信银行",
        "95508" to "广发银行",
        "95561" to "浦发银行",
        "95501" to "兴业银行",
        "95558" to "光大银行",
        "95562" to "民生银行",
        "95577" to "华夏银行",
        "95595" to "邮储银行",
        "1069" to "支付宝",
        "95017" to "微信支付",
    )

    // 【银行名】前缀识别
    private val bankPrefixPattern = Regex("""【([^】]+)】""")

    // 已知的银行/支付来源名称
    private val knownBankNames = setOf(
        "招商银行", "工商银行", "建设银行", "农业银行", "中国银行", "交通银行",
        "中信银行", "广发银行", "浦发银行", "兴业银行", "光大银行", "民生银行",
        "华夏银行", "邮储银行", "微信支付", "支付宝", "云闪付",
    )

    // 交易关键词（必须包含其中之一才算消费短信）
    private val transactionKeywords = listOf("消费", "支出", "支付", "付款", "扣款", "转账出")

    // 排除关键词（包含这些则跳过）
    private val rejectKeywords = listOf("退款", "退回", "已退", "收入", "到账", "转入")
    private val nonTransactionKeywords = listOf("余额", "查询", "验证码", "校验码", "广告", "营销", "抽奖", "优惠", "活动", "提醒")

    // 金额提取模式（优先级从高到低）
    private val keywordAmountPatterns = listOf(
        // "消费人民币20.00元" / "支出人民币20.00元"
        Regex("""(?:消费人民币|消费支出|支出人民币?)\s*(\d+\.?\d{0,2})"""),
        // "支付了￥20.00" / "支付20.00元"
        Regex("""支付了?\s*(?:￥|¥)\s*(\d+\.?\d{0,2})"""),
        // "付款20.00元"
        Regex("""付款\s*(\d+\.?\d{0,2})\s*元"""),
        // "扣款20.00元"
        Regex("""扣款\s*(\d+\.?\d{0,2})\s*元"""),
    )
    private val yenAmountPattern = Regex("""(?:￥|¥)\s*(\d+\.?\d{0,2})""")
    private val yuanAmountPattern = Regex("""(\d+\.?\d{0,2})\s*元""")

    // 商户提取模式 — 注意：使用普通字符串避免 Kotlin 三引号冲突
    private val merchantPatterns = listOf(
        Regex("商户[名号称：:]*[\u201C\u201D\"]?\\s*([^\u201C\u201D\"，。；;\\s)）]+?)[\u201C\u201D\"]?(?=[，,。；;)）]|${'$'})"),
        Regex("""(?:收款方|对方|店铺)[：:]\s*([^,，。；;]+?)(?=[,，。；;]|$)"""),
        Regex("""[(（]商户[：:]\s*([^)）]+)[)）]"""),
        Regex("""[(（]商户名[：:]\s*([^)）]+)[)）]"""),
        Regex("向商户[\u201C\u201D\"]([^\u201C\u201D\"]+)[\u201C\u201D\"]"),
        Regex("""在(.+?)消费"""),
        Regex("""于(.+?)消费"""),
    )

    /**
     * 解析银行/支付短信
     *
     * @param smsBody 短信正文
     * @param senderAddress 发送者短号（如 95555）
     * @param receivedAtMillis 短信接收时间戳
     * @return 解析成功返回 PaymentCapture，否则 null
     */
    fun parse(smsBody: String, senderAddress: String? = null, receivedAtMillis: Long = System.currentTimeMillis()): PaymentCapture? {
        if (smsBody.isBlank()) return null

        // 1. 提取银行前缀
        val bankPrefix = extractBankPrefix(smsBody) ?: return null

        // 2. 排除纯非交易短信（仅在没有交易关键词时生效）
        val hasTransactionKeyword = transactionKeywords.any { smsBody.contains(it) }
        if (!hasTransactionKeyword && nonTransactionKeywords.any { smsBody.contains(it) }) return null
        if (rejectKeywords.any { smsBody.contains(it) }) return null

        // 3. 必须包含交易关键词
        if (!hasTransactionKeyword) return null

        // 4. 提取金额
        val amountCents = extractAmountCents(smsBody) ?: return null
        if (amountCents > MAX_REASONABLE_AMOUNT_CENTS) return null

        // 5. 提取商户
        val merchant = extractMerchant(smsBody).ifBlank { bankPrefix }

        // 6. 确定 sourceApp（优先 sender 映射，否则用银行前缀）
        val sourceApp = senderAddress?.let { bankSenders[it] } ?: bankPrefix

        return PaymentCapture(
            amountCents = amountCents,
            merchant = merchant,
            sourceApp = sourceApp,
            paymentMethod = sourceApp,
            occurredAtMillis = receivedAtMillis,
            confidence = 0.92f,
            rawText = smsBody,
        )
    }

    fun extractBankPrefix(smsBody: String): String? {
        val match = bankPrefixPattern.find(smsBody) ?: return null
        val name = match.groupValues[1]
        return if (knownBankNames.contains(name)) name else null
    }

    fun extractAmountCents(text: String): Long? {
        // 策略1: 关键词后的金额
        for (pattern in keywordAmountPatterns) {
            val match = pattern.find(text) ?: continue
            val amount = match.groupValues[1].toBigDecimalOrNull() ?: continue
            return amount.movePointRight(2).toLong()
        }

        // 策略2: 从所有 ¥ 金额中取最小值（支付金额 < 余额）
        val yenAmounts = yenAmountPattern.findAll(text).mapNotNull {
            it.groupValues[1].toBigDecimalOrNull()?.movePointRight(2)?.toLong()
        }.toList()
        if (yenAmounts.isNotEmpty()) return yenAmounts.min()

        // 策略3: 从所有 "X元" 中取最小值
        val yuanAmounts = yuanAmountPattern.findAll(text).mapNotNull {
            it.groupValues[1].toBigDecimalOrNull()?.movePointRight(2)?.toLong()
        }.toList()
        if (yuanAmounts.isNotEmpty()) return yuanAmounts.min()

        return null
    }

    private fun extractMerchant(text: String): String {
        for (pattern in merchantPatterns) {
            val match = pattern.find(text) ?: continue
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotBlank() && merchant.length >= 2) return merchant.take(32)
        }
        return ""
    }
}