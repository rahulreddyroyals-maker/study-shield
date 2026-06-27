// BootReceiver.kt
package com.studyshield.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyshield.services.AppMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {
            AppMonitorService.start(context)
        }
    }
}
