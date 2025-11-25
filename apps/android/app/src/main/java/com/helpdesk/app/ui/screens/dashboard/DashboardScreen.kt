package com.helpdesk.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.helpdesk.app.domain.model.Ticket
import com.helpdesk.app.domain.model.TicketStatus
import com.helpdesk.app.domain.model.UserRole
import com.helpdesk.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTicket: (String) -> Unit,
    onNavigateToCreateTicket: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToStatusSummary: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            if (uiState.canCreate) {
                FloatingActionButton(
                    onClick = onNavigateToCreateTicket,
                    containerColor = Primary,
                    contentColor = Background,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Ticket")
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = pullRefreshState
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Card
                item {
                    HeroCard(
                        userName = uiState.userName,
                        statusCounts = uiState.statusCounts,
                        onSignOut = {
                            viewModel.signOut()
                            onSignOut()
                        },
                        onNotificationClick = { /* TODO: Implement notifications */ }
                    )
                }

                // Control Panel
                item {
                    ControlPanel(
                        selectedFilter = uiState.statusFilter,
                        onFilterChange = { viewModel.setStatusFilter(it) },
                        assignedOnly = uiState.assignedOnly,
                        onAssignedOnlyChange = { viewModel.setAssignedOnly(it) },
                        showAssignedToggle = uiState.userRole != UserRole.USER
                    )
                }

                // Quick Navigation for Admin
                if (uiState.userRole == UserRole.ADMIN) {
                    item {
                        AdminQuickNav(
                            onUserManagement = onNavigateToUserManagement,
                            onStatusSummary = onNavigateToStatusSummary
                        )
                    }
                }

                // Tickets Section Header
                item {
                    Text(
                        text = "Tickets",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Tickets List
                if (uiState.isLoading && uiState.tickets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                } else if (uiState.tickets.isEmpty()) {
                    item {
                        EmptyState(canCreate = uiState.canCreate)
                    }
                } else {
                    items(uiState.tickets, key = { it.id }) { ticket ->
                        TicketCard(
                            ticket = ticket,
                            onClick = { onNavigateToTicket(ticket.id) }
                        )
                    }
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    userName: String,
    statusCounts: StatusCountsUi,
    onSignOut: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Surface.copy(alpha = 0.85f))
            .border(1.dp, Primary.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "COMMAND CENTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hi, ${userName.split(" ").first()}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Surface, CircleShape)
                        .border(1.dp, Border, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextPrimary
                    )
                }

                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .border(1.dp, BorderHover, RoundedCornerShape(999.dp))
                ) {
                    Text("Sign out", color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Monitor tickets, workload, and signals in one sleek view.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                Triple("Open", statusCounts.open, "Active queue"),
                Triple("In progress", statusCounts.inProgress, "Being handled"),
                Triple("Resolved", statusCounts.resolved, "Closed"),
                Triple("Total", statusCounts.total, "Tracked")
            ).forEach { (label, value, hint) ->
                StatCard(
                    label = label,
                    value = value.toString(),
                    hint = hint,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Surface.copy(alpha = 0.9f))
            .border(1.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = Secondary
        )
    }
}

@Composable
private fun ControlPanel(
    selectedFilter: TicketStatus?,
    onFilterChange: (TicketStatus?) -> Unit,
    assignedOnly: Boolean,
    onAssignedOnlyChange: (Boolean) -> Unit,
    showAssignedToggle: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Background.copy(alpha = 0.8f))
            .border(1.dp, TextSecondary.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                label = "All",
                isSelected = selectedFilter == null,
                onClick = { onFilterChange(null) }
            )
            FilterChip(
                label = "Open",
                isSelected = selectedFilter == TicketStatus.OPEN,
                onClick = { onFilterChange(TicketStatus.OPEN) }
            )
            FilterChip(
                label = "In Progress",
                isSelected = selectedFilter == TicketStatus.IN_PROGRESS,
                onClick = { onFilterChange(TicketStatus.IN_PROGRESS) }
            )
            FilterChip(
                label = "Resolved",
                isSelected = selectedFilter == TicketStatus.RESOLVED,
                onClick = { onFilterChange(TicketStatus.RESOLVED) }
            )
        }

        if (showAssignedToggle) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assigned to me",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Switch(
                    checked = assignedOnly,
                    onCheckedChange = onAssignedOnlyChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Secondary.copy(alpha = 0.5f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Border
                    )
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) Primary else Background)
            .border(1.dp, if (isSelected) Primary else Border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) Background else TextSecondary
        )
    }
}

@Composable
private fun AdminQuickNav(
    onUserManagement: () -> Unit,
    onStatusSummary: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickNavCard(
            icon = Icons.Default.People,
            title = "User Management",
            subtitle = "Manage members",
            onClick = onUserManagement,
            modifier = Modifier.weight(1f)
        )
        QuickNavCard(
            icon = Icons.Default.Assessment,
            title = "Status Summary",
            subtitle = "Org overview",
            onClick = onStatusSummary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickNavCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TicketCard(
    ticket: Ticket,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceVariant)
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${ticket.id.take(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            StatusBadge(status = ticket.status)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = ticket.description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Priority: ${ticket.priority.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "Type: ${ticket.issueType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = ticket.assignee?.name?.let { "Assigned to $it" } ?: "Unassigned",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        if (ticket.pendingSync) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Warning.copy(alpha = 0.2f))
                    .border(1.dp, Warning, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Pending sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = Warning
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: TicketStatus) {
    val (backgroundColor, text) = when (status) {
        TicketStatus.OPEN -> StatusOpen to "Open"
        TicketStatus.IN_PROGRESS -> StatusInProgress to "In Progress"
        TicketStatus.RESOLVED -> StatusResolved to "Resolved"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
    }
}

@Composable
private fun EmptyState(canCreate: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No tickets found",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (canCreate) {
                "Try a different filter or create a new ticket below."
            } else {
                "Try a different filter or request access from an admin."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private val sp = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
