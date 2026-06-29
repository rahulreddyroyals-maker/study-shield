package com.studyshield.ui

import android.app.AppOpsManager
import android.content.*
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.studyshield.services.AppMonitorService
import com.studyshield.utils.DeviceIdUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasUsageAccess() || !hasOverlay()) {
            startActivity(Intent(this, SetupActivity::class.java)); finish(); return
        }
        buildUI()
        AppMonitorService.start(this)
    }

    private fun buildUI() {
        val deviceId = DeviceIdUtils.get(this)
        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF0D1B3E.toInt()) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(48, 96, 48, 80)
        }

        root.addView(TextView(this).apply { text = "🛡️"; textSize = 72f; gravity = Gravity.CENTER })
        root.addView(TextView(this).apply {
            text = "Study Shield"; textSize = 30f; setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setPadding(0,16,0,4)
        })
        root.addView(TextView(this).apply {
            text = "Powered by School Connect"; textSize = 13f
            setTextColor(0xFF0EA5E9.toInt()); gravity = Gravity.CENTER; setPadding(0,0,0,48)
        })

        // Device ID card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(36,28,36,28)
            background = GradientDrawable().apply { setColor(0x14FFFFFF); cornerRadius=20f; setStroke(1,0x33FFFFFF) }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 20; layoutParams = lp
        }
        card.addView(TextView(this).apply { text="DEVICE ID"; textSize=11f; setTextColor(0xFF94A3B8.toInt()); letterSpacing=0.12f })
        card.addView(TextView(this).apply { text=deviceId; textSize=28f; setTextColor(0xFF22C55E.toInt()); typeface=Typeface.MONOSPACE; setPadding(0,10,0,10) })
        card.addView(TextView(this).apply { text="Enter this in the parent app when adding this device."; textSize=12f; setTextColor(0xFF64748B.toInt()); lineSpacingExtra=4f })
        root.addView(card)

        // Copy button
        root.addView(Button(this).apply {
            text = "📋  Copy Device ID"; textSize=15f; isAllCaps=false; setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply { colors=intArrayOf(0xFF22C55E.toInt(),0xFF0EA5E9.toInt()); orientation=GradientDrawable.Orientation.LEFT_RIGHT; cornerRadius=14f }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin=16; layoutParams=lp; setPadding(0,44,0,44)
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                Toast.makeText(context,"✅ Device ID copied!",Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(TextView(this).apply {
            text = if (AppMonitorService.isRunning) "✅  Monitoring active" else "⏸  Waiting for study session..."
            textSize=13f; setTextColor(if (AppMonitorService.isRunning) 0xFF22C55E.toInt() else 0xFF94A3B8.toInt())
            gravity=Gravity.CENTER; setPadding(0,8,0,0)
        })
        root.addView(TextView(this).apply {
            text="Study Shield v1.0 · studyshield-ai.web.app"; textSize=11f
            setTextColor(0xFF334155.toInt()); gravity=Gravity.CENTER; setPadding(0,48,0,0)
        })

        scroll.addView(root); setContentView(scroll)
    }

    fun hasUsageAccess(): Boolean = try {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }

    fun hasOverlay() = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    override fun onResume() {
        super.onResume()
        if (!hasUsageAccess() || !hasOverlay()) { startActivity(Intent(this, SetupActivity::class.java)); finish() }
    }
}
