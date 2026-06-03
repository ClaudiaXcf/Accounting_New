package com.accounting.newapp.capture

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.accounting.newapp.AccountingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class PaymentAccessibilityService : AccessibilityService() {
    private var serviceJob: Job? = null
    private val serviceScope: CoroutineScope get() = CoroutineScope(serviceJob!! + Dispatchers.IO)
    private val recentCaptures = android.util.LruCache<String, Long>(50)
    private val dedupWindowMs = 60_000L

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceJob = SupervisorJob()
    }

    private fun isDuplicate(fingerprint: String): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = recentCaptures[fingerprint]
        if (lastTime != null && now - lastTime < dedupWindowMs) return true
        recentCaptures.put(fingerprint, now)
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        val root = rootInActiveWindow ?: return
        try {
            val text = buildString { collectText(root, this) }
            val capture = PaymentTextParser.parse(text, packageName) ?: return
            val fingerprint = "${capture.sourceApp}:${capture.amountCents}:${capture.merchant}"
            if (isDuplicate(fingerprint)) return

            serviceScope.launch {
                (application as AccountingApplication).repository.addCapture(capture)
            }
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceJob?.cancel()
        serviceJob = null
        super.onDestroy()
    }

    private fun collectText(node: AccessibilityNodeInfo, output: StringBuilder) {
        node.text?.let { output.append(it).append('\n') }
        node.contentDescription?.let { output.append(it).append('\n') }
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collectText(child, output)
                child.recycle()
            }
        }
    }
}

