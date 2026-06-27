// MainActivity.kt
package com.studyshield.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.studyshield.services.AppMonitorService
import com.studyshield.utils.DeviceIdUtils

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceId = DeviceIdUtils.getDeviceId(this)
        val prefs = getSharedPreferences("study_shield", Context.MODE_PRIVATE)
        val setupDone = prefs.getBoolean("setup_complete", false)

        if (!setupDone || !hasUsagePermission()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        buildUI(deviceId)
        // Start monitoring service
        AppMonitorService.start(this)
    }

    private fun buildUI(deviceId: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFF0D1B3E.toInt())
            setPadding(48, 96, 48, 48)
        }

        // Logo
        val logo = TextView(this).apply {
            text = "🛡️"
            textSize = 64f
            gravity = android.view.Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Study Shield"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 16, 0, 4)
        }

        val sub = TextView(this).apply {
            text = "Powered by School Connect"
            textSize = 13f
            setTextColor(0xFF0EA5E9.toInt())
            gravity = android.view.Gravity.CENTER
        }

        // Device ID card
        val idCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x1AFFFFFF)
                cornerRadius = 16f
            }
            setPadding(32, 24, 32, 24)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 48
            layoutParams = lp
        }

        val idLabel = TextView(this).apply {
            text = "Your Device ID"
            textSize = 12f
            setTextColor(0xFF94A3B8.toInt())
            letterSpacing = 0.1f
        }

        val idValue = TextView(this).apply {
            text = deviceId
            textSize = 18f
            setTextColor(0xFF22C55E.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 8, 0, 0)
        }

        val idHint = TextView(this).apply {
            text = "Enter this ID in the parent app when adding this device"
            textSize = 11f
            setTextColor(0xFF64748B.toInt())
            setPadding(0, 6, 0, 0)
            lineSpacingExtra = 3f
        }

        idCard.addView(idLabel)
        idCard.addView(idValue)
        idCard.addView(idHint)

        // Status
        val statusTv = TextView(this).apply {
            text = if (AppMonitorService.isRunning) "✅  Monitoring active" else "⏸  Waiting for study session..."
            textSize = 13f
            setTextColor(if (AppMonitorService.isRunning) 0xFF22C55E.toInt() else 0xFF94A3B8.toInt())
            gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 32
            layoutParams = lp
        }

        layout.addView(logo)
        layout.addView(title)
        layout.addView(sub)
        layout.addView(idCard)
        layout.addView(statusTv)
        setContentView(layout)
    }

    private fun hasUsagePermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasUsagePermission()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }
    }
}
