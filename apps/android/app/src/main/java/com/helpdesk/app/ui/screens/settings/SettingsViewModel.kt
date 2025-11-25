package com.helpdesk.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpdesk.app.data.local.SecureStorage
import com.helpdesk.app.data.repository.AuthRepository
import com.helpdesk.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hasOfflineSession: Boolean = false,
    val isOffline: Boolean = false,
    val pendingAuthActions: Int = 0,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val secureStorage: SecureStorage,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                hasOfflineSession = authRepository.isOfflineSession.value,
                isOffline = !networkMonitor.isOnline
            )
        }

        viewModelScope.launch {
            authRepository.isOfflineSession.collect { isOffline ->
                _uiState.update { it.copy(hasOfflineSession = isOffline) }
            }
        }

        viewModelScope.launch {
            networkMonitor.observeConnectivity().collect { isConnected ->
                _uiState.update { it.copy(isOffline = !isConnected) }
            }
        }
    }

    fun clearOfflineData() {
        viewModelScope.launch {
            secureStorage.clearOfflineSession()
            _uiState.update {
                it.copy(
                    message = "Offline access cleared. You'll need to sign in online again before biometric unlock is available offline."
                )
            }
        }
    }
}
