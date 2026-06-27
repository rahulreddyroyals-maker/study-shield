// DeviceIdUtils.kt
package com.studyshield.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdUtils {
    fun get(ctx: Context): String {
        val raw = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "default"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .take(4).joinToString("") { "%02X".format(it) }
        return "SS-$hash"
    }
}

// SessionState.kt
package com.studyshield.utils

object SessionState {
    data class Session(
        val sessionId: String,
        val childId: String,
        val allowedPackages: Set<String>,
        val endTimeMs: Long,
    )

    @Volatile var current: Session? = null
        private set

    fun update(sessionId: String, childId: String, allowedPackages: Set<String>, endTimeMs: Long) {
        current = Session(sessionId, childId, allowedPackages, endTimeMs)
    }

    fun clear() { current = null }
}
