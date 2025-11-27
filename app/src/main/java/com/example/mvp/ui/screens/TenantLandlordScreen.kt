package com.example.mvp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvp.data.ConnectionStatus
import com.example.mvp.data.LandlordTenantConnection
import com.example.mvp.data.DirectMessage

@Composable
fun TenantLandlordScreen(
    connection: LandlordTenantConnection?,
    pendingConnections: List<LandlordTenantConnection>,
    messages: List<DirectMessage>,
    currentUserEmail: String,
    currentUserName: String,
    onConfirmConnection: (String, Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenChat: () -> Unit,
    onBack: () -> Unit,
    globalLastViewedTimestamps: MutableMap<String, String>? = null // Pass global timestamps
) {
    var showRequestsDialog by remember { mutableStateOf(false) }
    
    val pendingCount = pendingConnections.size
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Landlord",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Landlord Requests Button - Always visible
        Button(
            onClick = { showRequestsDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Landlord Requests")
                if (pendingCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text("$pendingCount")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Landlord Requests Dialog
        if (showRequestsDialog) {
            AlertDialog(
                onDismissRequest = { showRequestsDialog = false },
                title = {
                    Text(
                        text = "Landlord Requests",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pendingConnections.isEmpty()) {
                            Text(
                                text = "No pending requests",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            pendingConnections.forEach { pending ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = pending.landlordEmail.split("@").first().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = pending.landlordEmail,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    onConfirmConnection(pending.id, true)
                                                    showRequestsDialog = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Accept")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    onConfirmConnection(pending.id, false)
                                                    showRequestsDialog = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Decline")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRequestsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
        
        // Connected Landlord Info
        connection?.let { conn ->
            if (conn.status == ConnectionStatus.CONNECTED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Landlord Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Email:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = conn.landlordEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        } ?: run {
            // No connection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Landlord Connected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your landlord will send you a connection request.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        // Messages Button - At the bottom for connected landlords
        connection?.let { conn ->
            if (conn.status == ConnectionStatus.CONNECTED) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Messages Button
                // Unread count is now based on readBy field, not timestamps
                val unreadCount = remember(messages, currentUserEmail) {
                    messages.filter { 
                        // Only count messages sent by landlord that haven't been read by current user
                        it.senderEmail.lowercase() != currentUserEmail.lowercase() &&
                        !it.readBy.contains(currentUserEmail.lowercase())
                    }.size
                }
                
                Button(
                    onClick = {
                        onOpenChat()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Messages")
                }
            }
        }
    }
}

