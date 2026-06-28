package com.studyshield.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.studyshield.ui.BlockedActivity
import com.studyshield.utils.SessionState

class StudyShieldAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "SSAccessibility"
        @Volatile var isEnabled = false
    }

    override fun onServiceConnected() {
        isEnabled = true
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100L
        }
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Skip system and own package
        if (pkg.startsWith("com.android.systemui")) return
        if (pkg == packageName) return

        val session = SessionState.current ?: return
        if (session.allowedPackages.contains(pkg)) return

        Log.i(TAG, "Accessibility blocking: $pkg")
        startActivity(Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("BLOCKED_PKG", pkg)
        })
    }

    override fun onInterrupt() {
        isEnabled = false
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isEnabled = false
    }
}
