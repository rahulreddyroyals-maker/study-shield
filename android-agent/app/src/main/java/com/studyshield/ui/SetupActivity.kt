package com.studyshield.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    private var currentStep = 0

    data class Step(
        val emoji: String,
        val title: String,
        val description: String,
        val buttonLabel: String,
        val onAction: SetupActivity.() -> Unit,
        val isGranted: SetupActivity.() -> Boolean
    )

    private val steps by lazy {
        listOf(
            Step(
                emoji = "📊",
                title = "Usage access",
                description = "Allows Study Shield to detect which app is currently open. Required for app blocking to work.\n\nSteps:\n1. Tap the button below\n2. Find \"Study Shield\" in the list\n3. Enable \"Permit usage access\"",
                buttonLabel = "Open Usage Access Settings",
                onAction = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                isGranted = {
                    try {
                        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                        ops.checkOpNoThrow(
                            AppOpsManager.OPSTR_GET_USAGE_STATS,
                            android.os.Process.myUid(), packageName
                        ) == AppOpsManager.MODE_ALLOWED
                    } catch (e: Exception) { false }
                }
            ),
            Step(
                emoji = "🪟",
                title = "Overlay permission",
                description = "Allows the blocking screen to appear on top of restricted apps.\n\nSteps:\n1. Tap the button below\n2. Find \"Study Shield\"\n3. Enable \"Allow display over other apps\"",
                buttonLabel = "Grant Overlay Permission",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        startActivity(Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        ))
                    }
                },
                isGranted = {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
                }
            ),
            Step(
                emoji = "♿",
                title = "Accessibility service",
                description = "Provides a second layer of app detection, making blocking more reliable.\n\nSteps:\n1. Tap the button below\n2. Find \"Study Shield\" under Installed Services\n3. Toggle it ON\n4. Tap OK on the confirmation dialog",
                buttonLabel = "Open Accessibility Settings",
                onAction = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                isGranted = { false }  // Can't programmatically check; user confirms manually
            ),
            Step(
                emoji = "✅",
                title = "All set!",
                description = "Study Shield is fully configured.\n\nShare the Device ID shown on the home screen with the parent to link this phone.\n\nThe parent starts study sessions from studyshield-ai.web.app",
                buttonLabel = "Open Study Shield",
                onAction = { startActivity(Intent(this, MainActivity::class.java)); finish() },
                isGranted = { true }
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showStep(0)
    }

    private fun showStep(index: Int) {
        currentStep = index
        val step = steps.getOrNull(index) ?: run { finish(); return }

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF0D1B3E.toInt()) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 80, 48, 80)
        }

        // Step counter
        root.addView(TextView(this).apply {
            text = "Step ${index + 1} of ${steps.size}"
            textSize = 12f
            setTextColor(0xFF64748B.toInt())
            gravity = Gravity.CENTER
        })

        // Progress bar
        val progressBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 12; lp.bottomMargin = 32; layoutParams = lp
        }
        repeat(steps.size) { i ->
            progressBar.addView(View(this).apply {
                val segLp = LinearLayout.LayoutParams(0, 6, 1f)
                segLp.setMargins(3, 0, 3, 0)
                layoutParams = segLp
                background = GradientDrawable().apply {
                    cornerRadius = 3f
                    setColor(if (i <= index) 0xFF22C55E.toInt() else 0xFF1E3A5F.toInt())
                }
            })
        }
        root.addView(progressBar)

        // Emoji
        root.addView(TextView(this).apply {
            text = step.emoji
            textSize = 64f
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 8; layoutParams = lp
        })

        // Title
        root.addView(TextView(this).apply {
            text = step.title
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 16; layoutParams = lp
        })

        // Description
        root.addView(TextView(this).apply {
            text = step.description
            textSize = 14f
            setTextColor(0xFF94A3B8.toInt())
            lineSpacingExtra = 5f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 40; layoutParams = lp
        })

        // Action button
        root.addView(Button(this).apply {
            text = step.buttonLabel
            textSize = 15f; isAllCaps = false
            setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                colors = intArrayOf(0xFF22C55E.toInt(), 0xFF0EA5E9.toInt())
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = 14f
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 12; layoutParams = lp; setPadding(0, 44, 0, 44)
            setOnClickListener { step.onAction(this@SetupActivity) }
        })

        // Next / skip button
        if (index < steps.size - 1) {
            root.addView(Button(this).apply {
                text = if (index == 2) "I've done this →" else "Next →"
                textSize = 14f; isAllCaps = false
                setTextColor(0xFF22C55E.toInt())
                setBackgroundColor(0x00000000)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams = lp
                setOnClickListener { showStep(index + 1) }
            })
        }

        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        // Auto-advance if permission was already granted
        val step = steps.getOrNull(currentStep) ?: return
        if (step.isGranted(this) && currentStep < steps.size - 1) {
            showStep(currentStep + 1)
        }
    }
}
