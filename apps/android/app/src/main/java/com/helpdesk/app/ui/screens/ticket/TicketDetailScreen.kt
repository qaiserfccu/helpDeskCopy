package com.helpdesk.app.ui.screens.ticket

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.helpdesk.app.domain.model.TicketActivityEntry
import com.helpdesk.app.domain.model.TicketStatus
import com.helpdesk.app.domain.model.User
import com.helpdesk.app.domain.model.UserRole
import com.helpdesk.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticketId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: TicketDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(ticketId) {
        viewModel.loadTicket(ticketId)
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Ticket Details", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.ticket == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Ticket not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        } else {
            val ticket = uiState.ticket!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ticket ID and Description
                item {
                    Text(
                        text = "Ticket #${ticket.id.take(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ticket.description,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }

                // Badges Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BadgeCard(
                            label = "Status",
                            value = ticket.status.displayName(),
                            modifier = Modifier.weight(1f)
                        )
                        BadgeCard(
                            label = "Priority",
                            value = ticket.priority.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.weight(1f)
                        )
                        BadgeCard(
                            label = "Type",
                            value = ticket.issueType.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Creator
                item {
                    Section(label = "Creator") {
                        Text(
                            text = ticket.creator.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }

                // Pending Sync Banner
                if (ticket.pendingSync) {
                    item {
                        PendingSyncBanner()
                    }
                }

                // Assignee
                item {
                    Section(label = "Assignee") {
                        Text(
                            text = ticket.assignee?.name ?: "Unassigned",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        if (ticket.assignmentRequest != null && ticket.assignee == null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Requested by ${ticket.assignmentRequest.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Warning
                            )
                        }
                    }
                }

                // Assignment Section (for admins)
                if (uiState.canAssign && !uiState.agents.isNullOrEmpty()) {
                    item {
                        AssignmentSection(
                            agents = uiState.agents,
                            selectedAgentId = uiState.selectedAgentId,
                            onAgentSelected = { viewModel.selectAgent(it) }
                        )
                    }
                }

                // Activity Section
                item {
                    Section(label = "Activity") {
                        if (uiState.isActivityLoading) {
                            CircularProgressIndicator(
                                color = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (uiState.activities.isEmpty()) {
                            Text(
                                text = "No recent changes yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.activities.forEach { activity ->
                                    ActivityItem(activity = activity)
                                }
                            }
                        }
                    }
                }

                // Resolved Notice
                if (ticket.status == TicketStatus.RESOLVED && uiState.userRole != UserRole.ADMIN) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StatusInProgress)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "This ticket is resolved. Contact support if you need further changes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Actions
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.canEdit) {
                            OutlinedButton(
                                onClick = { onNavigateToEdit(ticket.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                )
                            ) {
                                Text("Edit ticket")
                            }
                        }

                        if (uiState.canAssign) {
                            Button(
                                onClick = { viewModel.assignTicket() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                enabled = uiState.selectedAgentId != null || ticket.assignmentRequest != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = Background
                                )
                            ) {
                                val buttonText = when {
                                    uiState.selectedAgentId != null -> {
                                        val agent = uiState.agents?.find { it.id == uiState.selectedAgentId }
                                        "Assign to ${agent?.name ?: "agent"}"
                                    }
                                    ticket.assignmentRequest != null -> {
                                        "Assign to ${ticket.assignmentRequest.name}"
                                    }
                                    else -> "Select an agent"
                                }
                                Text(buttonText)
                            }
                        }

                        if (uiState.canRequestAssignment) {
                            OutlinedButton(
                                onClick = { viewModel.requestAssignment() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !uiState.hasRequestedAssignment,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                )
                            ) {
                                Text(
                                    if (uiState.hasRequestedAssignment) "Request pending approval"
                                    else "Request assignment"
                                )
                            }
                        }

                        if (uiState.canResolve) {
                            Button(
                                onClick = { viewModel.resolveTicket() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Secondary,
                                    contentColor = Background
                                )
                            ) {
                                Text("Resolve")
                            }
                        }

                        if (uiState.canReopen) {
                            OutlinedButton(
                                onClick = { viewModel.reopenTicket() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Primary
                                )
                            ) {
                                Text("Reopen ticket")
                            }
                        }
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun Section(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun PendingSyncBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Warning.copy(alpha = 0.1f))
            .border(1.dp, Warning, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Pending sync",
            style = MaterialTheme.typography.titleSmall,
            color = Warning
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This change will be sent automatically once you are back online.",
            style = MaterialTheme.typography.bodySmall,
            color = Warning.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AssignmentSection(
    agents: List<User>,
    selectedAgentId: String?,
    onAgentSelected: (String) -> Unit
) {
    Section(label = "Assign ticket") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            agents.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { agent ->
                        val isSelected = selectedAgentId == agent.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) StatusInProgress else Surface)
                                .border(
                                    1.dp,
                                    if (isSelected) Primary else Border,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onAgentSelected(agent.id) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = agent.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Primary else TextPrimary
                                )
                                Text(
                                    text = agent.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    // Fill remaining space if odd number
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select an agent and tap Assign to re-route immediately.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ActivityItem(activity: TicketActivityEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = activity.describe(),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatActivityTime(activity.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

private fun formatActivityTime(iso: String): String {
    return try {
        java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault())
            .format(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(iso)!!)
    } catch (e: Exception) {
        iso
    }
}
