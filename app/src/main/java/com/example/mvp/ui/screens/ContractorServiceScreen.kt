package com.example.mvp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvp.data.LocationData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorServiceScreen(
    currentSpecialization: List<String>,
    currentServiceAreas: Map<String, List<String>>,
    onSave: (List<String>, Map<String, List<String>>) -> Unit,
    onBack: () -> Unit
) {
    var specialization by remember { mutableStateOf(currentSpecialization.toMutableList()) }
    var showWorkTypeDropdown by remember { mutableStateOf(false) }
    val availableWorkTypes = com.example.mvp.data.JobTypes.ALL_TYPES
    var selectedStates by remember { mutableStateOf(currentServiceAreas.keys.toSet()) }
    var citySearchQuery by remember { mutableStateOf("") }
    var serviceAreas by remember { 
        mutableStateOf(
            currentServiceAreas.mapValues { it.value.toMutableList() }.toMutableMap()
        )
    }
    
    // Helper function to update service areas properly
    fun updateServiceAreas(update: (MutableMap<String, MutableList<String>>) -> Unit) {
        val newMap = serviceAreas.mapValues { it.value.toMutableList() }.toMutableMap()
        update(newMap)
        serviceAreas = newMap
    }
    
    // Get all cities from selected states, filtered by search query
    val availableCities = remember(selectedStates, citySearchQuery) {
        val allCities = mutableListOf<Pair<String, String>>() // Pair of (city, state)
        selectedStates.forEach { state ->
            LocationData.getCitiesForState(state).forEach { city ->
                allCities.add(city to state)
            }
        }
        
        if (citySearchQuery.isBlank()) {
            allCities
        } else {
            val query = citySearchQuery.lowercase()
            allCities.filter { (city, _) ->
                city.lowercase().contains(query)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Areas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Work Types Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Types of Work",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Add Work Type - Dropdown Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            label = { Text("Add work type") },
                            placeholder = { Text("Select work type") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWorkTypeDropdown = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showWorkTypeDropdown = true }) {
                                    Text("▼", fontSize = 12.sp)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showWorkTypeDropdown,
                            onDismissRequest = { showWorkTypeDropdown = false }
                        ) {
                            availableWorkTypes.forEach { workType ->
                                DropdownMenuItem(
                                    text = { Text(workType) },
                                    onClick = {
                                        if (!specialization.contains(workType)) {
                                            specialization = (specialization + workType).toMutableList()
                                        }
                                        showWorkTypeDropdown = false
                                    },
                                    enabled = !specialization.contains(workType)
                                )
                            }
                        }
                    }
                    
                    // Work Type Chips
                    if (specialization.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(specialization) { workType ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.clickable { }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = workType,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        IconButton(
                                            onClick = {
                                                specialization = specialization.filter { it != workType }.toMutableList()
                                            },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No work types added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // Service Areas Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Service Areas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Select states and cities where you provide services",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    // State Selection - Multi-select with checkboxes
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Select States",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            LazyColumn(
                                modifier = Modifier.height(200.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(LocationData.states) { state ->
                                    val isSelected = state in selectedStates
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedStates = if (checked) {
                                                    selectedStates + state
                                                } else {
                                                    selectedStates - state
                                                }
                                            }
                                        )
                                        Text(
                                            text = state,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // City Search Bar
                    if (selectedStates.isNotEmpty()) {
                        OutlinedTextField(
                            value = citySearchQuery,
                            onValueChange = { citySearchQuery = it },
                            label = { Text("Search Cities") },
                            placeholder = { Text("Type to search cities...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    // City Selection - Show all cities from selected states with checkboxes
                    if (selectedStates.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Select Cities",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                if (availableCities.isEmpty()) {
                                    Text(
                                        text = if (citySearchQuery.isBlank()) {
                                            "Select at least one state to see cities"
                                        } else {
                                            "No cities found matching \"$citySearchQuery\""
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                } else {
                                    // Cities list with checkboxes
                                    LazyColumn(
                                        modifier = Modifier.height(300.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(availableCities) { (city, state) ->
                                            val isSelected = city in (serviceAreas[state] ?: emptyList())
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        updateServiceAreas { map ->
                                                            val cities = map[state]?.toMutableList() ?: mutableListOf()
                                                            if (checked) {
                                                                if (!cities.contains(city)) {
                                                                    cities.add(city)
                                                                }
                                                                map[state] = cities
                                                            } else {
                                                                cities.remove(city)
                                                                if (cities.isEmpty()) {
                                                                    map.remove(state)
                                                                } else {
                                                                    map[state] = cities
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = city,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = state,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Service Area Chips
                    if (serviceAreas.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            serviceAreas.forEach { (state, cities) ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = state,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(cities) { city ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.clickable { }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = city,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            updateServiceAreas { map ->
                                                                val cityList = map[state]?.toMutableList() ?: mutableListOf()
                                                                cityList.remove(city)
                                                                if (cityList.isEmpty()) {
                                                                    map.remove(state)
                                                                } else {
                                                                    map[state] = cityList
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No service areas added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // Save Button
            Button(
                onClick = {
                    onSave(specialization, serviceAreas)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

