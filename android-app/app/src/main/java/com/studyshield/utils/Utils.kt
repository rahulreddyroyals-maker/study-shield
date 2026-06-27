// utils/DeviceIdUtils.kt
package com.studyshield.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdUtils {
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        // Create a short, readable ID
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray())
            .take(4)
            .joinToString("") { "%02X".format(it) }
        return "SS-$hash"
    }
}

// utils/AppBlockerUtils.kt
package com.studyshield.utils

import android.content.Context
import android.content.pm.PackageManager

object AppBlockerUtils {
    fun getAppName(context: Context, packageName: String): String {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast(".")
        }
    }

    fun isSystemApp(context: Context, packageName: String): Boolean {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }
}

// utils/SessionState.kt
package com.studyshield.utils

data class ActiveSession(
    val sessionId: String,
    val childId: String,
    val allowedPackages: Set<String>,
    val endTimeMs: Long,
)

object SessionState {
    var current: ActiveSession? = null
}
