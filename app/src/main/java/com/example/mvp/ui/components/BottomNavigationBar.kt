package com.example.mvp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            NavItem("tenant_messages", "Messages", Icons.Default.Email),
            NavItem("tenant_review", "Review", Icons.Default.Star),
            NavItem("history", "History", Icons.Default.Info),
            NavItem("chat", "AI Chat", Icons.Default.Notifications)
        )
        UserRole.LANDLORD -> listOf(
            NavItem("dashboard", "Dashboard", Icons.Default.Home),
            NavItem("landlord_tenants", "Tenants", Icons.Default.Person),
            NavItem("marketplace", "Marketplace", Icons.Default.Person),
            NavItem("landlord_messages", "Messages", Icons.Default.Email),
            NavItem("history", "History", Icons.Default.Info),
            NavItem("chat", "AI Chat", Icons.Default.Notifications)
        )
        UserRole.CONTRACTOR -> listOf(
            NavItem("contractor_dashboard", "Jobs", Icons.Default.Build),
            NavItem("schedule", "Schedule", Icons.Default.DateRange),
            NavItem("contractor_messages", "Messages", Icons.Default.Email),
            NavItem("history", "History", Icons.Default.Info),
            NavItem("chat", "AI Chat", Icons.Default.Notifications)
        )
    }

    NavigationBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        navItems.forEach { item ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        item.icon, 
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                label = { 
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                selected = currentRoute?.contains(item.route) == true,
                onClick = { onNavigate(item.route) },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

