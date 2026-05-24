package com.contrary.phonevps.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenCryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "contrary_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Store a token for botId — never stored plaintext */
    fun storeToken(botId: String, token: String) {
        encryptedPrefs.edit().putString(tokenKey(botId), token).apply()
    }

    /** Retrieve decrypted token for botId */
    fun retrieveToken(botId: String): String? {
        return encryptedPrefs.getString(tokenKey(botId), null)
    }

    /** Delete token when bot is deleted */
    fun deleteToken(botId: String) {
        encryptedPrefs.edit().remove(tokenKey(botId)).apply()
    }

    /** Check if a token exists for this bot */
    fun hasToken(botId: String): Boolean {
        return encryptedPrefs.contains(tokenKey(botId))
    }

    private fun tokenKey(botId: String) = "token_$botId"
}
