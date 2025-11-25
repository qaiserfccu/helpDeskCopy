package com.helpdesk.app.ui.screens.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpdesk.app.data.repository.TicketRepository
import com.helpdesk.app.domain.model.*
import com.helpdesk.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketFormUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false,
    val isLocked: Boolean = false,
    val error: String? = null,
    val description: String = "",
    val priority: TicketPriority = TicketPriority.MEDIUM,
    val issueType: IssueType = IssueType.OTHER,
    val ticketId: String? = null
)

@HiltViewModel
class TicketFormViewModel @Inject constructor(
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketFormUiState())
    val uiState: StateFlow<TicketFormUiState> = _uiState.asStateFlow()

    fun loadTicket(ticketId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, ticketId = ticketId) }

            when (val result = ticketRepository.getTicket(ticketId)) {
                is Result.Success -> {
                    val ticket = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            description = ticket.description,
                            priority = ticket.priority,
                            issueType = ticket.issueType,
                            isLocked = ticket.status == TicketStatus.RESOLVED
                        )
                    }
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

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description, error = null) }
    }

    fun updatePriority(priority: TicketPriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun updateIssueType(issueType: IssueType) {
        _uiState.update { it.copy(issueType = issueType) }
    }

    fun submit() {
        val currentState = _uiState.value

        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(error = "Please describe the issue") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val result = if (currentState.ticketId != null) {
                ticketRepository.updateTicket(
                    ticketId = currentState.ticketId,
                    payload = UpdateTicketPayload(
                        description = currentState.description.trim(),
                        priority = currentState.priority,
                        issueType = currentState.issueType
                    )
                )
            } else {
                ticketRepository.createTicket(
                    payload = CreateTicketPayload(
                        description = currentState.description.trim(),
                        priority = currentState.priority,
                        issueType = currentState.issueType
                    )
                )
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, isSaved = true) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = result.exception.message ?: "Failed to save ticket"
                        )
                    }
                }
                Result.Loading -> {}
            }
        }
    }
}
