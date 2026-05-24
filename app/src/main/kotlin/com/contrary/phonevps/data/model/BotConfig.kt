package com.contrary.phonevps.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "bots")
data class BotConfig(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    // Token stored as encrypted reference key (never plaintext)
    val tokenKeyAlias: String = "",
    val scriptPath: String = "",
    val scriptContent: String = "",
    val requirementsPath: String = "",
    val autoStart: Boolean = false,
    val autoRestart: Boolean = true,
    val maxRestarts: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val groupName: String = "Default",
    val isEnabled: Boolean = true,
)

enum class BotStatus {
    STOPPED,
    STARTING,
    RUNNING,
    CRASHING,
    INSTALLING_DEPS,
    ERROR;

    fun displayName(): String = when (this) {
        STOPPED -> "Stopped"
        STARTING -> "Starting..."
        RUNNING -> "Running"
        CRASHING -> "Crashing"
        INSTALLING_DEPS -> "Installing deps..."
        ERROR -> "Error"
    }

    fun isActive(): Boolean = this == RUNNING || this == STARTING || this == INSTALLING_DEPS
}

data class BotRuntimeState(
    val botId: String,
    val status: BotStatus = BotStatus.STOPPED,
    val ramUsageMb: Float = 0f,
    val cpuPercent: Float = 0f,
    val uptimeSeconds: Long = 0L,
    val restartCount: Int = 0,
    val lastError: String? = null,
)
