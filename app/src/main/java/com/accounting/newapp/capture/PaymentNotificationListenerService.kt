package com.accounting.newapp.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.accounting.newapp.AccountingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class PaymentNotificationListenerService : NotificationListenerService() {
    private var serviceJob: Job? = null
    private val serviceScope: CoroutineScope get() = CoroutineScope(serviceJob!! + Dispatchers.IO)
    private val recentCaptures = android.util.LruCache<String, Long>(50)
    private val dedupWindowMs = 60_000L

    override fun onCreate() {
        super.onCreate()
        serviceJob = SupervisorJob()
    }

    private fun isDuplicate(fingerprint: String): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = recentCaptures[fingerprint]
        if (lastTime != null && now - lastTime < dedupWindowMs) return true
        recentCaptures.put(fingerprint, now)
        return false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val extras = notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val raw = listOf(title, text, bigText).joinToString("\n")
        val capture = PaymentTextParser.parse(raw, sbn.packageName, sbn.postTime) ?: return

        val fingerprint = "${capture.sourceApp}:${capture.amountCents}:${capture.merchant}"
        if (isDuplicate(fingerprint)) return

        serviceScope.launch {
            (application as AccountingApplication).repository.addCapture(capture)
        }
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        serviceJob = null
        super.onDestroy()
    }
}
