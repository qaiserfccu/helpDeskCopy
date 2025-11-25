package com.helpdesk.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpdesk.app.data.repository.AuthRepository
import com.helpdesk.app.domain.model.RegisterCredentials
import com.helpdesk.app.domain.model.UserRole
import com.helpdesk.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole = UserRole.USER,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun updateRole(role: UserRole) {
        _uiState.update { it.copy(role = role, error = null) }
    }

    fun register() {
        val currentState = _uiState.value
        
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your name") }
            return
        }
        
        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email") }
            return
        }
        
        if (currentState.password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.register(
                RegisterCredentials(
                    name = currentState.name.trim(),
                    email = currentState.email.trim(),
                    password = currentState.password,
                    role = currentState.role
                )
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = result.exception.message ?: "Registration failed"
                        ) 
                    }
                }
                Result.Loading -> { /* Already handled */ }
            }
        }
    }
}
