package com.boost.your.srt.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class LoginUiState {
    object Idle : LoginUiState()
    object CheckingSaved : LoginUiState()
    object Verifying : LoginUiState()
    data class Success(val key: String, val daysLeft: Int, val type: String) : LoginUiState()
    data class Error(val message: String, val isNetwork: Boolean = false) : LoginUiState()
}

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = KeyRepository(app)
    val uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val keyInput = MutableStateFlow("")
    val hwid: String = repo.getHwid(app)

    init { checkSavedKey() }

    private fun checkSavedKey() {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                if (repo.isKeyLocallyValid()) repo.getSavedKey() else null
            } ?: return@launch
            uiState.value = LoginUiState.CheckingSaved
            when (val result = repo.verifyKey(saved, hwid)) {
                is KeyVerifyResult.Success -> {
                    repo.saveKey(result.key, result.expiresAt, hwid)
                    uiState.value = LoginUiState.Success(result.key, result.daysLeft, result.type)
                }
                is KeyVerifyResult.Failure -> {
                    repo.clearKey()
                    uiState.value = LoginUiState.Idle
                }
                is KeyVerifyResult.NetworkError -> {
                    // Server unreachable: allow offline entry only because the key is stored,
                    // bound to this device and not locally expired.
                    uiState.value = LoginUiState.Success(saved, -1, "offline")
                }
            }
        }
    }

    /** Auto-uppercases and strips anything that cannot be part of a key. */
    fun onKeyChange(raw: String) {
        keyInput.value = raw.uppercase().filter { it.isLetterOrDigit() || it == '_' }.take(20)
        clearError()
    }

    fun login() {
        val key = keyInput.value.trim().uppercase()
        if (key.isBlank()) {
            uiState.value = LoginUiState.Error("Enter your license key")
            return
        }
        if (!KEY_REGEX.matches(key)) {
            uiState.value = LoginUiState.Error("Invalid key format. Expected: SRT_XXXXXXXX")
            return
        }
        if (uiState.value is LoginUiState.Verifying) return
        viewModelScope.launch {
            uiState.value = LoginUiState.Verifying
            when (val result = repo.verifyKey(key, hwid)) {
                is KeyVerifyResult.Success -> {
                    withContext(Dispatchers.IO) { repo.saveKey(result.key, result.expiresAt, hwid) }
                    uiState.value = LoginUiState.Success(result.key, result.daysLeft, result.type)
                }
                is KeyVerifyResult.Failure -> uiState.value = LoginUiState.Error(result.message)
                is KeyVerifyResult.NetworkError -> uiState.value = LoginUiState.Error(
                    "Server waking up... (~30s on free tier)\n${result.message}", isNetwork = true
                )
            }
        }
    }

    fun clearError() {
        if (uiState.value is LoginUiState.Error) uiState.value = LoginUiState.Idle
    }

    companion object {
        val KEY_REGEX = Regex("^(SRT|SAMUEL|NK|KALEYYY)_[A-Z0-9]{8}$")
        val PREFIXES = listOf("SRT", "SAMUEL", "NK", "KALEYYY")
    }
}
