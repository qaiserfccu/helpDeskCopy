package com.helpdesk.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpdesk.app.data.repository.ReportsRepository
import com.helpdesk.app.data.repository.UsersRepository
import com.helpdesk.app.domain.model.AgentAssignment
import com.helpdesk.app.domain.model.User
import com.helpdesk.app.domain.model.UserRole
import com.helpdesk.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserManagementUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val users: List<User> = emptyList(),
    val assignments: List<AgentAssignment> = emptyList(),
    val roleFilter: UserRole? = null,
    val showUserDialog: Boolean = false,
    val editingUser: User? = null,
    val formName: String = "",
    val formEmail: String = "",
    val formPassword: String = "",
    val formRole: UserRole = UserRole.AGENT,
    val formError: String? = null,
    val isSaving: Boolean = false,
    val deletingUserId: String? = null
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val usersRepository: UsersRepository,
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadUsers()
            loadAssignments()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadUsers()
            loadAssignments()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setRoleFilter(role: UserRole?) {
        _uiState.update { it.copy(roleFilter = role) }
        viewModelScope.launch {
            loadUsers()
        }
    }

    fun showCreateDialog() {
        _uiState.update {
            it.copy(
                showUserDialog = true,
                editingUser = null,
                formName = "",
                formEmail = "",
                formPassword = "",
                formRole = UserRole.AGENT,
                formError = null
            )
        }
    }

    fun showEditDialog(user: User) {
        _uiState.update {
            it.copy(
                showUserDialog = true,
                editingUser = user,
                formName = user.name,
                formEmail = user.email,
                formPassword = "",
                formRole = user.role,
                formError = null
            )
        }
    }

    fun hideDialog() {
        _uiState.update { it.copy(showUserDialog = false, editingUser = null) }
    }

    fun updateFormName(name: String) {
        _uiState.update { it.copy(formName = name, formError = null) }
    }

    fun updateFormEmail(email: String) {
        _uiState.update { it.copy(formEmail = email, formError = null) }
    }

    fun updateFormPassword(password: String) {
        _uiState.update { it.copy(formPassword = password, formError = null) }
    }

    fun updateFormRole(role: UserRole) {
        _uiState.update { it.copy(formRole = role) }
    }

    fun saveUser() {
        val currentState = _uiState.value

        if (currentState.formName.isBlank()) {
            _uiState.update { it.copy(formError = "Please enter a name") }
            return
        }
        if (currentState.formEmail.isBlank()) {
            _uiState.update { it.copy(formError = "Please enter an email") }
            return
        }
        if (currentState.editingUser == null && currentState.formPassword.length < 6) {
            _uiState.update { it.copy(formError = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, formError = null) }

            val result = if (currentState.editingUser != null) {
                usersRepository.updateUser(
                    userId = currentState.editingUser.id,
                    name = currentState.formName.trim(),
                    email = currentState.formEmail.trim(),
                    password = currentState.formPassword.takeIf { it.isNotBlank() },
                    role = currentState.formRole
                )
            } else {
                usersRepository.createUser(
                    name = currentState.formName.trim(),
                    email = currentState.formEmail.trim(),
                    password = currentState.formPassword,
                    role = currentState.formRole
                )
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            showUserDialog = false,
                            editingUser = null
                        )
                    }
                    loadUsers()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            formError = result.exception.message ?: "Failed to save user"
                        )
                    }
                }
                Result.Loading -> {}
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingUserId = userId) }

            when (usersRepository.deleteUser(userId)) {
                is Result.Success -> {
                    loadUsers()
                }
                is Result.Error -> {
                    // Could show an error toast here
                }
                Result.Loading -> {}
            }

            _uiState.update { it.copy(deletingUserId = null) }
        }
    }

    private suspend fun loadUsers() {
        val roleFilter = _uiState.value.roleFilter
        when (val result = usersRepository.getUsers(roleFilter)) {
            is Result.Success -> {
                _uiState.update { it.copy(users = result.data, error = null) }
            }
            is Result.Error -> {
                _uiState.update { it.copy(error = result.exception.message) }
            }
            Result.Loading -> {}
        }
    }

    private suspend fun loadAssignments() {
        when (val result = reportsRepository.getAdminOverviewReport()) {
            is Result.Success -> {
                _uiState.update { it.copy(assignments = result.data.assignmentLoad) }
            }
            else -> {}
        }
    }
}
