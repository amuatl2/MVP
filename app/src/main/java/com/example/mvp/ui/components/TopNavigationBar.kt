package com.example.mvp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvp.data.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar(
    currentRoute: String?,
    userRole: UserRole?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    unreadMessageCount: Int = 0 // Total unread message count
) {
    if (userRole == null) return

    data class NavItem(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val showBadge: Boolean = false)
    
    val navItems = when (userRole) {
        UserRole.TENANT -> listOf(
            NavItem("Dashboard", "dashboard", Icons.Default.Home),
            NavItem("Ticket", "create_ticket", Icons.Default.Add),
            NavItem("Landlord", "tenant_landlord", Icons.Default.Person),
            NavItem("History", "history", Icons.Default.Info)
        )
        UserRole.LANDLORD -> listOf(
            NavItem("Dashboard", "dashboard", Icons.Default.Home),
            NavItem("Tenants", "landlord_tenants", Icons.Default.Person),
            NavItem("Marketplace", "marketplace", Icons.Default.Person),
            NavItem("Messages", "landlord_messages", Icons.Default.Email, showBadge = false),
            NavItem("History", "history", Icons.Default.Info)
        )
        UserRole.CONTRACTOR -> listOf(
            NavItem("Jobs", "contractor_dashboard", Icons.Default.Build),
            NavItem("Schedule", "schedule", Icons.Default.DateRange),
            NavItem("Messages", "contractor_messages", Icons.Default.Email, showBadge = false),
            NavItem("History", "history", Icons.Default.Info)
        )
    }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "HOME",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "HOME",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            HorizontalScrollableRow(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                navItems.forEach { navItem ->
                    TopNavButton(
                        label = navItem.label,
                        icon = navItem.icon,
                        currentRoute = currentRoute,
                        route = navItem.route,
                        onNavigate = onNavigate,
                        badgeCount = if (navItem.showBadge && unreadMessageCount > 0) unreadMessageCount else null
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            // Logout Button - Always visible, icon only on mobile for space
            IconButton(
                onClick = onLogout,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun TopNavButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentRoute: String?,
    route: String,
    onNavigate: (String) -> Unit,
    badgeCount: Int? = null
) {
    val isSelected = currentRoute?.contains(route) == true
    
    // Use smaller text on mobile to prevent wrapping
    Surface(
        onClick = { onNavigate(route) },
        shape = MaterialTheme.shapes.small,
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
            // Badge in top right
            if (badgeCount != null && badgeCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
