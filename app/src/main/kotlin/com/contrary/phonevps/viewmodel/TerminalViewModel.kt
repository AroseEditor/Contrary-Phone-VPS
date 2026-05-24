package com.contrary.phonevps.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contrary.phonevps.data.model.LogEntry
import com.contrary.phonevps.data.model.LogLevel
import com.contrary.phonevps.data.repository.BotRepository
import com.contrary.phonevps.python.PipManager
import com.contrary.phonevps.python.PythonBridge
import com.contrary.phonevps.service.BotForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/** Result of a terminal command execution */
data class TerminalLine(
    val text: String,
    val type: TerminalLineType,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class TerminalLineType {
    INPUT,      // user typed this
    OUTPUT,     // normal output
    ERROR,      // error/stderr
    SUCCESS,    // green success
    SYSTEM,     // blue system message
    PIP,        // pip output
    WARN,       // yellow warning
}

@HiltViewModel
class TerminalViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val botRepository: BotRepository,
    private val pipManager: PipManager,
    private val pythonBridge: PythonBridge,
) : ViewModel() {

    // Terminal line buffer — visible in console
    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    // Command history for up-arrow navigation
    private val _history = mutableListOf<String>()
    val history: List<String> get() = _history.toList()
    private var _historyIndex = -1

    // Current active bot context (null = global)
    private val _activeBotId = MutableStateFlow<String?>(null)
    val activeBotId: StateFlow<String?> = _activeBotId.asStateFlow()

    // Is a command currently executing?
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    init {
        emit("Contrary Phone VPS Terminal v1.0.0", TerminalLineType.SYSTEM)
        emit("Type 'help' for available commands.", TerminalLineType.SYSTEM)
        emit("", TerminalLineType.OUTPUT)

        // Stream live bot logs into terminal
        viewModelScope.launch {
            botRepository.getAllLogs(limit = 200)
                .distinctUntilChanged()
                .collect { logs ->
                    // Only stream the most recent log entry if it just arrived
                }
        }
    }

    /** Execute a user-typed command */
    fun executeCommand(rawInput: String, botId: String? = null) {
        val input = rawInput.trim()
        if (input.isBlank()) return

        // Add to history
        _history.add(0, input)
        if (_history.size > 100) _history.removeAt(_history.size - 1)
        _historyIndex = -1

        emit("$ $input", TerminalLineType.INPUT)

        viewModelScope.launch {
            _isExecuting.value = true
            try {
                dispatch(input, botId)
            } finally {
                _isExecuting.value = false
            }
        }
    }

    private suspend fun dispatch(input: String, botId: String?) {
        val parts = input.trim().split(Regex("\\s+"))
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        when (cmd) {
            // ─── PIP COMMANDS ─────────────────────────────────────
            "pip" -> handlePip(args)

            // ─── BOT COMMANDS ─────────────────────────────────────
            "start" -> handleStartBot(args, botId)
            "stop", "exit" -> handleStopBot(args, botId)
            "restart" -> handleRestartBot(args, botId)
            "status" -> handleStatus(botId)
            "bots" -> handleListBots()

            // ─── PYTHON EVAL ──────────────────────────────────────
            "python", "py" -> handlePythonExec(args.joinToString(" "))
            "eval" -> handlePythonEval(args.joinToString(" "))

            // ─── UTILITY ──────────────────────────────────────────
            "clear", "cls" -> clearTerminal()
            "help" -> showHelp()
            "version" -> emit("Contrary Phone VPS v1.0.0 | Python 3.11 | Chaquopy", TerminalLineType.SYSTEM)
            "echo" -> emit(args.joinToString(" "), TerminalLineType.OUTPUT)
            "history" -> showHistory()
            "logs" -> handleShowLogs(args, botId)
            "export" -> handleExportLogs(botId)

            // Unknown
            else -> emit("Unknown command: '$cmd'. Type 'help' for help.", TerminalLineType.ERROR)
        }
    }

    // ────────────────────────────────────────────────────────────────
    // PIP HANDLER
    // ────────────────────────────────────────────────────────────────

    private suspend fun handlePip(args: List<String>) {
        if (args.isEmpty()) {
            emit("Usage: pip <install|uninstall|list|show|freeze> [package]", TerminalLineType.WARN)
            return
        }
        when (args[0].lowercase()) {
            "install" -> {
                if (args.size < 2) { emit("Usage: pip install <package>", TerminalLineType.WARN); return }
                val pkg = args.drop(1).joinToString(" ")
                emit("Installing $pkg...", TerminalLineType.PIP)
                val result = pipManager.installPackage(pkg)
                if (result.success) {
                    emit("✓ Successfully installed $pkg", TerminalLineType.SUCCESS)
                } else {
                    emit("✗ Failed to install $pkg", TerminalLineType.ERROR)
                }
                if (result.output.isNotBlank()) {
                    result.output.lines().forEach { line ->
                        if (line.isNotBlank()) emit(line, TerminalLineType.PIP)
                    }
                }
            }
            "uninstall" -> {
                if (args.size < 2) { emit("Usage: pip uninstall <package>", TerminalLineType.WARN); return }
                val pkg = args[1]
                emit("Uninstalling $pkg...", TerminalLineType.PIP)
                val result = pipManager.uninstallPackage(pkg)
                if (result.success) {
                    emit("✓ Uninstalled $pkg", TerminalLineType.SUCCESS)
                } else {
                    emit("✗ Failed: ${result.output}", TerminalLineType.ERROR)
                }
            }
            "list", "freeze" -> {
                emit("Installed packages:", TerminalLineType.SYSTEM)
                val packages = pipManager.listPackages()
                if (packages.isEmpty()) {
                    emit("  (no packages installed)", TerminalLineType.OUTPUT)
                } else {
                    packages.forEach { emit("  $it", TerminalLineType.OUTPUT) }
                    emit("Total: ${packages.size} packages", TerminalLineType.SYSTEM)
                }
            }
            "show" -> {
                if (args.size < 2) { emit("Usage: pip show <package>", TerminalLineType.WARN); return }
                val info = pipManager.showPackage(args[1])
                info.lines().forEach { emit(it, TerminalLineType.OUTPUT) }
            }
            else -> emit("pip: unknown subcommand '${args[0]}'. Try: install, uninstall, list, show", TerminalLineType.ERROR)
        }
    }

    // ────────────────────────────────────────────────────────────────
    // BOT CONTROL HANDLERS
    // ────────────────────────────────────────────────────────────────

    private fun handleStartBot(args: List<String>, currentBotId: String?) {
        val botId = args.firstOrNull() ?: currentBotId
        if (botId == null) {
            emit("Usage: start <bot-id>  or select a bot first", TerminalLineType.WARN)
            return
        }
        val intent = Intent(context, BotForegroundService::class.java).apply {
            action = BotForegroundService.ACTION_START_BOT
            putExtra(BotForegroundService.EXTRA_BOT_ID, botId)
        }
        context.startForegroundService(intent)
        emit("Starting bot $botId...", TerminalLineType.SYSTEM)
    }

    /**
     * 'exit' or 'stop' — stops the bot but keeps the terminal open.
     * Does NOT quit the app.
     */
    private fun handleStopBot(args: List<String>, currentBotId: String?) {
        val botId = args.firstOrNull() ?: currentBotId
        if (botId == null) {
            emit("No active bot. Usage: stop <bot-id>", TerminalLineType.WARN)
            emit("Terminal remains open. Type 'help' for commands.", TerminalLineType.SYSTEM)
            return
        }
        val intent = Intent(context, BotForegroundService::class.java).apply {
            action = BotForegroundService.ACTION_STOP_BOT
            putExtra(BotForegroundService.EXTRA_BOT_ID, botId)
        }
        context.startForegroundService(intent)
        emit("Bot $botId stopped. Terminal is still active.", TerminalLineType.SUCCESS)
        emit("Type 'start $botId' to restart, or 'help' for options.", TerminalLineType.SYSTEM)
    }

    private fun handleRestartBot(args: List<String>, currentBotId: String?) {
        val botId = args.firstOrNull() ?: currentBotId
        if (botId == null) { emit("Usage: restart <bot-id>", TerminalLineType.WARN); return }
        val intent = Intent(context, BotForegroundService::class.java).apply {
            action = BotForegroundService.ACTION_RESTART_BOT
            putExtra(BotForegroundService.EXTRA_BOT_ID, botId)
        }
        context.startForegroundService(intent)
        emit("Restarting bot $botId...", TerminalLineType.SYSTEM)
    }

    private suspend fun handleStatus(botId: String?) {
        val states = BotForegroundService.runtimeStates.value
        if (states.isEmpty()) {
            emit("No bots are running.", TerminalLineType.OUTPUT)
            return
        }
        states.entries.forEach { (id, state) ->
            val target = if (botId != null && id != botId) return@forEach
            emit("Bot: $id", TerminalLineType.SYSTEM)
            emit("  Status: ${state.status.displayName()}", TerminalLineType.OUTPUT)
            emit("  Uptime: ${formatUptime(state.uptimeSeconds)}", TerminalLineType.OUTPUT)
            emit("  Restarts: ${state.restartCount}", TerminalLineType.OUTPUT)
            if (state.lastError != null) emit("  Last error: ${state.lastError}", TerminalLineType.ERROR)
        }
    }

    private suspend fun handleListBots() {
        val bots = botRepository.allBots.firstOrNull() ?: emptyList()
        if (bots.isEmpty()) {
            emit("No bots configured. Create one in the sidebar.", TerminalLineType.OUTPUT)
            return
        }
        emit("Configured bots:", TerminalLineType.SYSTEM)
        bots.forEach { bot ->
            val state = BotForegroundService.runtimeStates.value[bot.id]
            val status = state?.status?.displayName() ?: "Stopped"
            emit("  [${bot.id}] ${bot.name} — $status", TerminalLineType.OUTPUT)
        }
    }

    // ────────────────────────────────────────────────────────────────
    // PYTHON EVAL
    // ────────────────────────────────────────────────────────────────

    private suspend fun handlePythonExec(code: String) {
        if (code.isBlank()) { emit("Usage: python <code>  e.g. python print('hello')", TerminalLineType.WARN); return }
        emit(">>> $code", TerminalLineType.SYSTEM)
        val result = withContext(Dispatchers.IO) { pythonBridge.exec(code) }
        if (result.stdout.isNotBlank()) result.stdout.lines().forEach { emit(it, TerminalLineType.OUTPUT) }
        if (result.stderr.isNotBlank()) result.stderr.lines().forEach { emit(it, TerminalLineType.ERROR) }
        if (result.exitCode != 0) emit("Exit code: ${result.exitCode}", TerminalLineType.WARN)
    }

    private suspend fun handlePythonEval(expr: String) {
        if (expr.isBlank()) { emit("Usage: eval <expression>", TerminalLineType.WARN); return }
        val result = withContext(Dispatchers.IO) { pythonBridge.eval(expr) }
        emit(result, TerminalLineType.OUTPUT)
    }

    // ────────────────────────────────────────────────────────────────
    // LOGS
    // ────────────────────────────────────────────────────────────────

    private suspend fun handleShowLogs(args: List<String>, botId: String?) {
        val limit = args.firstOrNull()?.toIntOrNull() ?: 50
        val logs = if (botId != null) {
            botRepository.getLogsForBot(botId, limit).firstOrNull()
        } else {
            botRepository.getAllLogs(limit).firstOrNull()
        } ?: emptyList()

        logs.reversed().forEach { log ->
            val type = when (log.level) {
                LogLevel.ERROR, LogLevel.STDERR -> TerminalLineType.ERROR
                LogLevel.WARN -> TerminalLineType.WARN
                LogLevel.SYSTEM -> TerminalLineType.SYSTEM
                LogLevel.PIP -> TerminalLineType.PIP
                else -> TerminalLineType.OUTPUT
            }
            emit("[${log.formattedTimestamp()}] ${log.message}", type)
        }
        emit("— ${logs.size} log entries shown —", TerminalLineType.SYSTEM)
    }

    private suspend fun handleExportLogs(botId: String?) {
        val logs = botRepository.getAllLogs(5000).firstOrNull() ?: emptyList()
        val text = logs.reversed().joinToString("\n") { "[${it.formattedTimestamp()}] [${it.level}] ${it.message}" }
        val file = java.io.File(context.getExternalFilesDir(null), "contrary_vps_logs_${System.currentTimeMillis()}.txt")
        withContext(Dispatchers.IO) { file.writeText(text) }
        emit("✓ Logs exported to: ${file.absolutePath}", TerminalLineType.SUCCESS)
    }

    // ────────────────────────────────────────────────────────────────
    // UTILITY
    // ────────────────────────────────────────────────────────────────

    private fun clearTerminal() {
        _lines.value = emptyList()
        emit("Terminal cleared.", TerminalLineType.SYSTEM)
    }

    private fun showHistory() {
        if (_history.isEmpty()) { emit("No command history.", TerminalLineType.OUTPUT); return }
        _history.reversed().take(50).forEachIndexed { i, cmd ->
            emit("  ${i + 1}  $cmd", TerminalLineType.OUTPUT)
        }
    }

    private fun showHelp() {
        val help = """
Contrary Phone VPS — Terminal Commands
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

BOT CONTROL
  start [bot-id]      Start a bot
  stop  [bot-id]      Stop bot (terminal stays open!)
  exit  [bot-id]      Alias for stop
  restart [bot-id]    Restart a bot
  status [bot-id]     Show bot status & uptime
  bots                List all configured bots

PIP PACKAGE MANAGER
  pip install <pkg>   Install a Python package
  pip uninstall <pkg> Uninstall a package
  pip list            List installed packages
  pip show <pkg>      Show package info
  pip freeze          Same as pip list

PYTHON
  python <code>       Execute Python code
  eval <expr>         Evaluate a Python expression

LOGS
  logs [n]            Show last n log lines (default 50)
  export              Export all logs to file

TERMINAL
  clear / cls         Clear the terminal
  history             Show command history
  echo <text>         Print text
  version             Show app version
  help                Show this help

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIP: 'exit' stops the bot but NOT the terminal.
        """.trimIndent()
        help.lines().forEach { emit(it, TerminalLineType.SYSTEM) }
    }

    /** Add log entries from the running bots into the terminal display */
    fun ingestLogEntry(log: LogEntry) {
        val type = when (log.level) {
            LogLevel.ERROR, LogLevel.STDERR -> TerminalLineType.ERROR
            LogLevel.WARN -> TerminalLineType.WARN
            LogLevel.SYSTEM -> TerminalLineType.SYSTEM
            LogLevel.PIP -> TerminalLineType.PIP
            LogLevel.STDOUT -> TerminalLineType.OUTPUT
            else -> TerminalLineType.OUTPUT
        }
        emit("[${log.formattedTimestamp()}] ${log.message}", type)
    }

    fun setActiveBotId(botId: String?) {
        _activeBotId.value = botId
    }

    /** Navigate command history with arrow keys */
    fun historyUp(current: String): String {
        if (_history.isEmpty()) return current
        _historyIndex = (_historyIndex + 1).coerceAtMost(_history.size - 1)
        return _history[_historyIndex]
    }

    fun historyDown(): String {
        if (_historyIndex <= 0) { _historyIndex = -1; return "" }
        _historyIndex--
        return _history[_historyIndex]
    }

    private fun emit(text: String, type: TerminalLineType) {
        val line = TerminalLine(text, type)
        _lines.update { current ->
            val updated = current + line
            // Keep max 2000 lines in memory
            if (updated.size > 2000) updated.takeLast(2000) else updated
        }
    }

    private fun formatUptime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
