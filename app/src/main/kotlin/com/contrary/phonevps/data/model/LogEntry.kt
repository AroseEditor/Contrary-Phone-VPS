package com.contrary.phonevps.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, SYSTEM, STDOUT, STDERR, PIP, COMMAND
}

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val botId: String?,           // null = global/system log
    val level: LogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String = "",
) {
    fun formattedTimestamp(): String {
        val ms = timestamp
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ms))
    }
}

// Terminal command result — shown inline in console
data class TerminalCommandResult(
    val command: String,
    val output: String,
    val exitCode: Int,
    val timestamp: Long = System.currentTimeMillis(),
)
