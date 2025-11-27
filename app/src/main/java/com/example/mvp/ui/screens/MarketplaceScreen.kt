package com.example.mvp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.example.mvp.data.Ticket
import com.example.mvp.data.TicketStatus
import com.example.mvp.utils.LocationUtils

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
fun MarketplaceScreen(
    contractors: List<Contractor>,
    tickets: List<Ticket> = emptyList(),
    onContractorClick: (String) -> Unit,
    onAssign: (String) -> Unit,
    onMessage: ((String, String) -> Unit)? = null, // ticketId, contractorId
    onApplyToJob: ((String) -> Unit)? = null,
    userRole: com.example.mvp.data.UserRole,
    ticketId: String? = null,
    applications: List<com.example.mvp.data.JobApplication> = emptyList(),
    invitations: List<com.example.mvp.data.JobInvitation> = emptyList(),
    tenantCity: String? = null,
    tenantState: String? = null,
    contractorUsers: Map<String, com.example.mvp.data.User> = emptyMap(),
    tenantUsers: Map<String, com.example.mvp.data.User> = emptyMap()
) {
    var filterCategory by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // For contractors: show available jobs (unassigned tickets)
    // For landlords: show contractors
    val isContractor = userRole == com.example.mvp.data.UserRole.CONTRACTOR
    val availableJobs = if (isContractor) {
        tickets.filter { it.assignedTo == null && it.status == TicketStatus.SUBMITTED }
    } else {
        emptyList()
    }

    val categories = com.example.mvp.data.JobTypes.ALL_TYPES

    val filteredContractors = if (!isContractor) {
        // Get the ticket if ticketId is provided to filter by its category
        val ticketForFiltering = ticketId?.let { id -> tickets.find { it.id == id } }
        
        val filtered = contractors.filter { contractor ->
            // Filter by category: if ticketId is provided, use ticket's category; otherwise use filterCategory
            val categoryToFilter = if (ticketForFiltering != null) {
                ticketForFiltering.category
            } else {
                filterCategory
            }
            
            (categoryToFilter.isEmpty() || contractor.specialization.any { specialization ->
                categoriesMatch(specialization, categoryToFilter)
            }) &&
            (searchQuery.isEmpty() || contractor.name.contains(searchQuery, ignoreCase = true) || contractor.company.contains(searchQuery, ignoreCase = true))
        }
        
        // Filter by service areas - contractor must serve at least one city/state where landlord has tenants
        val locationFiltered = if (userRole == com.example.mvp.data.UserRole.LANDLORD) {
            // For landlords: only show contractors who serve at least one city/state where they have tenants
            if (tenantUsers.isNotEmpty()) {
                val tenantLocations = tenantUsers.values.mapNotNull { user ->
                    if (user.city != null && user.state != null) {
                        user.state to user.city
                    } else null
                }.toSet()
                
                // Only show contractors if we have tenant locations and they serve at least one
                if (tenantLocations.isNotEmpty()) {
                    filtered.filter { contractor ->
                        tenantLocations.any { (state, city) ->
                            val serviceCities = contractor.serviceAreas[state] ?: emptyList()
                            serviceCities.any { it.equals(city, ignoreCase = true) } && serviceCities.isNotEmpty()
                        } && contractor.serviceAreas.isNotEmpty() && 
                        contractor.serviceAreas.values.any { it.isNotEmpty() }
                    }
                } else {
                    // No tenant locations available, don't show any contractors
                    emptyList()
                }
            } else {
                // If no tenants, don't show any contractors
                emptyList()
            }
        } else if (ticketForFiltering != null && ticketId != null) {
            // For specific ticket: filter by ticket creator's city and state
            // Get tenant user from tenantUsers map using ticket's submittedBy email
            val ticketCreator = tenantUsers[ticketForFiltering.submittedBy]
            val creatorCity = ticketCreator?.city
            val creatorState = ticketCreator?.state
            
            if (creatorCity != null && creatorState != null) {
                // Filter by ticket creator's location
                filtered.filter { contractor ->
                    val serviceCities = contractor.serviceAreas[creatorState] ?: emptyList()
                    serviceCities.any { it.equals(creatorCity, ignoreCase = true) } && serviceCities.isNotEmpty()
                }
            } else {
                // If ticket creator location not available, only filter by category
                filtered
            }
        } else if (tenantCity != null && tenantState != null) {
            // For specific ticket: check if contractor serves this city/state (case-insensitive)
            filtered.filter { contractor ->
                val serviceCities = contractor.serviceAreas[tenantState] ?: emptyList()
                serviceCities.any { it.equals(tenantCity, ignoreCase = true) } && serviceCities.isNotEmpty()
            }
        } else {
            // Default: only show contractors who have at least one service area
            filtered.filter { contractor ->
                contractor.serviceAreas.isNotEmpty() && 
                contractor.serviceAreas.values.any { it.isNotEmpty() }
            }
        }
        
        // Sort by proximity if tenant location is available (but don't show distance)
        if (tenantCity != null && tenantState != null && ticketId != null) {
            locationFiltered.map { contractor ->
                val contractorUser = contractorUsers[contractor.id]
                val distance = LocationUtils.calculateDistance(
                    tenantCity,
                    tenantState,
                    contractorUser?.city,
                    contractorUser?.state
                )
                contractor to distance
            }.sortedBy { it.second }
                .map { it.first }
        } else {
            locationFiltered
        }
    } else {
        emptyList()
    }

    val filteredJobs = if (isContractor) {
        availableJobs.filter { job ->
            (filterCategory.isEmpty() || job.category == filterCategory) &&
            (searchQuery.isEmpty() || job.title.contains(searchQuery, ignoreCase = true) || job.description.contains(searchQuery, ignoreCase = true))
        }
    } else {
        emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isContractor) "Contractor Marketplace" else "Contractor Marketplace",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    text = if (isContractor) "Contractor Marketplace" else "Contractor Marketplace",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isContractor) "Browse available jobs and grow your business" else "Browse and select contractors",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Search and Filter Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(if (isContractor) "Search jobs..." else "Search contractors...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            placeholder = { Text(if (isContractor) "Search by title or description..." else "Search by name or company...") }
                        )

                        // Filters Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Category Filter
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = if (filterCategory.isEmpty()) "All Categories" else filterCategory,
                                    onValueChange = { },
                                    label = { Text("Category") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCategoryDropdown = true },
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { showCategoryDropdown = true }) {
                                            Text("▼", fontSize = 12.sp)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = showCategoryDropdown,
                                    onDismissRequest = { showCategoryDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Categories") },
                                        onClick = {
                                            filterCategory = ""
                                            showCategoryDropdown = false
                                        }
                                    )
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                filterCategory = cat
                                                showCategoryDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }

            // Content Grid
            if (isContractor) {
                // Show available jobs for contractors
                if (filteredJobs.isEmpty()) {
                    item {
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
                                    .padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("💼", fontSize = 64.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No available jobs match your filters",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredJobs) { ticket ->
                        JobCardForContractor(
                            ticket = ticket,
                            onViewProfile = { onApplyToJob?.invoke(ticket.id) },
                            onApply = { onApplyToJob?.invoke(ticket.id) }
                        )
                    }
                }
            } else {
                // Show applicants first if viewing a specific ticket
                if (ticketId != null && applications.isNotEmpty()) {
                    item {
                        Text(
                            text = "Applicants (${applications.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(applications) { application ->
                        val contractor = contractors.find { it.id == application.contractorId }
                        if (contractor != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = application.contractorName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = application.contractorEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "Applied: ${application.appliedAt.split(" ").firstOrNull() ?: application.appliedAt}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Button(
                                        onClick = { onAssign(application.contractorId) }
                                    ) {
                                        Text("Assign")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Contractors",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Show contractors for landlords
                if (filteredContractors.isEmpty()) {
                    item {
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
                                    .padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👷", fontSize = 64.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No contractors match your filters",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredContractors) { contractor ->
                        // Don't show contractors who already applied
                        val hasApplied = applications.any { it.contractorId == contractor.id }
                        if (!hasApplied) {
                            ContractorCardForLandlord(
                                contractor = contractor,
                                onViewProfile = { onContractorClick(contractor.id) },
                                onAssign = { onAssign(contractor.id) },
                                ticketId = ticketId,
                                tenantUsers = tenantUsers,
                                onMessage = onMessage?.let { callback ->
                                    { callback(ticketId ?: "", contractor.id) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobCardForContractor(
    ticket: Ticket,
    onViewProfile: () -> Unit,
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
            // Header with initials circle and info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
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
                                text = ticket.title.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = ticket.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Maintenance Request",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = ticket.category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Description
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Profile")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun ContractorCardForLandlord(
    contractor: Contractor,
    onViewProfile: () -> Unit,
    onAssign: () -> Unit,
    onMessage: (() -> Unit)? = null,
    ticketId: String?,
    invitation: com.example.mvp.data.JobInvitation? = null,
    tenantUsers: Map<String, com.example.mvp.data.User> = emptyMap()
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
            // Header with initials circle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
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
                if (contractor.preferred) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Preferred",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
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

            // Serves section - show cities where landlord has tenants
            val landlordTenantCities = remember(tenantUsers, contractor.serviceAreas) {
                tenantUsers.values
                    .mapNotNull { tenant -> tenant.city?.let { city -> tenant.state?.let { state -> city to state } } }
                    .distinct()
                    .filter { (city, state) ->
                        contractor.serviceAreas[state]?.contains(city) == true
                    }
                    .map { it.first }
                    .sorted()
            }
            
            if (landlordTenantCities.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Serves:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        landlordTenantCities.forEach { city ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = city,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Info")
                }
                
                // Message button (always enabled)
                Button(
                    onClick = { 
                        if (onMessage != null) {
                            onMessage()
                        } else {
                            onAssign()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = onMessage != null || ticketId != null
                ) {
                    Text("Message")
                }
            }
        }
    }
}
