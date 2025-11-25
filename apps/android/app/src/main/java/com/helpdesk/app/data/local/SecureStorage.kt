package com.helpdesk.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.helpdesk.app.domain.model.AuthSession
import com.helpdesk.app.domain.model.User
import com.helpdesk.app.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun saveSession(session: AuthSession) = withContext(Dispatchers.IO) {
        val sessionJson = gson.toJson(SessionData.fromDomain(session))
        securePrefs.edit()
            .putString(KEY_SESSION, sessionJson)
            .apply()
    }

    suspend fun getSession(): AuthSession? = withContext(Dispatchers.IO) {
        val sessionJson = securePrefs.getString(KEY_SESSION, null)
        sessionJson?.let {
            try {
                gson.fromJson(it, SessionData::class.java).toDomain()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        securePrefs.edit()
            .remove(KEY_SESSION)
            .apply()
    }

    suspend fun saveOfflineSession(session: AuthSession) = withContext(Dispatchers.IO) {
        val sessionJson = gson.toJson(SessionData.fromDomain(session))
        securePrefs.edit()
            .putString(KEY_OFFLINE_SESSION, sessionJson)
            .apply()
    }

    suspend fun getOfflineSession(): AuthSession? = withContext(Dispatchers.IO) {
        val sessionJson = securePrefs.getString(KEY_OFFLINE_SESSION, null)
        sessionJson?.let {
            try {
                gson.fromJson(it, SessionData::class.java).toDomain()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun clearOfflineSession() = withContext(Dispatchers.IO) {
        securePrefs.edit()
            .remove(KEY_OFFLINE_SESSION)
            .apply()
    }

    suspend fun cacheLoginCredentials(email: String, passwordHash: String) = withContext(Dispatchers.IO) {
        securePrefs.edit()
            .putString(KEY_CACHED_EMAIL, email)
            .putString(KEY_CACHED_PASSWORD_HASH, passwordHash)
            .apply()
    }

    suspend fun getCachedEmail(): String? = withContext(Dispatchers.IO) {
        securePrefs.getString(KEY_CACHED_EMAIL, null)
    }

    suspend fun getCachedPasswordHash(): String? = withContext(Dispatchers.IO) {
        securePrefs.getString(KEY_CACHED_PASSWORD_HASH, null)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        securePrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "helpdesk_secure_prefs"
        private const val KEY_SESSION = "session"
        private const val KEY_OFFLINE_SESSION = "offline_session"
        private const val KEY_CACHED_EMAIL = "cached_email"
        private const val KEY_CACHED_PASSWORD_HASH = "cached_password_hash"
    }
}

// Internal data class for JSON serialization
private data class SessionData(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userRole: String,
    val accessToken: String,
    val refreshToken: String
) {
    fun toDomain(): AuthSession = AuthSession(
        user = User(
            id = userId,
            name = userName,
            email = userEmail,
            role = UserRole.fromString(userRole)
        ),
        accessToken = accessToken,
        refreshToken = refreshToken
    )

    companion object {
        fun fromDomain(session: AuthSession): SessionData = SessionData(
            userId = session.user.id,
            userName = session.user.name,
            userEmail = session.user.email,
            userRole = session.user.role.toApiValue(),
            accessToken = session.accessToken,
            refreshToken = session.refreshToken
        )
    }
}
