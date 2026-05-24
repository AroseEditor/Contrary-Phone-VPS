package com.contrary.phonevps.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.contrary.phonevps.data.repository.BotRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var botRepository: BotRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) return

        Timber.d("Boot completed — checking auto-start bots")

        CoroutineScope(Dispatchers.IO).launch {
            val autoStartBots = botRepository.getAutoStartBots()
            if (autoStartBots.isNotEmpty()) {
                Timber.d("Auto-starting ${autoStartBots.size} bots after boot")
                val serviceIntent = Intent(context, BotForegroundService::class.java).apply {
                    action = BotForegroundService.ACTION_START_ALL_AUTO
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
