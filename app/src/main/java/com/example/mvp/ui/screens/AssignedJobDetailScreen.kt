package com.example.mvp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvp.data.Job
import com.example.mvp.data.Ticket
import com.example.mvp.data.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedJobDetailScreen(
    job: Job,
    ticket: Ticket,
    tenantUser: com.example.mvp.data.User? = null, // Add tenant user parameter
    onBack: () -> Unit,
    onSchedule: (String) -> Unit, // jobId
    onComplete: (String) -> Unit,
    onMessageLandlord: (() -> Unit)? = null, // Callback to message landlord
    onMessageTenant: (() -> Unit)? = null, // Callback to message tenant
    userRole: UserRole
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = ticket.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = ticket.category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Description Section
            item {
                Column {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ticket.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Job Information
            item {
                Column {
                    Text(
                        text = "Job Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Status:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = job.status,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Property Address Section
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Property Address:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                if (tenantUser != null && (tenantUser.address != null || tenantUser.city != null || tenantUser.state != null)) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        tenantUser.address?.takeIf { it.isNotBlank() }?.let { address ->
                                            Text(
                                                text = address,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        val cityState = listOfNotNull(
                                            tenantUser.city?.takeIf { it.isNotBlank() },
                                            tenantUser.state?.takeIf { it.isNotBlank() }
                                        ).joinToString(", ")
                                        if (cityState.isNotEmpty()) {
                                            Text(
                                                text = cityState,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    // Fallback to job.propertyAddress if no tenant user info
                                    Text(
                                        text = job.propertyAddress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            // Scheduled Date/Time
                            if (job.scheduledDate != null || ticket.scheduledDate != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Scheduled:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        val scheduledDateStr = job.scheduledDate ?: ticket.scheduledDate
                                        Text(
                                            text = scheduledDateStr ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        job.scheduledTime?.let { time ->
                                            val time12Hour = com.example.mvp.utils.DateUtils.formatTime12Hour(time)
                                            Text(
                                                text = "at $time12Hour",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Message Buttons (for contractors)
            if (userRole == UserRole.CONTRACTOR) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Message Landlord Button
                    if (onMessageLandlord != null) {
                        OutlinedButton(
                            onClick = onMessageLandlord,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Message Landlord",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Message Tenant Button
                    if (onMessageTenant != null) {
                        OutlinedButton(
                            onClick = onMessageTenant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Message Tenant",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            // Schedule and Complete Work Buttons (for contractors)
            if (userRole == UserRole.CONTRACTOR && job.status != "completed") {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Schedule Button
                    val isScheduled = job.scheduledDate != null && job.scheduledTime != null
                    OutlinedButton(
                        onClick = { onSchedule(job.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isScheduled
                    ) {
                        Text(
                            text = if (isScheduled) "Scheduled" else "Schedule",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Complete Work Button
                    Button(
                        onClick = { onComplete(job.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isScheduled
                    ) {
                        Text(
                            text = "Complete Work",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}




