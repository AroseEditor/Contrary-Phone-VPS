package com.contrary.phonevps.service

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire() {
        if (wakeLock?.isHeld == true) return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ContraryPhoneVPS::BotWakeLock"
        ).also {
            it.acquire(12 * 60 * 60 * 1000L) // max 12 hours, service restarts it
            Timber.d("WakeLock acquired")
        }
    }

    fun release() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Timber.d("WakeLock released")
            }
        } catch (e: Exception) {
            Timber.e(e, "WakeLock release failed")
        }
        wakeLock = null
    }

    fun isHeld(): Boolean = wakeLock?.isHeld == true
}
