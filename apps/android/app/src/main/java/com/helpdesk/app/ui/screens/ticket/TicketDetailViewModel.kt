package com.helpdesk.app.ui.screens.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpdesk.app.data.repository.AuthRepository
import com.helpdesk.app.data.repository.TicketRepository
import com.helpdesk.app.data.repository.UsersRepository
import com.helpdesk.app.domain.model.*
import com.helpdesk.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketDetailUiState(
    val isLoading: Boolean = false,
    val ticket: Ticket? = null,
    val error: String? = null,
    val activities: List<TicketActivityEntry> = emptyList(),
    val isActivityLoading: Boolean = false,
    val agents: List<User>? = null,
    val selectedAgentId: String? = null,
    val userRole: UserRole = UserRole.USER,
    val userId: String? = null,
    val canEdit: Boolean = false,
    val canAssign: Boolean = false,
    val canResolve: Boolean = false,
    val canReopen: Boolean = false,
    val canRequestAssignment: Boolean = false,
    val hasRequestedAssignment: Boolean = false,
    val isOperationInProgress: Boolean = false
)

@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ticketRepository: TicketRepository,
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    private var ticketId: String? = null

    init {
        authRepository.currentUser?.let { user ->
            _uiState.update {
                it.copy(
                    userRole = user.role,
                    userId = user.id
                )
            }
        }
    }

    fun loadTicket(id: String) {
        ticketId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = ticketRepository.getTicket(id)) {
                is Result.Success -> {
                    val ticket = result.data
                    updateTicketState(ticket)
                    loadActivity(id)
                    loadAgentsIfNeeded()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                }
                Result.Loading -> {}
            }
        }
    }

    private fun updateTicketState(ticket: Ticket) {
        val currentState = _uiState.value
        val isAdmin = currentState.userRole == UserRole.ADMIN
        val isAgent = currentState.userRole == UserRole.AGENT
        val isUser = currentState.userRole == UserRole.USER
        val isResolved = ticket.status == TicketStatus.RESOLVED
        val isOwner = ticket.creator.id == currentState.userId
        val isAssignedAgent = isAgent && ticket.assignee?.id == currentState.userId

        _uiState.update {
            it.copy(
                isLoading = false,
                ticket = ticket,
                error = null,
                selectedAgentId = ticket.assignee?.id ?: ticket.assignmentRequest?.id,
                canEdit = !isResolved && (isAdmin || (isUser && isOwner)),
                canAssign = isAdmin && !isResolved,
                canResolve = isAssignedAgent && ticket.status != TicketStatus.RESOLVED,
                canReopen = isAdmin && isResolved,
                canRequestAssignment = isAgent && ticket.assignee == null && !isResolved && !isAssignedAgent,
                hasRequestedAssignment = ticket.assignmentRequest?.id == currentState.userId
            )
        }
    }

    private suspend fun loadActivity(ticketId: String) {
        _uiState.update { it.copy(isActivityLoading = true) }

        when (val result = ticketRepository.getTicketActivity(ticketId)) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        activities = result.data,
                        isActivityLoading = false
                    )
                }
            }
            is Result.Error -> {
                _uiState.update { it.copy(isActivityLoading = false) }
            }
            Result.Loading -> {}
        }
    }

    private suspend fun loadAgentsIfNeeded() {
        if (_uiState.value.canAssign) {
            when (val result = usersRepository.getUsers(UserRole.AGENT)) {
                is Result.Success -> {
                    _uiState.update { it.copy(agents = result.data) }
                }
                else -> {}
            }
        }
    }

    fun selectAgent(agentId: String) {
        _uiState.update { it.copy(selectedAgentId = agentId) }
    }

    fun assignTicket() {
        val id = ticketId ?: return
        val assigneeId = _uiState.value.selectedAgentId ?: _uiState.value.ticket?.assignmentRequest?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true) }

            when (val result = ticketRepository.assignTicket(id, assigneeId)) {
                is Result.Success -> {
                    updateTicketState(result.data)
                    loadActivity(id)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            error = result.exception.message
                        )
                    }
                }
                Result.Loading -> {}
            }

            _uiState.update { it.copy(isOperationInProgress = false) }
        }
    }

    fun resolveTicket() {
        val id = ticketId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true) }

            when (val result = ticketRepository.resolveTicket(id)) {
                is Result.Success -> {
                    updateTicketState(result.data)
                    loadActivity(id)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            error = result.exception.message
                        )
                    }
                }
                Result.Loading -> {}
            }

            _uiState.update { it.copy(isOperationInProgress = false) }
        }
    }

    fun reopenTicket() {
        val id = ticketId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true) }

            when (val result = ticketRepository.updateTicket(id, UpdateTicketPayload(status = TicketStatus.OPEN))) {
                is Result.Success -> {
                    updateTicketState(result.data)
                    loadActivity(id)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            error = result.exception.message
                        )
                    }
                }
                Result.Loading -> {}
            }

            _uiState.update { it.copy(isOperationInProgress = false) }
        }
    }

    fun requestAssignment() {
        val id = ticketId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true) }

            when (val result = ticketRepository.requestAssignment(id)) {
                is Result.Success -> {
                    updateTicketState(result.data)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            error = result.exception.message
                        )
                    }
                }
                Result.Loading -> {}
            }

            _uiState.update { it.copy(isOperationInProgress = false) }
        }
    }
}
