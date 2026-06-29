package com.studyshield.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdUtils {
    fun get(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: "fallback"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray())
            .take(4).joinToString("") { "%02X".format(it) }
        return "SS-$hash"
    }
}
