package com.studyshield

import android.app.Application
import androidx.multidex.MultiDex
import android.content.Context

class StudyShieldApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Firebase is auto-initialized via google-services.json
    }
}
