package com.studyshield.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StudyShieldDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Log.i("SSDeviceAdmin", "Device admin enabled")
    }
    override fun onDisabled(context: Context, intent: Intent) {
        Log.i("SSDeviceAdmin", "Device admin disabled")
    }
}
