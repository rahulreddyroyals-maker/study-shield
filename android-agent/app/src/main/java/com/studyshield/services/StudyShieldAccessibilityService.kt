// ─── StudyShieldAccessibilityService.kt ──────────────────────────────────────
package com.studyshield.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.studyshield.ui.BlockedActivity
import com.studyshield.utils.SessionState

class StudyShieldAccessibilityService : AccessibilityService() {
    companion object { var isEnabled = false }

    override fun onServiceConnected() {
        isEnabled = true
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg.startsWith("com.android.systemui") || pkg == packageName) return
        val session = SessionState.current ?: return
        if (session.allowedPackages.contains(pkg)) return
        startActivity(Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("PKG", pkg)
        })
    }

    override fun onInterrupt() { isEnabled = false }
    override fun onDestroy() { super.onDestroy(); isEnabled = false }
}
