package com.contrary.phonevps.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contrary.phonevps.data.model.BotConfig
import com.contrary.phonevps.data.model.BotRuntimeState
import com.contrary.phonevps.data.model.BotStatus
import com.contrary.phonevps.data.model.LogEntry
import com.contrary.phonevps.data.model.LogLevel
import com.contrary.phonevps.data.repository.BotRepository
import com.contrary.phonevps.python.ScriptValidator
import com.contrary.phonevps.service.BotForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BotUiState(
    val bots: List<BotConfig> = emptyList(),
    val runtimeStates: Map<String, BotRuntimeState> = emptyMap(),
    val selectedBotId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BotViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val botRepository: BotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotUiState())
    val uiState: StateFlow<BotUiState> = _uiState.asStateFlow()

    val selectedBot: StateFlow<BotConfig?> = _uiState
        .map { state -> state.bots.find { it.id == state.selectedBotId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Observe bots from DB
        viewModelScope.launch {
            botRepository.allBots.collect { bots ->
                _uiState.update { it.copy(bots = bots) }
            }
        }
        // Observe service runtime states
        viewModelScope.launch {
            BotForegroundService.runtimeStates.collect { states ->
                _uiState.update { it.copy(runtimeStates = states) }
            }
        }
    }

    fun selectBot(botId: String?) {
        _uiState.update { it.copy(selectedBotId = botId) }
    }

    suspend fun createBot(
        id: String,
        name: String,
        description: String,
        token: String,
        scriptContent: String,
        scriptPath: String,
    ) {
        val bot = BotConfig(
            id = id,
            name = name.ifBlank { "Bot-${id.take(6)}" },
            description = description,
            tokenKeyAlias = id,
            scriptPath = scriptPath,
            scriptContent = scriptContent,
            updatedAt = System.currentTimeMillis(),
        )
        if (token.isNotBlank()) botRepository.storeToken(id, token)
        botRepository.saveBot(bot)
    }

    suspend fun updateBot(bot: BotConfig, newToken: String? = null) {
        if (newToken != null && newToken.isNotBlank()) {
            botRepository.storeToken(bot.id, newToken)
        }
        botRepository.saveBot(bot.copy(updatedAt = System.currentTimeMillis()))
    }

    fun deleteBot(botId: String) {
        viewModelScope.launch {
            stopBot(botId)
            botRepository.deleteBotById(botId)
            if (_uiState.value.selectedBotId == botId) {
                _uiState.update { it.copy(selectedBotId = null) }
            }
        }
    }

    fun startBot(botId: String) {
        viewModelScope.launch {
            // Validate script first by loading freshest config directly from DB
            val bot = botRepository.getBotById(botId) ?: run {
                _uiState.update { it.copy(error = "Bot configuration not found. Please save first.") }
                return@launch
            }
            if (bot.scriptContent.isNotBlank()) {
                val validation = ScriptValidator.validate(bot.scriptContent)
                if (!validation.isValid) {
                    _uiState.update { it.copy(error = validation.errors.joinToString("\n")) }
                    return@launch
                }
            }
            val intent = Intent(context, BotForegroundService::class.java).apply {
                action = BotForegroundService.ACTION_START_BOT
                putExtra(BotForegroundService.EXTRA_BOT_ID, botId)
            }
            context.startForegroundService(intent)
        }
    }

    fun stopBot(botId: String) {
        val intent = Intent(context, BotForegroundService::class.java).apply {
            action = BotForegroundService.ACTION_STOP_BOT
            putExtra(BotForegroundService.EXTRA_BOT_ID, botId)
        }
        context.startForegroundService(intent)
    }

    fun restartBot(botId: String) {
        val intent = Intent(context, BotForegroundService::class.java).apply {
            action = BotForegroundService.ACTION_RESTART_BOT
            putExtra(BotForegroundService.EXTRA_BOT_ID, botId)
        }
        context.startForegroundService(intent)
    }

    fun getToken(botId: String): String? = botRepository.getToken(botId)

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun getRuntimeState(botId: String): BotRuntimeState =
        _uiState.value.runtimeStates[botId] ?: BotRuntimeState(botId)

    fun getLogsForBot(botId: String): Flow<List<LogEntry>> =
        botRepository.getLogsForBot(botId)

    suspend fun getBotById(botId: String): BotConfig? = botRepository.getBotById(botId)
}
