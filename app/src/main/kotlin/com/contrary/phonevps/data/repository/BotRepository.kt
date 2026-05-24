package com.contrary.phonevps.data.repository

import com.contrary.phonevps.data.db.BotDao
import com.contrary.phonevps.data.db.LogDao
import com.contrary.phonevps.data.model.BotConfig
import com.contrary.phonevps.data.model.LogEntry
import com.contrary.phonevps.data.model.LogLevel
import com.contrary.phonevps.data.security.TokenCryptoManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BotRepository @Inject constructor(
    private val botDao: BotDao,
    private val logDao: LogDao,
    private val tokenCryptoManager: TokenCryptoManager,
) {
    val allBots: Flow<List<BotConfig>> = botDao.getAllBots()

    suspend fun getBotById(id: String): BotConfig? = botDao.getBotById(id)

    suspend fun getAutoStartBots(): List<BotConfig> = botDao.getAutoStartBots()

    suspend fun saveBot(bot: BotConfig) = botDao.upsertBot(bot)

    suspend fun deleteBot(bot: BotConfig) {
        tokenCryptoManager.deleteToken(bot.id)
        botDao.deleteBot(bot)
    }

    suspend fun deleteBotById(id: String) {
        tokenCryptoManager.deleteToken(id)
        botDao.deleteBotById(id)
    }

    fun storeToken(botId: String, token: String) = tokenCryptoManager.storeToken(botId, token)

    fun getToken(botId: String): String? = tokenCryptoManager.retrieveToken(botId)

    fun hasToken(botId: String): Boolean = tokenCryptoManager.hasToken(botId)

    // Logging
    fun getLogsForBot(botId: String?, limit: Int = 500): Flow<List<LogEntry>> =
        logDao.getLogsForBot(botId, limit)

    fun getAllLogs(limit: Int = 1000): Flow<List<LogEntry>> = logDao.getAllLogs(limit)

    fun searchLogs(query: String): Flow<List<LogEntry>> = logDao.searchLogs(query)

    fun getErrorLogs(): Flow<List<LogEntry>> =
        logDao.getLogsByLevel(listOf(LogLevel.ERROR, LogLevel.STDERR, LogLevel.WARN))

    suspend fun addLog(log: LogEntry): Long = logDao.insertLog(log)

    suspend fun clearLogsForBot(botId: String) = logDao.clearLogsForBot(botId)

    suspend fun clearAllLogs() = logDao.clearAllLogs()

    suspend fun pruneLogsIfNeeded() {
        val count = logDao.logCount()
        if (count > 5000) {
            logDao.pruneOldestLogs(count - 3000)
        }
    }
}
