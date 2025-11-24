package com.example.mvp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.mvp.data.ConnectionStatus
import com.example.mvp.data.LandlordTenantConnection
import com.example.mvp.data.DirectMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordTenantsScreen(
    connections: List<LandlordTenantConnection>,
    selectedTenantEmail: String?,
    messages: List<DirectMessage>,
    currentUserEmail: String,
    currentUserName: String,
    onAddTenant: (String) -> Unit,
    onSelectTenant: (String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var showAddTenantDialog by remember { mutableStateOf(false) }
    var newTenantEmail by remember { mutableStateOf("") }
    var newMessage by remember { mutableStateOf("") }
    
    if (showAddTenantDialog) {
        AlertDialog(
            onDismissRequest = { showAddTenantDialog = false },
            title = { Text("Add Tenant") },
            text = {
                Column {
                    Text("Enter the tenant's email address:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTenantEmail,
                        onValueChange = { newTenantEmail = it },
                        label = { Text("Tenant Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTenantEmail.isNotBlank()) {
                            onAddTenant(newTenantEmail.trim())
                            newTenantEmail = ""
                            showAddTenantDialog = false
                        }
                    },
                    enabled = newTenantEmail.isNotBlank()
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTenantDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tenant List - Always visible
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Tenants",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showAddTenantDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add Tenant")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val connectedTenants = connections.filter { it.status == ConnectionStatus.CONNECTED }
            val pendingTenants = connections.filter { it.status == ConnectionStatus.PENDING }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (connectedTenants.isNotEmpty()) {
                    item {
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    items(connectedTenants) { connection ->
                        TenantListItem(
                            tenantEmail = connection.tenantEmail,
                            isSelected = selectedTenantEmail == connection.tenantEmail,
                            onClick = { onSelectTenant(connection.tenantEmail) }
                        )
                    }
                }
                
                if (pendingTenants.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    items(pendingTenants) { connection ->
                        TenantListItem(
                            tenantEmail = connection.tenantEmail,
                            isSelected = false,
                            onClick = {},
                            isPending = true
                        )
                    }
                }
                
                if (connections.isEmpty()) {
                    item {
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
                                    text = "No tenants yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Messages - Full screen overlay when tenant is selected
        selectedTenantEmail?.let { tenantEmail ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                color = MaterialTheme.colorScheme.surface
            ) {
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                
                // Auto-scroll to bottom when new messages arrive
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
                
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top bar with close button
                    TopAppBar(
                        title = {
                            Text(
                                text = "Messages with $tenantEmail",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { onSelectTenant("") }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    // Messages list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No messages yet. Start a conversation!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            items(messages) { message ->
                                val isSentByMe = message.senderEmail == currentUserEmail
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        modifier = Modifier.widthIn(max = 300.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSentByMe)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp)
                                        ) {
                                            Text(
                                                text = message.senderName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = message.text,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = message.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Input area
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newMessage,
                                onValueChange = { newMessage = it },
                                placeholder = { Text("Type a message...") },
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                shape = MaterialTheme.shapes.medium
                            )
                            FilledIconButton(
                                onClick = {
                                    if (newMessage.isNotBlank()) {
                                        onSendMessage(tenantEmail, newMessage)
                                        newMessage = ""
                                    }
                                },
                                enabled = newMessage.isNotBlank(),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TenantListItem(
    tenantEmail: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isPending: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = tenantEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isPending) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

