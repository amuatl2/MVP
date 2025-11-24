package com.example.mvp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mvp.data.UserRole

@Composable
fun HomeBottomNavigation(
    currentRoute: String?,
    userRole: UserRole?,
    onNavigate: (String) -> Unit
) {
    if (userRole == null) return

    val navItems = when (userRole) {
        UserRole.TENANT -> listOf(
            NavItem("dashboard", "Dashboard", Icons.Default.Home),
            NavItem("create_ticket", "Ticket", Icons.Default.Add),
            NavItem("tenant_landlord", "Landlord", Icons.Default.Person),
            NavItem("history", "History", Icons.Default.Info)
        )
        UserRole.LANDLORD -> listOf(
            NavItem("dashboard", "Dashboard", Icons.Default.Home),
            NavItem("landlord_tenants", "Tenants", Icons.Default.Person),
            NavItem("marketplace", "Marketplace", Icons.Default.Person),
            NavItem("history", "History", Icons.Default.Info)
        )
        UserRole.CONTRACTOR -> listOf(
            NavItem("contractor_dashboard", "Jobs", Icons.Default.Build),
            NavItem("schedule", "Schedule", Icons.Default.DateRange),
            NavItem("history", "History", Icons.Default.Info)
        )
    }

    NavigationBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        navItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                selected = currentRoute?.contains(item.route) == true,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

