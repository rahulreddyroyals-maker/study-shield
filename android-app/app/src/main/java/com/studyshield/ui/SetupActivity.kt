// SetupActivity.kt — Step-by-step permission wizard
package com.studyshield.ui

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.studyshield.receivers.DeviceAdminReceiver

class SetupActivity : AppCompatActivity() {

    private var currentStep = 0
    private val steps = listOf(
        SetupStep("Usage access", "Allow Study Shield to track which apps are open", "Open settings") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        },
        SetupStep("Overlay permission", "Allows blocking screen to appear over other apps", "Grant overlay") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        },
        SetupStep("Accessibility service", "Detects when blocked apps are opened", "Open accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        SetupStep("Device admin (optional)", "Prevents students from uninstalling the app", "Enable admin") {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, DeviceAdminReceiver::class.java)
            if (!dpm.isAdminActive(admin)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Prevents students from uninstalling Study Shield")
                }
                startActivity(intent)
            }
        },
    )

    data class SetupStep(val title: String, val desc: String, val btnLabel: String, val action: SetupActivity.() -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showStep(0)
    }

    private fun showStep(step: Int) {
        currentStep = step
        val s = steps.getOrNull(step)

        if (s == null) {
            // All steps done
            getSharedPreferences("study_shield", Context.MODE_PRIVATE)
                .edit().putBoolean("setup_complete", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFF0D1B3E.toInt())
            setPadding(48, 96, 48, 48)
        }

        // Progress
        val progress = TextView(this).apply {
            text = "Step ${step + 1} of ${steps.size}"
            textSize = 12f
            setTextColor(0xFF64748B.toInt())
            gravity = android.view.Gravity.CENTER
        }

        val icon = TextView(this).apply {
            text = listOf("📊","🪟","♿","🛡️")[step]
            textSize = 56f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        val title = TextView(this).apply {
            text = s.title
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 16, 0, 8)
        }

        val desc = TextView(this).apply {
            text = s.desc
            textSize = 14f
            setTextColor(0xFF94A3B8.toInt())
            gravity = android.view.Gravity.CENTER
            lineSpacingExtra = 4f
        }

        val actionBtn = Button(this).apply {
            text = s.btnLabel
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                colors = intArrayOf(0xFF22C55E.toInt(), 0xFF0EA5E9.toInt())
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = 16f
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 48
            layoutParams = lp
            setPadding(0, 40, 0, 40)
            isAllCaps = false
            setOnClickListener { s.action(this@SetupActivity) }
        }

        val nextBtn = Button(this).apply {
            text = if (step < steps.size - 1) "Next →" else "Finish setup"
            textSize = 14f
            setTextColor(0xFF22C55E.toInt())
            setBackgroundColor(0x00000000)
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 12
            layoutParams = lp
            setOnClickListener { showStep(step + 1) }
        }

        layout.addView(progress)
        layout.addView(icon)
        layout.addView(title)
        layout.addView(desc)
        layout.addView(actionBtn)
        layout.addView(nextBtn)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        // Auto-advance if permission was already granted
        if (currentStep == 0 && hasUsageAccess()) showStep(1)
        if (currentStep == 1 && hasOverlay()) showStep(2)
    }

    private fun hasUsageAccess(): Boolean {
        return try {
            val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    private fun hasOverlay(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }
}
