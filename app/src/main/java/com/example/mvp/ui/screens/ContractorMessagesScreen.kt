package com.example.mvp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mvp.data.ContractorLandlordMessage
import com.example.mvp.data.User
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorMessagesScreen(
    messages: List<ContractorLandlordMessage>,
    tenantMessages: List<com.example.mvp.data.DirectMessage> = emptyList(), // Add tenant messages
    landlordUsers: Map<String, User>,
    tenantUsers: Map<String, User> = emptyMap(), // Add tenant users
    currentUserEmail: String,
    onLandlordClick: (String, String?) -> Unit, // landlordEmail, ticketId
    onTenantClick: ((String) -> Unit)? = null, // tenantEmail
    globalLastViewedTimestamps: MutableMap<String, String>? = null // Pass global timestamps
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Messages",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        // Get unique landlords and their latest messages
        // Unread count is now based on readBy field, not timestamps
        val landlordConversations = remember(messages, landlordUsers, currentUserEmail) {
            val normalizedCurrentEmail = currentUserEmail.lowercase()
            // Filter to only show messages where contractorEmail matches current user OR senderEmail matches
            val filteredMessages = messages.filter { message ->
                message.contractorEmail.lowercase() == normalizedCurrentEmail || 
                message.senderEmail.lowercase() == normalizedCurrentEmail
            }
            filteredMessages
                .map { it.landlordEmail.lowercase() }
                .distinct()
                .mapNotNull { landlordEmail ->
                    val landlordMessages = filteredMessages.filter { 
                        it.landlordEmail.lowercase() == landlordEmail 
                    }
                    val latestMessage = landlordMessages.maxByOrNull { it.timestamp }
                    val unreadCount = landlordMessages
                        .filter { 
                            // Only count messages sent by others that haven't been read by current user
                            it.senderEmail.lowercase() != normalizedCurrentEmail.lowercase() &&
                            !it.readBy.contains(normalizedCurrentEmail.lowercase())
                        }
                        .size
                    
                    if (latestMessage != null) {
                        LandlordConversation(
                            landlordEmail = landlordEmail,
                            landlordName = landlordUsers[landlordEmail]?.name 
                                ?: landlordUsers.entries.find { it.key.lowercase() == landlordEmail }?.value?.name
                                ?: landlordEmail,
                            latestMessage = latestMessage,
                            unreadCount = unreadCount
                        )
                    } else null
                }
                .sortedByDescending { it.latestMessage.timestamp }
        }
        
        // Get unique tenants and their latest messages (for contractor-tenant conversations)
        val tenantConversations = remember(tenantMessages, tenantUsers, currentUserEmail) {
            val normalizedCurrentEmail = currentUserEmail.lowercase()
            // Filter to only show messages between contractor and tenant (exclude landlord messages):
            // 1. Contractor is the sender AND tenant is the receiver, OR
            // 2. Tenant is the sender AND contractor is the receiver
            // This ensures contractors only see messages between them and the tenant, not landlord messages
            val filteredTenantMessages = tenantMessages.filter { message ->
                val sender = message.senderEmail.lowercase()
                val receiver = message.receiverEmail.lowercase()
                val landlordEmail = message.landlordEmail.lowercase()
                
                // Exclude messages where landlord is involved (unless contractor is also involved)
                // Message is between contractor and tenant if:
                // - Contractor sent it to tenant (sender == contractor, receiver == tenant)
                // - Tenant sent it to contractor (sender == tenant, receiver == contractor)
                // AND landlord is not the sender or receiver
                val isContractorToTenant = sender == normalizedCurrentEmail && receiver == message.tenantEmail.lowercase()
                val isTenantToContractor = sender == message.tenantEmail.lowercase() && receiver == normalizedCurrentEmail
                
                // Only include if it's a contractor-tenant message (not landlord-tenant)
                (isContractorToTenant || isTenantToContractor) && 
                landlordEmail != sender && landlordEmail != receiver
            }
            
            // Group by tenant and only include conversations where contractor has sent at least one message
            // OR where there are messages from the tenant (indicating an active conversation)
            val tenantGroups = filteredTenantMessages.groupBy { it.tenantEmail.lowercase() }
            val conversationsWithContractorMessages = tenantGroups.filter { (_, messages) ->
                // Include if contractor has sent at least one message to this tenant
                messages.any { it.senderEmail.lowercase() == normalizedCurrentEmail }
            }
            
            // Build conversation list from conversations where contractor has participated
            conversationsWithContractorMessages.keys
                .mapNotNull { normalizedTenantEmail ->
                    val messagesForTenant = conversationsWithContractorMessages[normalizedTenantEmail] ?: emptyList()
                    val latestMessage = messagesForTenant.maxByOrNull { it.timestamp }
                    val unreadCount = messagesForTenant
                        .filter { 
                            // Only count messages sent by others that haven't been read by current user
                            it.senderEmail.lowercase() != normalizedCurrentEmail.lowercase() &&
                            !it.readBy.contains(normalizedCurrentEmail.lowercase())
                        }
                        .size
                    
                    if (latestMessage != null) {
                        val tenantUser = tenantUsers.entries.find { it.key.lowercase() == normalizedTenantEmail }?.value
                        TenantConversationForContractor(
                            tenantEmail = normalizedTenantEmail,
                            tenantName = tenantUser?.name ?: normalizedTenantEmail.split("@").first().replaceFirstChar { it.uppercase() },
                            latestMessage = latestMessage,
                            unreadCount = unreadCount
                        )
                    } else null
                }
                .sortedByDescending { it.latestMessage.timestamp }
        }
        
        // Combine landlord and tenant conversations
        val allConversations = remember(landlordConversations, tenantConversations) {
            (landlordConversations.map { ContractorConversationItem.Landlord(it) } + 
             tenantConversations.map { ContractorConversationItem.Tenant(it) })
                .sortedByDescending { item ->
                    when (item) {
                        is ContractorConversationItem.Landlord -> item.conversation.latestMessage.timestamp
                        is ContractorConversationItem.Tenant -> item.conversation.latestMessage.timestamp
                    }
                }
        }
        
        if (allConversations.isEmpty()) {
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
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Messages from landlords and tenants will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allConversations) { item ->
                    when (item) {
                        is ContractorConversationItem.Landlord -> {
                            LandlordConversationCard(
                                conversation = item.conversation,
                                onClick = {
                                    // Use the ticketId from the latest message, or "general" if empty
                                    val ticketId = if (item.conversation.latestMessage.ticketId.isNotEmpty() && 
                                                      item.conversation.latestMessage.ticketId != "general") {
                                        item.conversation.latestMessage.ticketId
                                    } else {
                                        null // Will use "general" in navigation
                                    }
                                    onLandlordClick(
                                        item.conversation.landlordEmail,
                                        ticketId
                                    )
                                }
                            )
                        }
                        is ContractorConversationItem.Tenant -> {
                            TenantConversationCard(
                                conversation = item.conversation,
                                onClick = {
                                    onTenantClick?.invoke(item.conversation.tenantEmail)
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
private fun formatTimestamp(timestamp: String): String {
    return try {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = dateTimeFormat.parse(timestamp)
        if (date != null) {
            val now = Date()
            val diff = now.time - date.time
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes m ago"
                hours < 24 -> "$hours h ago"
                days < 7 -> "$days d ago"
                else -> {
                    val displayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    displayFormat.format(date)
                }
            }
        } else {
            timestamp
        }
    } catch (e: Exception) {
        timestamp
    }
}

private data class LandlordConversation(
    val landlordEmail: String,
    val landlordName: String,
    val latestMessage: ContractorLandlordMessage,
    val unreadCount: Int
)

private data class TenantConversationForContractor(
    val tenantEmail: String,
    val tenantName: String,
    val latestMessage: com.example.mvp.data.DirectMessage,
    val unreadCount: Int
)

private sealed class ContractorConversationItem {
    data class Landlord(val conversation: LandlordConversation) : ContractorConversationItem()
    data class Tenant(val conversation: TenantConversationForContractor) : ContractorConversationItem()
}

@Composable
private fun LandlordConversationCard(
    conversation: LandlordConversation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.landlordName.split(" ").map { it.first() }.joinToString(""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.landlordName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal
                    )
                }
                
                Text(
                    text = conversation.latestMessage.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
                
                Text(
                    text = formatTimestamp(conversation.latestMessage.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TenantConversationCard(
    conversation: TenantConversationForContractor,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.tenantName.split(" ").map { it.first() }.joinToString(""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conversation.tenantName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal
                        )
                        // Tenant label
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Tenant",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                Text(
                    text = conversation.latestMessage.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
                
                Text(
                    text = formatTimestamp(conversation.latestMessage.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
