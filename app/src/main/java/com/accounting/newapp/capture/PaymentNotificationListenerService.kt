package com.accounting.newapp.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.accounting.newapp.AccountingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val extras = notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val raw = listOf(title, text, bigText).joinToString("\n")
        val capture = PaymentTextParser.parse(raw, sbn.packageName, sbn.postTime) ?: return

        serviceScope.launch {
            (application as AccountingApplication).repository.addCapture(capture)
        }
    }
}
