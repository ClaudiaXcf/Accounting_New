package com.accounting.newapp.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.LruCache
import com.accounting.newapp.AccountingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsTransactionReceiver : BroadcastReceiver() {
    private val recentCaptures = LruCache<String, Long>(50)
    private val dedupWindowMs = 60_000L

    private fun isDuplicate(fingerprint: String): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = recentCaptures[fingerprint]
        if (lastTime != null && now - lastTime < dedupWindowMs) return true
        recentCaptures.put(fingerprint, now)
        return false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val body = messages.joinToString("") { it.messageBody ?: "" }
        if (body.isBlank()) return

        val senderAddress = messages.firstOrNull()?.originatingAddress
        val now = System.currentTimeMillis()

        val capture = BankSmsParser.parse(body, senderAddress, now) ?: return
        val fingerprint = "${capture.sourceApp}:${capture.amountCents}:${capture.merchant}"
        if (isDuplicate(fingerprint)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = (context.applicationContext as AccountingApplication).repository
                repository.addCapture(capture)
            } finally {
                pendingResult.finish()
            }
        }
    }
}