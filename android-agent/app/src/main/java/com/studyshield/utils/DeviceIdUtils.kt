package com.studyshield.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdUtils {
    /**
     * Returns a stable, short device ID derived from Android ID.
     * Format: SS-XXXX (e.g. SS-A3F1)
     * This is shown to the student and entered by the parent in the web app.
     */
    fun get(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "fallback_id"

        val hashBytes = MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray(Charsets.UTF_8))
        val shortHash = hashBytes.take(4).joinToString("") { "%02X".format(it) }
        return "SS-$shortHash"
    }
}
