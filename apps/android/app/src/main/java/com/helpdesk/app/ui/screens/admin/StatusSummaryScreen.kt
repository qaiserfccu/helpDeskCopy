package com.helpdesk.app.ui.screens.admin

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
import com.helpdesk.app.domain.model.TicketActivityEntry
import com.helpdesk.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSummaryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTicket: (String) -> Unit,
    viewModel: StatusSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Organization Report", color = TextPrimary) },
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Column {
                        Text(
                            text = "Live ticket overview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Summary Highlights
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HighlightCard(
                            label = "Total tickets",
                            value = uiState.totalTickets.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        HighlightCard(
                            label = "Open",
                            value = uiState.openCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HighlightCard(
                            label = "In progress",
                            value = uiState.inProgressCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        HighlightCard(
                            label = "Resolved",
                            value = uiState.resolvedCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Status Distribution
                item {
                    SectionCard(title = "Status distribution") {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (uiState.statusBuckets.isEmpty()) {
                            Text(
                                text = "No summary data yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.statusBuckets.forEach { bucket ->
                                    val percentage = if (uiState.totalTickets > 0) {
                                        (bucket.count.toFloat() / uiState.totalTickets * 100).toInt()
                                    } else 0

                                    StatusRow(
                                        label = bucket.status.displayName(),
                                        count = bucket.count,
                                        percentage = percentage
                                    )
                                }
                            }
                        }
                    }
                }

                // Assignment Load
                item {
                    SectionCard(title = "Assignment load") {
                        if (uiState.assignments.isEmpty()) {
                            Text(
                                text = "No active assignments.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.assignments.forEach { assignment ->
                                    AssignmentRow(
                                        name = assignment.agent?.name ?: "Unknown agent",
                                        email = assignment.agent?.email ?: "N/A",
                                        count = assignment.count
                                    )
                                }
                            }
                        }
                    }
                }

                // Oldest Open Tickets
                if (uiState.oldestOpen.isNotEmpty()) {
                    item {
                        SectionCard(
                            title = "Oldest open tickets",
                            subtitle = "Longest waiting issues"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.oldestOpen.forEach { ticket ->
                                    MiniTicketRow(
                                        ticket = ticket,
                                        onClick = { onNavigateToTicket(ticket.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // High Priority Alerts
                if (uiState.highPriority.isNotEmpty()) {
                    item {
                        SectionCard(
                            title = "High priority alerts",
                            subtitle = "Requires immediate action"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.highPriority.forEach { ticket ->
                                    MiniTicketRow(
                                        ticket = ticket,
                                        onClick = { onNavigateToTicket(ticket.id) },
                                        showPriority = true
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Activity
                if (uiState.recentActivity.isNotEmpty()) {
                    item {
                        SectionCard(title = "Recent activity") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.recentActivity.forEach { activity ->
                                    ActivityRow(activity = activity)
                                }
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
private fun HighlightCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceVariant)
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun StatusRow(
    label: String,
    count: Int,
    percentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = "$percentage% of total",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (percentage.coerceAtLeast(5).toFloat() / 100f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(Primary)
                )
            }
        }
    }
}

@Composable
private fun AssignmentRow(
    name: String,
    email: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Secondary
            )
        }
    }
}

@Composable
private fun MiniTicketRow(
    ticket: Ticket,
    onClick: () -> Unit,
    showPriority: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(ticket.status.displayName())
                    if (showPriority) {
                        append(" • ")
                        append(ticket.priority.name.uppercase())
                        append(" priority")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (showPriority) Warning else TextSecondary
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun ActivityRow(activity: TicketActivityEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.actor.name,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Text(
                text = activity.describe(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = formatTime(activity.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

private fun formatTime(iso: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val date = inputFormat.parse(iso) ?: return iso
        outputFormat.format(date)
    } catch (e: Exception) {
        iso
    }
}
