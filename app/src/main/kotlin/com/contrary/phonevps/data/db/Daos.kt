package com.contrary.phonevps.data.db

import androidx.room.*
import com.contrary.phonevps.data.model.BotConfig
import com.contrary.phonevps.data.model.LogEntry
import com.contrary.phonevps.data.model.LogLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface BotDao {
    @Query("SELECT * FROM bots ORDER BY createdAt DESC")
    fun getAllBots(): Flow<List<BotConfig>>

    @Query("SELECT * FROM bots WHERE id = :id")
    suspend fun getBotById(id: String): BotConfig?

    @Query("SELECT * FROM bots WHERE autoStart = 1 AND isEnabled = 1")
    suspend fun getAutoStartBots(): List<BotConfig>

    @Upsert
    suspend fun upsertBot(bot: BotConfig)

    @Delete
    suspend fun deleteBot(bot: BotConfig)

    @Query("DELETE FROM bots WHERE id = :id")
    suspend fun deleteBotById(id: String)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs WHERE botId = :botId OR botId IS NULL ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsForBot(botId: String?, limit: Int = 500): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    fun getAllLogs(limit: Int = 1000): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE level IN (:levels) ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByLevel(levels: List<LogLevel>, limit: Int = 500): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE message LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    fun searchLogs(query: String, limit: Int = 200): Flow<List<LogEntry>>

    @Insert
    suspend fun insertLog(log: LogEntry): Long

    @Query("DELETE FROM logs WHERE botId = :botId")
    suspend fun clearLogsForBot(botId: String)

    @Query("DELETE FROM logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs ORDER BY timestamp ASC LIMIT :count)")
    suspend fun pruneOldestLogs(count: Int)

    @Query("SELECT COUNT(*) FROM logs")
    suspend fun logCount(): Int
}
