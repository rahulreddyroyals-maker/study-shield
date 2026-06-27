// BootReceiver.kt
package com.studyshield.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyshield.services.AppMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            AppMonitorService.start(ctx)
        }
    }
}
