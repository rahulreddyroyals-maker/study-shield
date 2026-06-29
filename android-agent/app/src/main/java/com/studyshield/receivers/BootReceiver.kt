package com.studyshield.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyshield.services.AppMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED)
            AppMonitorService.start(context)
    }
}
