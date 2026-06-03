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

    override fun onCreate() {
        super.onCreate()
        serviceJob = SupervisorJob()
    }

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

    override fun onDestroy() {
        serviceJob?.cancel()
        serviceJob = null
        super.onDestroy()
    }
}
