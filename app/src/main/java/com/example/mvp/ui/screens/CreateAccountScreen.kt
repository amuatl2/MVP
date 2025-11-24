package com.example.mvp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvp.data.UserRole
import com.example.mvp.data.LocationData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    onBack: () -> Unit,
    onCreateAccount: (String, String, String, UserRole, String?, String?, String?, String?) -> Unit,
    authError: String? = null
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.TENANT) }
    var accountCreated by remember { mutableStateOf(false) }
    
    // Location fields
    var address by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var showStateDropdown by remember { mutableStateOf(false) }
    var showCityDropdown by remember { mutableStateOf(false) }
    
    // Update cities when state changes
    val availableCities = remember(selectedState) {
        if (selectedState.isNotEmpty()) {
            LocationData.getCitiesForState(selectedState)
        } else {
            emptyList()
        }
    }
    
    // Reset city when state changes
    LaunchedEffect(selectedState) {
        if (selectedCity.isNotEmpty() && !availableCities.contains(selectedCity)) {
            selectedCity = ""
        }
    }

    if (accountCreated) {
        SuccessScreen(
            message = "Account Created!",
            subtitle = "You can now login with your credentials.",
            onBack = {
                onBack()
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏠", fontSize = 48.sp)
                }
            }

            Text(
                text = "Join HOME",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create your account to get started",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("John Doe") },
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email *") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("your@example.com") },
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password *") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("••••••••") },
                singleLine = true,
                supportingText = {
                    if (password.isNotEmpty() && password.length < 6) {
                        Text(
                            text = "Password must be at least 6 characters",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password *") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("••••••••") },
                singleLine = true,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text(
                            text = "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select Your Role *",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            UserRole.values().forEach { role ->
                Card(
                    onClick = { selectedRole = role },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRole == role)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (selectedRole == role)
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else
                        null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = role.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (role) {
                                    UserRole.TENANT -> "Report and track maintenance issues"
                                    UserRole.LANDLORD -> "Manage properties and contractors"
                                    UserRole.CONTRACTOR -> "Find and complete jobs"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Role-specific fields
            if (selectedRole == UserRole.CONTRACTOR) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ABC Contractors") },
                    singleLine = true
                )
            }
            
            if (selectedRole == UserRole.TENANT || selectedRole == UserRole.CONTRACTOR) {
                if (selectedRole == UserRole.TENANT) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address *") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("123 Main St, Apt 4B") },
                        singleLine = true
                    )
                }
                
                // State Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedState.isEmpty()) "Select State *" else selectedState,
                        onValueChange = { },
                        label = { Text("State *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStateDropdown = true },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showStateDropdown = true }) {
                                Text("▼", fontSize = 12.sp)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showStateDropdown,
                        onDismissRequest = { showStateDropdown = false }
                    ) {
                        LocationData.states.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state) },
                                onClick = {
                                    selectedState = state
                                    selectedCity = ""
                                    showStateDropdown = false
                                }
                            )
                        }
                    }
                }
                
                // City Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedCity.isEmpty()) "Select City *" else selectedCity,
                        onValueChange = { },
                        label = { Text("City *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (selectedState.isNotEmpty()) {
                                    showCityDropdown = true
                                }
                            },
                        readOnly = true,
                        enabled = selectedState.isNotEmpty(),
                        placeholder = { Text(if (selectedState.isEmpty()) "Select state first" else "Select City") },
                        trailingIcon = {
                            IconButton(
                                onClick = { 
                                    if (selectedState.isNotEmpty()) {
                                        showCityDropdown = true
                                    }
                                },
                                enabled = selectedState.isNotEmpty()
                            ) {
                                Text("▼", fontSize = 12.sp)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showCityDropdown,
                        onDismissRequest = { showCityDropdown = false }
                    ) {
                        availableCities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    selectedCity = city
                                    showCityDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val canCreate = name.isNotBlank() && 
                           email.isNotBlank() && 
                           password.isNotBlank() && 
                           password == confirmPassword &&
                           password.length >= 6 &&
                           (selectedRole != UserRole.CONTRACTOR || companyName.isNotBlank()) &&
                           (selectedRole != UserRole.TENANT || (address.isNotBlank() && selectedState.isNotBlank() && selectedCity.isNotBlank())) &&
                           (selectedRole != UserRole.CONTRACTOR || (selectedState.isNotBlank() && selectedCity.isNotBlank()))

            Button(
                onClick = {
                    if (canCreate) {
                        onCreateAccount(
                            name, 
                            email, 
                            password, 
                            selectedRole,
                            if (selectedRole == UserRole.TENANT) address else null,
                            if (selectedRole == UserRole.TENANT || selectedRole == UserRole.CONTRACTOR) selectedCity else null,
                            if (selectedRole == UserRole.TENANT || selectedRole == UserRole.CONTRACTOR) selectedState else null,
                            if (selectedRole == UserRole.CONTRACTOR) companyName else null
                        )
                        accountCreated = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canCreate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (authError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
