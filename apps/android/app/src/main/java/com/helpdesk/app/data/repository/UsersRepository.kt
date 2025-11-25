package com.helpdesk.app.data.repository

import com.helpdesk.app.data.api.UsersApi
import com.helpdesk.app.data.model.CreateUserRequest
import com.helpdesk.app.data.model.UpdateUserRequest
import com.helpdesk.app.domain.model.User
import com.helpdesk.app.domain.model.UserRole
import com.helpdesk.app.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepository @Inject constructor(
    private val usersApi: UsersApi
) {
    suspend fun getUsers(role: UserRole? = null): Result<List<User>> {
        return try {
            val response = usersApi.getUsers(role?.toApiValue())
            if (response.isSuccessful && response.body() != null) {
                val users = response.body()!!.users.map { it.toDomain() }
                Result.Success(users)
            } else {
                Result.Error(Exception("Failed to fetch users: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createUser(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<User> {
        return try {
            val response = usersApi.createUser(
                CreateUserRequest(
                    name = name.trim(),
                    email = email.trim().lowercase(),
                    password = password,
                    role = role.toApiValue()
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user.toDomain())
            } else {
                Result.Error(Exception("Failed to create user: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateUser(
        userId: String,
        name: String? = null,
        email: String? = null,
        password: String? = null,
        role: UserRole? = null
    ): Result<User> {
        return try {
            val response = usersApi.updateUser(
                userId = userId,
                request = UpdateUserRequest(
                    name = name?.trim(),
                    email = email?.trim()?.lowercase(),
                    password = password,
                    role = role?.toApiValue()
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user.toDomain())
            } else {
                Result.Error(Exception("Failed to update user: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<User> {
        return try {
            val response = usersApi.deleteUser(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user.toDomain())
            } else {
                Result.Error(Exception("Failed to delete user: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
