package com.example.mvp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvp.data.Ticket
import com.example.mvp.data.TicketStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorLandlordChatScreen(
    tickets: List<Ticket>,
    contractorId: String?,
    onTicketClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("In Progress") }
    
    // Filter tickets assigned to this contractor
    val assignedTickets = tickets.filter { ticket ->
        ticket.assignedTo == contractorId || ticket.assignedContractor == contractorId
    }
    
    val filteredTickets = when (selectedFilter) {
        "In Progress" -> assignedTickets.filter { 
            it.status == TicketStatus.ASSIGNED || it.status == TicketStatus.SCHEDULED 
        }
        "Finished" -> assignedTickets.filter { 
            it.status == TicketStatus.COMPLETED 
        }
        else -> assignedTickets
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with Landlords", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "In Progress",
                    onClick = { selectedFilter = "In Progress" },
                    label = { Text("In Progress") }
                )
                FilterChip(
                    selected = selectedFilter == "Finished",
                    onClick = { selectedFilter = "Finished" },
                    label = { Text("Finished") }
                )
            }
            
            // Tickets List
            if (filteredTickets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No tickets found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Tickets will appear here once assigned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTickets) { ticket ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTicketClick(ticket.id) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ticket.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = when (ticket.status) {
                                            TicketStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                                            TicketStatus.ASSIGNED -> MaterialTheme.colorScheme.primaryContainer
                                            TicketStatus.SCHEDULED -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.errorContainer
                                        },
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = ticket.status.name.lowercase(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Text(
                                    text = ticket.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = ticket.category,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Text(
                                        text = "Tap to chat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}





