package com.accounting.newapp.capture

object PaymentTextParser {
    private val amountPatterns = listOf(
        Regex("""(?:¥|￥)\s*([0-9]+(?:\.[0-9]{1,2})?)"""),
        Regex("""([0-9]+(?:\.[0-9]{1,2})?)\s*元"""),
        Regex("""(?:付款|支付|消费|扣款|实付|合计|金额)\s*(?:¥|￥)?\s*([0-9]+(?:\.[0-9]{1,2})?)"""),
    )

    private val successKeywords = listOf("支付成功", "付款成功", "交易成功", "扣款成功", "消费", "已支付", "支付完成")
    private val refundKeywords = listOf("退款", "退回", "已退")
    private val merchantLabels = listOf("商户", "收款方", "对方", "店铺", "订单", "付款给")

    fun parse(rawText: String, packageName: String, nowMillis: Long = System.currentTimeMillis()): PaymentCapture? {
        val normalized = rawText.replace(Regex("""\s+"""), " ").trim()
        if (normalized.isBlank()) return null
        if (!successKeywords.any { normalized.contains(it) }) return null
        if (refundKeywords.any { normalized.contains(it) }) return null

        val amountCents = extractAmountCents(normalized) ?: return null
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

    fun extractAmountCents(text: String): Long? {
        val match = amountPatterns.firstNotNullOfOrNull { it.find(text) } ?: return null
        val amount = match.groupValues[1].toBigDecimalOrNull() ?: return null
        return amount.movePointRight(2).toLong()
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
        if (amountPatterns.any { it.containsMatchIn(text) }) score += 0.2f
        if (merchant.isNotBlank()) score += 0.1f
        if (sourceName(packageName) != packageName.substringAfterLast('.')) score += 0.05f
        return score.coerceAtMost(0.98f)
    }
}
