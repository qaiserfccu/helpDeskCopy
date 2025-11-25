package com.helpdesk.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpdesk.app.data.repository.ReportsRepository
import com.helpdesk.app.domain.model.*
import com.helpdesk.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusSummaryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val totalTickets: Int = 0,
    val openCount: Int = 0,
    val inProgressCount: Int = 0,
    val resolvedCount: Int = 0,
    val statusBuckets: List<StatusBucket> = emptyList(),
    val assignments: List<AgentAssignment> = emptyList(),
    val oldestOpen: List<Ticket> = emptyList(),
    val highPriority: List<Ticket> = emptyList(),
    val staleTickets: List<Ticket> = emptyList(),
    val recentActivity: List<TicketActivityEntry> = emptyList()
)

@HiltViewModel
class StatusSummaryViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusSummaryUiState())
    val uiState: StateFlow<StatusSummaryUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadOverview()
            loadEscalations()
            loadActivity()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadOverview()
            loadEscalations()
            loadActivity()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadOverview() {
        when (val result = reportsRepository.getAdminOverviewReport()) {
            is Result.Success -> {
                val report = result.data
                val openCount = report.statusCounts.open
                val inProgressCount = report.statusCounts.inProgress
                val resolvedCount = report.statusCounts.resolved
                val total = report.statusCounts.total

                _uiState.update {
                    it.copy(
                        totalTickets = total,
                        openCount = openCount,
                        inProgressCount = inProgressCount,
                        resolvedCount = resolvedCount,
                        statusBuckets = listOf(
                            StatusBucket(TicketStatus.OPEN, openCount),
                            StatusBucket(TicketStatus.IN_PROGRESS, inProgressCount),
                            StatusBucket(TicketStatus.RESOLVED, resolvedCount)
                        ),
                        assignments = report.assignmentLoad,
                        oldestOpen = report.oldestOpen,
                        error = null
                    )
                }
            }
            is Result.Error -> {
                _uiState.update { it.copy(error = result.exception.message) }
            }
            Result.Loading -> {}
        }
    }

    private suspend fun loadEscalations() {
        when (val result = reportsRepository.getAdminEscalationReport()) {
            is Result.Success -> {
                val report = result.data
                _uiState.update {
                    it.copy(
                        highPriority = report.highPriority,
                        staleTickets = report.staleTickets
                    )
                }
            }
            else -> {}
        }
    }

    private suspend fun loadActivity() {
        when (val result = reportsRepository.getRecentActivity(25)) {
            is Result.Success -> {
                _uiState.update { it.copy(recentActivity = result.data) }
            }
            else -> {}
        }
    }
}
