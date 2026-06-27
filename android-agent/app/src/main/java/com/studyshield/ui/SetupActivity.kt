package com.studyshield.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    data class Step(val icon: String, val title: String, val desc: String, val btnText: String, val action: SetupActivity.() -> Unit, val check: SetupActivity.() -> Boolean)

    private val steps by lazy { listOf(
        Step("📊","Usage access","Detect which app is open","Open Settings → Usage access",
            { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            { hasUsageAccess() }),
        Step("🪟","Overlay permission","Show block screen on top of other apps","Grant overlay permission",
            { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
            { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this) }),
        Step("♿","Accessibility","Secondary app detection layer","Open Accessibility → Study Shield",
            { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            { false }), // user must do manually
        Step("✅","All set!","Study Shield is ready. Share the Device ID with the parent app.","Open Study Shield",
            { startActivity(Intent(this, MainActivity::class.java)); finish() },
            { true }),
    ) }

    private var step = 0

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); show(0) }

    private fun show(idx: Int) {
        step = idx
        val s = steps.getOrNull(idx) ?: return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFF0D1B3E.toInt()); setPadding(48,80,48,48)
        }
        root.addView(TextView(this).apply { text = "Step ${idx+1} of ${steps.size}"; textSize=12f; setTextColor(0xFF64748B.toInt()); gravity=android.view.Gravity.CENTER })
        root.addView(TextView(this).apply { text=s.icon; textSize=56f; gravity=android.view.Gravity.CENTER; setPadding(0,24,0,0) })
        root.addView(TextView(this).apply { text=s.title; textSize=22f; setTextColor(0xFFFFFFFF.toInt()); gravity=android.view.Gravity.CENTER; typeface=android.graphics.Typeface.DEFAULT_BOLD; setPadding(0,16,0,8) })
        root.addView(TextView(this).apply { text=s.desc; textSize=14f; setTextColor(0xFF94A3B8.toInt()); gravity=android.view.Gravity.CENTER; lineSpacingExtra=4f })
        root.addView(Button(this).apply {
            text=s.btnText; textSize=14f; isAllCaps=false; setTextColor(0xFFFFFFFF.toInt())
            background=android.graphics.drawable.GradientDrawable().apply { colors=intArrayOf(0xFF22C55E.toInt(),0xFF0EA5E9.toInt()); orientation=android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT; cornerRadius=14f }
            val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.topMargin=40; layoutParams=lp; setPadding(0,40,0,40)
            setOnClickListener { s.action(this@SetupActivity) }
        })
        if (idx < steps.size - 1) {
            root.addView(Button(this).apply {
                text="Skip →"; textSize=13f; isAllCaps=false; setTextColor(0xFF22C55E.toInt()); setBackgroundColor(0)
                val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.topMargin=8; layoutParams=lp
                setOnClickListener { show(idx+1) }
            })
        }
        setContentView(root)
    }

    private fun hasUsageAccess() = try {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    } catch(e:Exception){false}

    override fun onResume() {
        super.onResume()
        val s = steps.getOrNull(step) ?: return
        if (s.check(this) && step < steps.size - 1) show(step + 1)
    }
}
