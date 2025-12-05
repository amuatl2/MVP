package com.example.mvp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvp.data.Contractor
import com.example.mvp.data.JobInvitation
import com.example.mvp.data.Ticket
import com.example.mvp.data.User

/**
 * Normalizes category names to handle plural/singular variations
 * Examples: "Appliance" <-> "Appliances", "General Repair" <-> "General Repairs"
 */
private fun normalizeCategory(category: String): String {
    return category.trim().lowercase()
}

/**
 * Checks if two categories match, handling plural/singular variations
 */
private fun categoriesMatch(category1: String, category2: String): Boolean {
    val normalized1 = normalizeCategory(category1)
    val normalized2 = normalizeCategory(category2)
    
    // Exact match
    if (normalized1 == normalized2) return true
    
    // Check if one is the plural/singular of the other
    // Handle common plural/singular patterns
    val singular1 = if (normalized1.endsWith("s") && normalized1.length > 1) {
        normalized1.dropLast(1)
    } else {
        normalized1
    }
    val singular2 = if (normalized2.endsWith("s") && normalized2.length > 1) {
        normalized2.dropLast(1)
    } else {
        normalized2
    }
    
    // Match if singular forms are the same
    if (singular1 == singular2) return true
    
    // Also check if one contains the other (for multi-word categories)
    return normalized1.contains(normalized2) || normalized2.contains(normalized1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignContractorScreen(
    ticket: Ticket,
    contractors: List<Contractor>,
    invitations: List<JobInvitation>,
    tenantUser: User?,
    contractorUsers: Map<String, User>,
    onBack: () -> Unit,
    onContractorInfo: (String) -> Unit,
    onApply: (String) -> Unit
) {
    // Filter contractors by:
    // 1. Serve the ticket creator's (tenant's) city
    // 2. Can handle the ticket category
    // 3. Ranked by rating
    val filteredAndRankedContractors = remember(contractors, ticket, tenantUser, contractorUsers) {
        android.util.Log.d("AssignContractorScreen", "Filtering contractors for ticket: ${ticket.id}, category: ${ticket.category}")
        android.util.Log.d("AssignContractorScreen", "Tenant user: ${tenantUser?.name}, city: ${tenantUser?.city}, state: ${tenantUser?.state}")
        android.util.Log.d("AssignContractorScreen", "Total contractors before filtering: ${contractors.size}")
        
        val filtered = contractors.filter { contractor ->
            // Check if contractor serves the ticket creator's (tenant's) city (case-insensitive)
            val servesCity = tenantUser?.city?.let { city ->
                tenantUser.state?.let { state ->
                    val normalizedState = state.trim()
                    val normalizedCity = city.trim()
                    val serviceCities = contractor.serviceAreas[normalizedState] ?: emptyList()
                    val serves = serviceCities.any { 
                        it.trim().equals(normalizedCity, ignoreCase = true) 
                    }
                    android.util.Log.d("AssignContractorScreen", "Contractor ${contractor.name}: serves $normalizedCity, $normalizedState? $serves (service areas: ${contractor.serviceAreas})")
                    serves
                } ?: false
            } ?: false
            
            // Check if contractor can handle the ticket category (handles plural/singular variations)
            val canHandleCategory = contractor.specialization.any { specialization ->
                categoriesMatch(specialization, ticket.category)
            }
            android.util.Log.d("AssignContractorScreen", "Contractor ${contractor.name}: can handle ${ticket.category}? $canHandleCategory (specializations: ${contractor.specialization})")
            
            val matches = servesCity && canHandleCategory
            android.util.Log.d("AssignContractorScreen", "Contractor ${contractor.name}: matches criteria? $matches")
            matches
        }.sortedByDescending { it.rating }
        
        android.util.Log.d("AssignContractorScreen", "Total contractors after filtering: ${filtered.size}")
        filtered
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "Assign Contractor",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ticket.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (filteredAndRankedContractors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No contractors available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "No contractors serve ${tenantUser?.city ?: "this city"} and can handle ${ticket.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAndRankedContractors) { contractor ->
                    ContractorCardForAssignment(
                        contractor = contractor,
                        ticketId = ticket.id,
                        invitation = invitations.find { 
                            it.contractorId == contractor.id && it.ticketId == ticket.id 
                        },
                        onInfo = { onContractorInfo(contractor.id) },
                        onApply = { onApply(contractor.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContractorCardForAssignment(
    contractor: Contractor,
    ticketId: String,
    invitation: JobInvitation?,
    onInfo: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with name and company
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contractor.name.split(" ").map { it.first() }.joinToString(""),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = contractor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = contractor.company,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Statistics
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = String.format("%.1f", contractor.rating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${contractor.completedJobs} jobs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Specializations
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                contractor.specialization.forEach { spec ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = spec,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onInfo,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Info")
                }
                
                // Determine button state based on invitation status
                val buttonText = when (invitation?.status) {
                    com.example.mvp.data.InvitationStatus.PENDING -> "Applied"
                    com.example.mvp.data.InvitationStatus.ACCEPTED -> "Accepted"
                    com.example.mvp.data.InvitationStatus.DECLINED -> "Denied"
                    null -> "Apply"
                }
                
                val isButtonEnabled = invitation == null
                val buttonColors = when (invitation?.status) {
                    com.example.mvp.data.InvitationStatus.PENDING -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    com.example.mvp.data.InvitationStatus.DECLINED -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                    else -> ButtonDefaults.buttonColors()
                }
                
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    enabled = isButtonEnabled,
                    colors = buttonColors
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}




