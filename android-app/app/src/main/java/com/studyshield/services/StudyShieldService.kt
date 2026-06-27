// StudyShieldService.kt
// Accessibility Service — provides a secondary layer of app blocking.
// When Android detects a window change, this fires and can redirect.
package com.studyshield.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.studyshield.ui.BlockedActivity
import com.studyshield.utils.AppBlockerUtils
import com.studyshield.utils.SessionState

class StudyShieldService : AccessibilityService() {

    companion object {
        const val TAG = "StudyShieldA11y"
        var isEnabled = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isEnabled = true
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip system UI and own package
        if (packageName.startsWith("com.android.systemui")) return
        if (packageName == this.packageName) return

        // Check if session is active and this package is blocked
        val session = SessionState.current ?: return
        if (session.allowedPackages.contains(packageName)) return

        Log.d(TAG, "Accessibility blocking: $packageName")
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("BLOCKED_APP", packageName)
            putExtra("APP_NAME", AppBlockerUtils.getAppName(this@StudyShieldService, packageName))
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        isEnabled = false
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isEnabled = false
    }
}
