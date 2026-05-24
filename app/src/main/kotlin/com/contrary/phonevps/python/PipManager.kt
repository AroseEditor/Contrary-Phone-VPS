package com.contrary.phonevps.python

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class PipResult(
    val success: Boolean,
    val output: String,
    val packageName: String,
)

@Singleton
class PipManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pythonBridge: PythonBridge,
) {
    /** Install a single package — e.g. "discord.py" or "nextcord==2.6.1" */
    suspend fun installPackage(packageSpec: String): PipResult = withContext(Dispatchers.IO) {
        Timber.d("pip install $packageSpec")
        try {
            val pipModule = pythonBridge.getPipModule()
            val result = pipModule.callAttr("install_package", packageSpec)
            val success = result.asList()[0].toJava(Boolean::class.java)
            val output = result.asList()[1].toString()
            PipResult(success, output, packageSpec)
        } catch (e: Exception) {
            Timber.e(e, "pip install failed for $packageSpec")
            PipResult(false, e.message ?: "Unknown pip error", packageSpec)
        }
    }

    /** Install from requirements.txt path */
    suspend fun installRequirements(requirementsPath: String): PipResult = withContext(Dispatchers.IO) {
        Timber.d("pip install -r $requirementsPath")
        try {
            val pipModule = pythonBridge.getPipModule()
            val result = pipModule.callAttr("install_requirements", requirementsPath)
            val success = result.asList()[0].toJava(Boolean::class.java)
            val output = result.asList()[1].toString()
            PipResult(success, output, requirementsPath)
        } catch (e: Exception) {
            Timber.e(e, "pip install -r failed")
            PipResult(false, e.message ?: "Unknown error", requirementsPath)
        }
    }

    /** List installed packages */
    suspend fun listPackages(): List<String> = withContext(Dispatchers.IO) {
        try {
            val pipModule = pythonBridge.getPipModule()
            val result = pipModule.callAttr("list_packages")
            result.asList().map { it.toString() }
        } catch (e: Exception) {
            Timber.e(e, "pip list failed")
            emptyList()
        }
    }

    /** Uninstall a package */
    suspend fun uninstallPackage(packageName: String): PipResult = withContext(Dispatchers.IO) {
        try {
            val pipModule = pythonBridge.getPipModule()
            val result = pipModule.callAttr("uninstall_package", packageName)
            val success = result.asList()[0].toJava(Boolean::class.java)
            val output = result.asList()[1].toString()
            PipResult(success, output, packageName)
        } catch (e: Exception) {
            PipResult(false, e.message ?: "Unknown error", packageName)
        }
    }

    /** Show package info */
    suspend fun showPackage(packageName: String): String = withContext(Dispatchers.IO) {
        try {
            val pipModule = pythonBridge.getPipModule()
            pipModule.callAttr("show_package", packageName).toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
