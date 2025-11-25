package com.helpdesk.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpdesk.app.data.api.SocketEvent
import com.helpdesk.app.data.api.SocketManager
import com.helpdesk.app.data.repository.AuthRepository
import com.helpdesk.app.data.repository.ReportsRepository
import com.helpdesk.app.data.repository.TicketRepository
import com.helpdesk.app.domain.model.Ticket
import com.helpdesk.app.domain.model.TicketFilters
import com.helpdesk.app.domain.model.TicketStatus
import com.helpdesk.app.domain.model.UserRole
import com.helpdesk.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusCountsUi(
    val open: Int = 0,
    val inProgress: Int = 0,
    val resolved: Int = 0,
    val total: Int = 0
)

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val tickets: List<Ticket> = emptyList(),
    val statusFilter: TicketStatus? = null,
    val assignedOnly: Boolean = false,
    val userName: String = "",
    val userRole: UserRole = UserRole.USER,
    val canCreate: Boolean = false,
    val statusCounts: StatusCountsUi = StatusCountsUi()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ticketRepository: TicketRepository,
    private val reportsRepository: ReportsRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Set initial user info
        authRepository.currentUser?.let { user ->
            _uiState.update {
                it.copy(
                    userName = user.name,
                    userRole = user.role,
                    canCreate = user.role == UserRole.USER || user.role == UserRole.ADMIN
                )
            }
        }

        // Observe auth session changes
        viewModelScope.launch {
            authRepository.sessionState.collect { session ->
                session?.let {
                    _uiState.update { state ->
                        state.copy(
                            userName = it.user.name,
                            userRole = it.user.role,
                            canCreate = it.user.role == UserRole.USER || it.user.role == UserRole.ADMIN
                        )
                    }
                    // Connect socket when session is available
                    socketManager.syncSession(it.accessToken)
                }
            }
        }

        // Observe socket events for real-time updates
        viewModelScope.launch {
            socketManager.events.collect { event ->
                when (event) {
                    is SocketEvent.TicketCreated,
                    is SocketEvent.TicketUpdated -> {
                        // Refresh tickets when we receive updates
                        loadTickets()
                    }
                    is SocketEvent.TicketActivity -> {
                        // Could show a notification here
                    }
                    else -> { /* Ignore other events */ }
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadTickets()
            loadStatusSummary()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadTickets()
            loadStatusSummary()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setStatusFilter(status: TicketStatus?) {
        _uiState.update { it.copy(statusFilter = status) }
        viewModelScope.launch {
            loadTickets()
        }
    }

    fun setAssignedOnly(assignedOnly: Boolean) {
        _uiState.update { it.copy(assignedOnly = assignedOnly) }
        viewModelScope.launch {
            loadTickets()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            socketManager.disconnect()
            authRepository.signOut()
        }
    }

    private suspend fun loadTickets() {
        val currentState = _uiState.value
        val filters = TicketFilters(
            status = currentState.statusFilter,
            assignedToMe = if (currentState.assignedOnly) true else null
        )

        when (val result = ticketRepository.getTickets(filters)) {
            is Result.Success -> {
                _uiState.update { it.copy(tickets = result.data, error = null) }
            }
            is Result.Error -> {
                _uiState.update { it.copy(error = result.exception.message) }
            }
            Result.Loading -> { /* Already handling loading state */ }
        }
    }

    private suspend fun loadStatusSummary() {
        // Calculate from current tickets or fetch summary
        val currentTickets = _uiState.value.tickets
        if (currentTickets.isNotEmpty()) {
            val counts = currentTickets.groupingBy { it.status }.eachCount()
            _uiState.update {
                it.copy(
                    statusCounts = StatusCountsUi(
                        open = counts[TicketStatus.OPEN] ?: 0,
                        inProgress = counts[TicketStatus.IN_PROGRESS] ?: 0,
                        resolved = counts[TicketStatus.RESOLVED] ?: 0,
                        total = currentTickets.size
                    )
                )
            }
        }

        // For admins, fetch the full summary
        if (_uiState.value.userRole == UserRole.ADMIN) {
            when (val result = reportsRepository.getStatusSummary()) {
                is Result.Success -> {
                    val summary = result.data
                    val openCount = summary.statuses.find { it.status == TicketStatus.OPEN }?.count ?: 0
                    val inProgressCount = summary.statuses.find { it.status == TicketStatus.IN_PROGRESS }?.count ?: 0
                    val resolvedCount = summary.statuses.find { it.status == TicketStatus.RESOLVED }?.count ?: 0

                    _uiState.update {
                        it.copy(
                            statusCounts = StatusCountsUi(
                                open = openCount,
                                inProgress = inProgressCount,
                                resolved = resolvedCount,
                                total = openCount + inProgressCount + resolvedCount
                            )
                        )
                    }
                }
                else -> { /* Use calculated counts */ }
            }
        }
    }
}
