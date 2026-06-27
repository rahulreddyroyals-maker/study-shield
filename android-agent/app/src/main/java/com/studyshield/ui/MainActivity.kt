package com.studyshield.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.studyshield.services.AppMonitorService
import com.studyshield.utils.DeviceIdUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasUsagePermission() || !hasOverlay()) {
            startActivity(Intent(this, SetupActivity::class.java)); finish(); return
        }
        buildUI()
        AppMonitorService.start(this)
    }

    private fun buildUI() {
        val deviceId = DeviceIdUtils.get(this)
        val root = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 96, 48, 48)
            setBackgroundColor(0xFF0D1B3E.toInt())
        }

        layout.addView(TextView(this).apply { text = "🛡️"; textSize = 64f; gravity = android.view.Gravity.CENTER })
        layout.addView(TextView(this).apply {
            text = "Study Shield"; textSize = 28f; setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0,16,0,4)
        })
        layout.addView(TextView(this).apply {
            text = "Powered by School Connect"; textSize = 13f; setTextColor(0xFF0EA5E9.toInt())
            gravity = android.view.Gravity.CENTER; setPadding(0,0,0,40)
        })

        // Device ID card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32,24,32,24)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x1AFFFFFF); cornerRadius = 16f
                setStroke(1, 0x33FFFFFF)
            }
        }
        card.addView(TextView(this).apply {
            text = "DEVICE ID — Enter this in parent app"
            textSize = 11f; setTextColor(0xFF94A3B8.toInt()); letterSpacing = 0.05f
        })
        card.addView(TextView(this).apply {
            text = deviceId; textSize = 24f; setTextColor(0xFF22C55E.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0,8,0,8)
        })
        card.addView(TextView(this).apply {
            text = "Share this with the parent to link this device"; textSize = 11f
            setTextColor(0xFF64748B.toInt())
        })
        layout.addView(card)

        // Copy button
        layout.addView(Button(this).apply {
            text = "📋  Copy Device ID"
            textSize = 14f; isAllCaps = false; setTextColor(0xFFFFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                colors = intArrayOf(0xFF22C55E.toInt(), 0xFF0EA5E9.toInt())
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = 12f
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 24; layoutParams = lp; setPadding(0,40,0,40)
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Device ID", deviceId))
                Toast.makeText(context, "Device ID copied!", Toast.LENGTH_SHORT).show()
            }
        })

        // Status
        layout.addView(TextView(this).apply {
            text = if (AppMonitorService.isRunning) "✅  Monitoring active" else "⏸  Waiting for study session..."
            textSize = 13f
            setTextColor(if (AppMonitorService.isRunning) 0xFF22C55E.toInt() else 0xFF94A3B8.toInt())
            gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 24; layoutParams = lp
        })

        root.addView(layout); root.setBackgroundColor(0xFF0D1B3E.toInt())
        setContentView(root)
    }

    private fun hasUsagePermission(): Boolean {
        return try {
            val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    private fun hasOverlay() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    override fun onResume() {
        super.onResume()
        if (!hasUsagePermission() || !hasOverlay()) {
            startActivity(Intent(this, SetupActivity::class.java)); finish()
        }
    }
}
