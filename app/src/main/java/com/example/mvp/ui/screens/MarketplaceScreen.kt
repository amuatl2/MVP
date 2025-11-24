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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    contractors: List<Contractor>,
    tickets: List<Ticket> = emptyList(),
    onContractorClick: (String) -> Unit,
    onAssign: (String) -> Unit,
    onApplyToJob: ((String) -> Unit)? = null,
    userRole: com.example.mvp.data.UserRole,
    ticketId: String? = null,
    applications: List<com.example.mvp.data.JobApplication> = emptyList(),
    tenantCity: String? = null,
    tenantState: String? = null,
    contractorUsers: Map<String, com.example.mvp.data.User> = emptyMap()
) {
    var filterCategory by remember { mutableStateOf("") }
    var filterDistance by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showDistanceDropdown by remember { mutableStateOf(false) }

    // For contractors: show available jobs (unassigned tickets)
    // For landlords: show contractors
    val isContractor = userRole == com.example.mvp.data.UserRole.CONTRACTOR
    val availableJobs = if (isContractor) {
        tickets.filter { it.assignedTo == null && it.status == TicketStatus.SUBMITTED }
    } else {
        emptyList()
    }

    val categories = if (isContractor) {
        availableJobs.map { it.category }.distinct().sorted()
    } else {
        contractors.flatMap { it.specialization }.distinct().sorted()
    }

    val filteredContractors = if (!isContractor) {
        val filtered = contractors.filter { contractor ->
            (filterCategory.isEmpty() || contractor.specialization.contains(filterCategory)) &&
            (searchQuery.isEmpty() || contractor.name.contains(searchQuery, ignoreCase = true) || contractor.company.contains(searchQuery, ignoreCase = true))
        }
        
        // Calculate and sort by distance if tenant location is available
        if (tenantCity != null && tenantState != null && ticketId != null) {
            filtered.map { contractor ->
                val contractorUser = contractorUsers[contractor.id]
                val distance = LocationUtils.calculateDistance(
                    tenantCity,
                    tenantState,
                    contractorUser?.city,
                    contractorUser?.state
                )
                contractor to distance
            }.sortedBy { it.second }
                .filter { (_, distance) ->
                    filterDistance.isEmpty() || distance <= (filterDistance.toFloatOrNull() ?: Float.MAX_VALUE)
                }
                .map { it.first }
        } else {
            filtered.filter { contractor ->
                filterDistance.isEmpty() || contractor.distance <= (filterDistance.toFloatOrNull() ?: Float.MAX_VALUE)
            }
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

                            // Distance Filter (only for landlord view)
                            if (!isContractor) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = if (filterDistance.isEmpty()) "Any Distance" else "$filterDistance mi",
                                        onValueChange = { },
                                        label = { Text("Distance") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDistanceDropdown = true },
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { showDistanceDropdown = true }) {
                                                Text("▼", fontSize = 12.sp)
                                            }
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = showDistanceDropdown,
                                        onDismissRequest = { showDistanceDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Any Distance") },
                                            onClick = {
                                                filterDistance = ""
                                                showDistanceDropdown = false
                                            }
                                        )
                                        listOf("5", "10", "20", "50").forEach { dist ->
                                            DropdownMenuItem(
                                                text = { Text("$dist miles") },
                                                onClick = {
                                                    filterDistance = dist
                                                    showDistanceDropdown = false
                                                }
                                            )
                                        }
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
                                ticketId = ticketId
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
    ticketId: String?
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
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format("%.1f mi", contractor.distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
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
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Profile")
                }
                Button(
                    onClick = onAssign,
                    modifier = Modifier.weight(1f),
                    enabled = ticketId != null
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
