package com.contrary.phonevps.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chaquo.python.PyObject
import com.contrary.phonevps.MainActivity
import com.contrary.phonevps.R
import com.contrary.phonevps.data.model.BotRuntimeState
import com.contrary.phonevps.data.model.BotStatus
import com.contrary.phonevps.data.model.LogEntry
import com.contrary.phonevps.data.model.LogLevel
import com.contrary.phonevps.data.repository.BotRepository
import com.contrary.phonevps.python.PythonBridge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class BotForegroundService : Service() {

    companion object {
        const val ACTION_START_BOT = "com.contrary.phonevps.START_BOT"
        const val ACTION_STOP_BOT = "com.contrary.phonevps.STOP_BOT"
        const val ACTION_RESTART_BOT = "com.contrary.phonevps.RESTART_BOT"
        const val ACTION_START_ALL_AUTO = "com.contrary.phonevps.START_ALL_AUTO"
        const val EXTRA_BOT_ID = "bot_id"

        const val CHANNEL_ID = "contrary_vps_service"
        const val CHANNEL_ID_BOTS = "contrary_vps_bots"
        const val NOTIFICATION_ID = 1001

        // Shared runtime state — observable from ViewModels
        private val _runtimeStates = MutableStateFlow<Map<String, BotRuntimeState>>(emptyMap())
        val runtimeStates: StateFlow<Map<String, BotRuntimeState>> = _runtimeStates

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        fun updateState(botId: String, state: BotRuntimeState) {
            val current = _runtimeStates.value.toMutableMap()
            current[botId] = state
            _runtimeStates.value = current
        }

        fun removeState(botId: String) {
            val current = _runtimeStates.value.toMutableMap()
            current.remove(botId)
            _runtimeStates.value = current
        }
    }

    @Inject lateinit var botRepository: BotRepository
    @Inject lateinit var pythonBridge: PythonBridge
    @Inject lateinit var wakeLockManager: WakeLockManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val botRunnerModule: PyObject by lazy { pythonBridge.getBotRunnerModule() }
    private val restartJobs = ConcurrentHashMap<String, Job>()
    private val startTimes = ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildNotification("Running — 0 bots active"))
        _isServiceRunning.value = true
        Timber.d("BotForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_BOT -> {
                val botId = intent.getStringExtra(EXTRA_BOT_ID) ?: return START_STICKY
                serviceScope.launch { startBot(botId) }
            }
            ACTION_STOP_BOT -> {
                val botId = intent.getStringExtra(EXTRA_BOT_ID) ?: return START_STICKY
                serviceScope.launch { stopBot(botId) }
            }
            ACTION_RESTART_BOT -> {
                val botId = intent.getStringExtra(EXTRA_BOT_ID) ?: return START_STICKY
                serviceScope.launch {
                    stopBot(botId)
                    delay(1000)
                    startBot(botId)
                }
            }
            ACTION_START_ALL_AUTO -> {
                serviceScope.launch {
                    val bots = botRepository.getAutoStartBots()
                    bots.forEach { bot -> startBot(bot.id) }
                }
            }
        }
        return START_STICKY
    }

    private suspend fun startBot(botId: String) {
        val bot = botRepository.getBotById(botId) ?: run {
            Timber.e("Bot $botId not found")
            return
        }
        val token = botRepository.getToken(botId) ?: run {
            log(botId, LogLevel.ERROR, "No token found for bot ${bot.name}")
            return
        }

        updateState(botId, BotRuntimeState(botId, BotStatus.STARTING))
        log(botId, LogLevel.SYSTEM, "Starting bot: ${bot.name}")
        wakeLockManager.acquire()
        startTimes[botId] = System.currentTimeMillis()

        try {
            val callback = { level: String, message: String ->
                val logLevel = when (level) {
                    "STDOUT" -> LogLevel.STDOUT
                    "STDERR" -> LogLevel.STDERR
                    "ERROR" -> LogLevel.ERROR
                    "WARN" -> LogLevel.WARN
                    "SYSTEM" -> LogLevel.SYSTEM
                    else -> LogLevel.INFO
                }
                serviceScope.launch { log(botId, logLevel, message) }
                Unit
            }

            val started = botRunnerModule.callAttr(
                "start_bot",
                botId,
                bot.scriptPath,
                token,
                callback,
            ).toJava(Boolean::class.java)

            if (started) {
                updateState(botId, BotRuntimeState(botId, BotStatus.RUNNING))
                log(botId, LogLevel.SYSTEM, "Bot ${bot.name} started successfully")
                updateNotification()
                monitorBot(botId, bot.autoRestart, bot.maxRestarts)
            } else {
                updateState(botId, BotRuntimeState(botId, BotStatus.ERROR, lastError = "Already running or failed to start"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start bot $botId")
            updateState(botId, BotRuntimeState(botId, BotStatus.ERROR, lastError = e.message))
            log(botId, LogLevel.ERROR, "Failed to start: ${e.message}")
        }
    }

    private fun monitorBot(botId: String, autoRestart: Boolean, maxRestarts: Int) {
        val job = serviceScope.launch {
            var restartCount = 0
            while (isActive) {
                delay(2000)
                val isRunning = try {
                    botRunnerModule.callAttr("is_bot_running", botId).toJava(Boolean::class.java)
                } catch (e: Exception) { false }

                // Update uptime
                val uptime = (System.currentTimeMillis() - (startTimes[botId] ?: System.currentTimeMillis())) / 1000
                val current = _runtimeStates.value[botId]
                if (current != null && current.status == BotStatus.RUNNING) {
                    updateState(botId, current.copy(uptimeSeconds = uptime))
                }

                if (!isRunning) {
                    val currentState = _runtimeStates.value[botId]
                    if (currentState?.status == BotStatus.STOPPED) break // intentionally stopped

                    log(botId, LogLevel.WARN, "Bot stopped unexpectedly")
                    if (autoRestart && restartCount < maxRestarts) {
                        restartCount++
                        updateState(botId, BotRuntimeState(botId, BotStatus.CRASHING, restartCount = restartCount))
                        log(botId, LogLevel.SYSTEM, "Auto-restarting (attempt $restartCount/$maxRestarts) in 5s...")
                        delay(5000)
                        startBot(botId)
                    } else {
                        updateState(botId, BotRuntimeState(botId, BotStatus.ERROR,
                            lastError = if (restartCount >= maxRestarts) "Max restarts reached" else "Stopped"))
                        updateNotification()
                        break
                    }
                }
            }
        }
        restartJobs[botId] = job
    }

    private suspend fun stopBot(botId: String) {
        restartJobs[botId]?.cancel()
        restartJobs.remove(botId)

        try {
            botRunnerModule.callAttr("stop_bot", botId)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping bot $botId")
        }

        updateState(botId, BotRuntimeState(botId, BotStatus.STOPPED))
        log(botId, LogLevel.SYSTEM, "Bot stopped")
        updateNotification()

        val anyRunning = _runtimeStates.value.values.any { it.status.isActive() }
        if (!anyRunning) {
            wakeLockManager.release()
            stopSelf()
        }
    }

    private suspend fun log(botId: String, level: LogLevel, message: String) {
        botRepository.addLog(LogEntry(botId = botId, level = level, message = message))
    }

    private fun updateNotification() {
        val runningCount = _runtimeStates.value.values.count { it.status.isActive() }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification("Running — $runningCount bot(s) active"))
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contrary Phone VPS")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannel(
            CHANNEL_ID,
            "Bot Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification for running bots"
            nm.createNotificationChannel(this)
        }
        NotificationChannel(
            CHANNEL_ID_BOTS,
            "Bot Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Bot crash and status alerts"
            nm.createNotificationChannel(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wakeLockManager.release()
        _isServiceRunning.value = false
        Timber.d("BotForegroundService destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart service when app is swiped away
        val restartIntent = Intent(applicationContext, BotForegroundService::class.java)
        restartIntent.setPackage(packageName)
        val pendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent)
        super.onTaskRemoved(rootIntent)
    }
}
