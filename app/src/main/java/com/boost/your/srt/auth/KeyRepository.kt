package com.boost.your.srt.auth

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

class KeyRepository(context: Context) {

    private val appContext = context.applicationContext
    private val backend = "https://fpsapp.onrender.com"
    private val client = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)   // Render free tier can take ~30s to wake
        .build()

    private val encPrefs by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            "boost_master_secure",
            MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getHwid(context: Context = appContext): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"

    /** Always asks the server. Never returns Success unless the backend says valid == true. */
    suspend fun verifyKey(key: String, hwid: String): KeyVerifyResult = withContext(Dispatchers.IO) {
        try {
            val url = "$backend/api/verify-key".toHttpUrl().newBuilder()
                .addQueryParameter("key", key.trim())
                .addQueryParameter("hwid", hwid)
                .build()
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                val body = response.body?.string()
                if (body.isNullOrBlank()) throw IOException("Empty response (HTTP ${response.code})")
                val json = JSONObject(body)
                if (json.optBoolean("valid", false)) {
                    KeyVerifyResult.Success(
                        key = json.optString("key", key.trim()),
                        expiresAt = json.optString("expiresAt", ""),
                        daysLeft = json.optInt("daysLeft", 0),
                        type = json.optString("type", "standard")
                    )
                } else {
                    KeyVerifyResult.Failure(json.optString("message", "Invalid key"))
                }
            }
        } catch (e: Exception) {
            KeyVerifyResult.NetworkError(e.message ?: "Connection failed")
        }
    }

    fun saveKey(key: String, expiresAt: String, hwid: String = getHwid()) {
        runCatching {
            encPrefs.edit()
                .putString(KEY_LICENSE, key)
                .putString(KEY_EXPIRES_AT, expiresAt)
                .putString(KEY_HWID, hwid)
                .apply()
        }
    }

    fun getSavedKey(): String? = runCatching { encPrefs.getString(KEY_LICENSE, null) }.getOrNull()
    fun getSavedExpiry(): String? = runCatching { encPrefs.getString(KEY_EXPIRES_AT, null) }.getOrNull()
    fun getSavedHwid(): String? = runCatching { encPrefs.getString(KEY_HWID, null) }.getOrNull()

    /** True when a key is stored, was saved for this device, and has not passed its expiry. */
    fun isKeyLocallyValid(): Boolean {
        val key = getSavedKey() ?: return false
        if (key.isBlank()) return false
        val savedHwid = getSavedHwid()
        if (savedHwid != null && savedHwid != getHwid()) return false
        val expiry = getSavedExpiry()
        if (expiry.isNullOrBlank()) return true
        return try {
            Instant.parse(expiry).isAfter(Instant.now())
        } catch (e: Exception) {
            true
        }
    }

    fun clearKey() {
        runCatching { encPrefs.edit().clear().apply() }
    }

    private companion object {
        const val KEY_LICENSE = "KEY_LICENSE"
        const val KEY_EXPIRES_AT = "KEY_EXPIRES_AT"
        const val KEY_HWID = "KEY_HWID"
    }
}

sealed class KeyVerifyResult {
    data class Success(val key: String, val expiresAt: String, val daysLeft: Int, val type: String) : KeyVerifyResult()
    data class Failure(val message: String) : KeyVerifyResult()
    data class NetworkError(val message: String) : KeyVerifyResult()
}
