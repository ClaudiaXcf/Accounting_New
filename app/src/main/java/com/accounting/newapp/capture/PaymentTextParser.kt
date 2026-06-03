package com.accounting.newapp.capture

object PaymentTextParser {
    // 支付关键词前缀的金额模式（最优先，因为直接关联支付行为）
    private val keywordAmountPattern = Regex("""(?:付款|支付|消费|扣款|实付|合计|金额)\s*(?:¥|￥)?\s*([0-9]+(?:\.[0-9]{1,2})?)""")
    // 通用金额模式（兜底）
    private val yenPrefixedPattern = Regex("""(?:¥|￥)\s*([0-9]+(?:\.[0-9]{1,2})?)""")
    private val yuanSuffixedPattern = Regex("""([0-9]+(?:\.[0-9]{1,2})?)\s*元""")

    // 单笔支付合理上限：10万元（1,000,000,000 分）
    private const val MAX_REASONABLE_AMOUNT_CENTS = 10_000_000L

    private val successKeywords = listOf("支付成功", "付款成功", "交易成功", "扣款成功", "消费", "已支付", "支付完成")
    private val refundKeywords = listOf("退款", "退回", "已退")
    private val merchantLabels = listOf("商户", "收款方", "对方", "店铺", "订单", "付款给")

    fun parse(rawText: String, packageName: String, nowMillis: Long = System.currentTimeMillis()): PaymentCapture? {
        val normalized = rawText.replace(Regex("""\s+"""), " ").trim()
        if (normalized.isBlank()) return null
        if (!successKeywords.any { normalized.contains(it) }) return null
        if (refundKeywords.any { normalized.contains(it) }) return null

        val amountCents = extractAmountCents(normalized) ?: return null
        // 超过合理上限的金额几乎一定是误捕获了余额，跳过
        if (amountCents > MAX_REASONABLE_AMOUNT_CENTS) return null
        val merchant = extractMerchant(normalized)
        val sourceApp = sourceName(packageName)
        val confidence = score(normalized, merchant, packageName)

        return PaymentCapture(
            amountCents = amountCents,
            merchant = merchant.ifBlank { sourceApp },
            sourceApp = sourceApp,
            paymentMethod = sourceApp,
            occurredAtMillis = nowMillis,
            confidence = confidence,
            rawText = rawText,
        )
    }

    /**
     * 提取金额策略（防止误捕获余额）：
     * 1. 优先提取支付关键词后的金额（最精确）
     * 2. 若无关键词匹配，从所有金额中取最小值（支付金额通常远小于余额）
     * 3. 合理性上限过滤在 parse() 中处理
     */
    fun extractAmountCents(text: String): Long? {
        // 策略1: 支付关键词后的金额（最精确，如"实付¥20.00"）
        val keywordMatch = keywordAmountPattern.find(text)
        if (keywordMatch != null) {
            val amount = keywordMatch.groupValues[1].toBigDecimalOrNull() ?: return null
            return amount.movePointRight(2).toLong()
        }

        // 策略2: 从所有¥金额中取最小值（支付金额 < 余额/可用）
        val allYenAmounts = yenPrefixedPattern.findAll(text).mapNotNull {
            it.groupValues[1].toBigDecimalOrNull()?.movePointRight(2)?.toLong()
        }.toList()
        if (allYenAmounts.isNotEmpty()) {
            return allYenAmounts.min()
        }

        // 策略3: 从所有"X元"中取最小值
        val allYuanAmounts = yuanSuffixedPattern.findAll(text).mapNotNull {
            it.groupValues[1].toBigDecimalOrNull()?.movePointRight(2)?.toLong()
        }.toList()
        if (allYuanAmounts.isNotEmpty()) {
            return allYuanAmounts.min()
        }

        return null
    }

    fun sourceName(packageName: String): String = when {
        packageName.contains("mm", ignoreCase = true) || packageName.contains("wechat", ignoreCase = true) -> "微信支付"
        packageName.contains("alipay", ignoreCase = true) -> "支付宝"
        packageName.contains("unionpay", ignoreCase = true) -> "云闪付"
        packageName.contains("bank", ignoreCase = true) -> "银行"
        packageName.contains("taobao", ignoreCase = true) -> "淘宝"
        packageName.contains("tmall", ignoreCase = true) -> "天猫"
        packageName.contains("jd", ignoreCase = true) -> "京东"
        packageName.contains("pinduoduo", ignoreCase = true) -> "拼多多"
        else -> packageName.substringAfterLast('.').ifBlank { "未知来源" }
    }

    private fun extractMerchant(text: String): String {
        val chunks = text.split(" ", "\n", "，", ",", "。").map { it.trim() }.filter { it.length >= 2 }
        merchantLabels.forEach { label ->
            val index = chunks.indexOfFirst { it.contains(label) }
            if (index >= 0 && index + 1 < chunks.size) return cleanupMerchant(chunks[index + 1])
            chunks.firstOrNull { it.startsWith(label) && it.length > label.length }?.let {
                return cleanupMerchant(it.removePrefix(label).trim(':', '：', ' '))
            }
        }
        return cleanupMerchant(chunks.firstOrNull { !it.contains("成功") && !it.contains("元") && !it.contains("¥") }.orEmpty())
    }

    private fun cleanupMerchant(value: String): String {
        return value
            .replace(Regex("""(?:商户|收款方|对方|店铺|订单|付款给)[:：]?"""), "")
            .replace(Regex("""(?:¥|￥)?[0-9]+(?:\.[0-9]{1,2})?元?"""), "")
            .trim()
            .take(32)
    }

    private fun score(text: String, merchant: String, packageName: String): Float {
        var score = 0.45f
        if (successKeywords.any { text.contains(it) }) score += 0.2f
        if (keywordAmountPattern.containsMatchIn(text) || yenPrefixedPattern.containsMatchIn(text) || yuanSuffixedPattern.containsMatchIn(text)) score += 0.2f
        if (merchant.isNotBlank()) score += 0.1f
        if (sourceName(packageName) != packageName.substringAfterLast('.')) score += 0.05f
        return score.coerceAtMost(0.98f)
    }
}
