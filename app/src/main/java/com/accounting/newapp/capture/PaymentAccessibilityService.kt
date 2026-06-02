package com.accounting.newapp.capture

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.accounting.newapp.AccountingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFingerprint: String = ""
    private var lastCapturedAt: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        val root = rootInActiveWindow ?: return
        val text = buildString { collectText(root, this) }
        val capture = PaymentTextParser.parse(text, packageName) ?: return
        val fingerprint = "${capture.sourceApp}:${capture.amountCents}:${capture.merchant}"
        val now = System.currentTimeMillis()
        if (fingerprint == lastFingerprint && now - lastCapturedAt < 10_000) return
        lastFingerprint = fingerprint
        lastCapturedAt = now

        serviceScope.launch {
            (application as AccountingApplication).repository.addCapture(capture)
        }
    }

    override fun onInterrupt() = Unit

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

