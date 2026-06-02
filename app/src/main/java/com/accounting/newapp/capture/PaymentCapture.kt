package com.accounting.newapp.capture

data class PaymentCapture(
    val amountCents: Long,
    val merchant: String,
    val sourceApp: String,
    val paymentMethod: String,
    val occurredAtMillis: Long = System.currentTimeMillis(),
    val confidence: Float,
    val rawText: String,
)

