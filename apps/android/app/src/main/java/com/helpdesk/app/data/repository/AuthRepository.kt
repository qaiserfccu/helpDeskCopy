package com.helpdesk.app.data.repository

import com.helpdesk.app.data.api.AuthApi
import com.helpdesk.app.data.local.SecureStorage
import com.helpdesk.app.data.model.LoginRequest
import com.helpdesk.app.data.model.RefreshRequest
import com.helpdesk.app.data.model.RegisterRequest
import com.helpdesk.app.domain.model.AuthSession
import com.helpdesk.app.domain.model.LoginCredentials
import com.helpdesk.app.domain.model.RegisterCredentials
import com.helpdesk.app.domain.model.User
import com.helpdesk.app.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage
) {
    private val _sessionState = MutableStateFlow<AuthSession?>(null)
    val sessionState: StateFlow<AuthSession?> = _sessionState.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isOfflineSession = MutableStateFlow(false)
    val isOfflineSession: StateFlow<Boolean> = _isOfflineSession.asStateFlow()

    val currentSession: AuthSession?
        get() = _sessionState.value

    val currentUser: User?
        get() = _sessionState.value?.user

    val accessToken: String?
        get() = _sessionState.value?.accessToken

    suspend fun initialize() {
        val session = secureStorage.getSession()
        _sessionState.value = session
        _isInitialized.value = true
    }

    suspend fun login(credentials: LoginCredentials): Result<AuthSession> {
        return try {
            val response = authApi.login(
                LoginRequest(
                    email = credentials.email.trim().lowercase(),
                    password = credentials.password
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val session = AuthSession(
                    user = authResponse.user.toDomain(),
                    accessToken = authResponse.tokens.accessToken,
                    refreshToken = authResponse.tokens.refreshToken
                )
                applySession(session)
                cacheLoginCredentials(credentials)
                Result.Success(session)
            } else {
                Result.Error(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun register(credentials: RegisterCredentials): Result<AuthSession> {
        return try {
            val response = authApi.register(
                RegisterRequest(
                    name = credentials.name.trim(),
                    email = credentials.email.trim().lowercase(),
                    password = credentials.password,
                    role = credentials.role.toApiValue()
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val session = AuthSession(
                    user = authResponse.user.toDomain(),
                    accessToken = authResponse.tokens.accessToken,
                    refreshToken = authResponse.tokens.refreshToken
                )
                applySession(session)
                cacheLoginCredentials(LoginCredentials(credentials.email, credentials.password))
                Result.Success(session)
            } else {
                val errorMessage = if (response.code() == 409) {
                    "An account with that email already exists."
                } else {
                    "Registration failed: ${response.message()}"
                }
                Result.Error(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun refreshSession(): Result<AuthSession> {
        val currentRefreshToken = _sessionState.value?.refreshToken
            ?: return Result.Error(Exception("No refresh token available"))

        return try {
            val response = authApi.refresh(RefreshRequest(currentRefreshToken))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val session = AuthSession(
                    user = authResponse.user.toDomain(),
                    accessToken = authResponse.tokens.accessToken,
                    refreshToken = authResponse.tokens.refreshToken
                )
                applySession(session)
                Result.Success(session)
            } else {
                Result.Error(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun signOut(forgetOfflineSnapshot: Boolean = false) {
        secureStorage.clearSession()
        if (forgetOfflineSnapshot) {
            secureStorage.clearOfflineSession()
        }
        _sessionState.value = null
        _isOfflineSession.value = false
    }

    suspend fun resumeOfflineSession(credentials: LoginCredentials): Boolean {
        val cachedEmail = secureStorage.getCachedEmail()
        val cachedPasswordHash = secureStorage.getCachedPasswordHash()

        if (cachedEmail == null || cachedPasswordHash == null) {
            return false
        }

        val normalizedEmail = credentials.email.trim().lowercase()
        if (cachedEmail != normalizedEmail) {
            return false
        }

        val inputHash = hashPassword(credentials.password)
        if (inputHash != cachedPasswordHash) {
            return false
        }

        val offlineSession = secureStorage.getOfflineSession()
        if (offlineSession != null) {
            _sessionState.value = offlineSession
            _isOfflineSession.value = true
            secureStorage.saveSession(offlineSession)
            return true
        }

        return false
    }

    private suspend fun applySession(session: AuthSession, isOffline: Boolean = false) {
        secureStorage.saveSession(session)
        if (!isOffline) {
            secureStorage.saveOfflineSession(session)
        }
        _sessionState.value = session
        _isOfflineSession.value = isOffline
    }

    private suspend fun cacheLoginCredentials(credentials: LoginCredentials) {
        val normalizedEmail = credentials.email.trim().lowercase()
        val passwordHash = hashPassword(credentials.password)
        secureStorage.cacheLoginCredentials(normalizedEmail, passwordHash)
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
