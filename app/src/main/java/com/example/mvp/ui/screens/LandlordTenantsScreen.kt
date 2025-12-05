package com.example.mvp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mvp.data.ConnectionStatus
import com.example.mvp.data.LandlordTenantConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordTenantsScreen(
    connections: List<LandlordTenantConnection>,
    tenantUsers: Map<String, com.example.mvp.data.User> = emptyMap(),
    onAddTenant: (String) -> Unit,
    onSelectTenant: (String) -> Unit,
    onCancelConnection: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddTenantDialog by remember { mutableStateOf(false) }
    var newTenantEmail by remember { mutableStateOf("") }
    
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
                        val tenantUser = tenantUsers[connection.tenantEmail] 
                            ?: tenantUsers.entries.find { it.key.lowercase() == connection.tenantEmail.lowercase() }?.value
                        TenantListItem(
                            tenantEmail = connection.tenantEmail,
                            tenantName = tenantUser?.name ?: connection.tenantEmail.split("@").first().replaceFirstChar { it.uppercase() },
                            tenantAddress = tenantUser?.address,
                            isSelected = false,
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
                        val tenantUser = tenantUsers[connection.tenantEmail] 
                            ?: tenantUsers.entries.find { it.key.lowercase() == connection.tenantEmail.lowercase() }?.value
                        TenantListItem(
                            tenantEmail = connection.tenantEmail,
                            tenantName = tenantUser?.name ?: connection.tenantEmail.split("@").first().replaceFirstChar { it.uppercase() },
                            tenantAddress = tenantUser?.address,
                            isSelected = false,
                            onClick = {},
                            isPending = true,
                            connectionId = connection.id,
                            onCancel = { onCancelConnection(connection.id) }
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
}

@Composable
fun TenantListItem(
    tenantEmail: String,
    tenantName: String,
    tenantAddress: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    isPending: Boolean = false,
    connectionId: String? = null,
    onCancel: (() -> Unit)? = null
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tenantName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (tenantAddress != null && tenantAddress.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = tenantAddress,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (isPending) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            if (isPending && onCancel != null) {
                IconButton(
                    onClick = { onCancel() },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Request",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

