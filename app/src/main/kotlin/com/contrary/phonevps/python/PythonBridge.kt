package com.contrary.phonevps.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    fun getPython(): Python = Python.getInstance()

    /** Execute a Python expression and return its string result */
    fun eval(expression: String): String {
        return try {
            val builtins = getPython().getBuiltins()
            builtins.callAttr("eval", expression).toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /** Execute a Python statement block, capturing stdout/stderr */
    fun exec(code: String, globals: Map<String, Any> = emptyMap()): PythonExecResult {
        return try {
            val module = getPython().getModule("stdio_capture")
            val result = module.callAttr("exec_capture", code)
            val stdout = result.asList()[0].toString()
            val stderr = result.asList()[1].toString()
            val exitCode = result.asList()[2].toJava(Int::class.java)
            PythonExecResult(stdout, stderr, exitCode)
        } catch (e: Exception) {
            Timber.e(e, "Python exec error")
            PythonExecResult("", e.message ?: "Unknown error", 1)
        }
    }

    /** Get the module for bot running */
    fun getBotRunnerModule() = getPython().getModule("bot_runner")

    /** Get pip helper module */
    fun getPipModule() = getPython().getModule("pip_helper")
}

data class PythonExecResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)
