package com.contrary.phonevps

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ContraryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        // Initialize Chaquopy Python runtime
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
