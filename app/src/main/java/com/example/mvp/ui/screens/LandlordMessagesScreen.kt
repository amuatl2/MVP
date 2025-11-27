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
fun LandlordMessagesScreen(
    contractorMessages: List<ContractorLandlordMessage>,
    tenantMessages: List<com.example.mvp.data.DirectMessage>,
    contractorUsers: Map<String, User>,
    tenantUsers: Map<String, User>,
    currentUserEmail: String,
    onContractorClick: (String, String?) -> Unit, // contractorEmail, ticketId
    onTenantClick: (String) -> Unit, // tenantEmail
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
        // Get unique contractors and their latest messages
        // Unread count is now based on readBy field, not timestamps
        val contractorConversations = remember(contractorMessages, contractorUsers, currentUserEmail) {
            val normalizedCurrentEmail = currentUserEmail.lowercase()
            // Filter to only show messages where:
            // 1. landlordEmail matches current user (landlord is the recipient), OR
            // 2. senderEmail matches (landlord sent it)
            // This ensures landlords only see messages where they are the sender or intended recipient
            val filteredContractorMessages = contractorMessages.filter { message ->
                message.landlordEmail.lowercase() == normalizedCurrentEmail || 
                message.senderEmail.lowercase() == normalizedCurrentEmail
            }
            
            // Only show conversations where landlord has sent at least one message OR received at least one
            val contractorGroups = filteredContractorMessages.groupBy { it.contractorEmail.lowercase() }
            val conversationsWithLandlordMessages = contractorGroups.filter { (_, messages) ->
                // Include if landlord has sent or received messages in this conversation
                messages.any { 
                    it.landlordEmail.lowercase() == normalizedCurrentEmail || 
                    it.senderEmail.lowercase() == normalizedCurrentEmail 
                }
            }
            
            // Use only messages from conversations where landlord has participated
            conversationsWithLandlordMessages.values.flatten()
            conversationsWithLandlordMessages.keys
                .mapNotNull { contractorEmail ->
                    val messages = conversationsWithLandlordMessages[contractorEmail.lowercase()] ?: emptyList()
                    val latestMessage = messages.maxByOrNull { it.timestamp }
                    val unreadCount = messages
                        .filter { 
                            // Only count messages sent by others that haven't been read by current user
                            it.senderEmail.lowercase() != currentUserEmail.lowercase() &&
                            !it.readBy.contains(currentUserEmail.lowercase())
                        }
                        .size
                    
                    if (latestMessage != null) {
                        ContractorConversation(
                            contractorEmail = contractorEmail,
                            contractorName = contractorUsers[contractorEmail]?.name ?: contractorEmail,
                            latestMessage = latestMessage,
                            unreadCount = unreadCount
                        )
                    } else null
                }
                .sortedByDescending { it.latestMessage.timestamp }
        }
        
        // Get unique tenants and their latest messages
        // IMPORTANT: Show conversations even if landlord sent the first message
        // Unread count is now based on readBy field, not timestamps
        val tenantConversations = remember(tenantMessages, tenantUsers, currentUserEmail) {
            val normalizedCurrentEmail = currentUserEmail.lowercase()
            // Filter to only show messages where:
            // 1. landlordEmail matches current user (landlord is the recipient - old format), OR
            // 2. receiverEmail matches current user (landlord is the recipient - new format), OR
            // 3. senderEmail matches (landlord sent it)
            // This ensures landlords only see messages where they are the sender or intended recipient
            val filteredTenantMessages = tenantMessages.filter { message ->
                message.landlordEmail.lowercase() == normalizedCurrentEmail || 
                message.receiverEmail.lowercase() == normalizedCurrentEmail ||
                message.senderEmail.lowercase() == normalizedCurrentEmail
            }
            
            android.util.Log.d("LandlordMessagesScreen", "Building tenant conversations from ${filteredTenantMessages.size} filtered messages (out of ${tenantMessages.size} total)")
            
            // Group by tenant and show all conversations where there are messages
            filteredTenantMessages
                .groupBy { it.tenantEmail.lowercase() }
                .mapNotNull { (normalizedTenantEmail, messages) ->
                    android.util.Log.d("LandlordMessagesScreen", "Tenant $normalizedTenantEmail has ${messages.size} messages")
                    val latestMessage = messages.maxByOrNull { it.timestamp }
                    val unreadCount = messages
                        .filter { 
                            // Only count messages sent by others that haven't been read by current user
                            it.senderEmail.lowercase() != currentUserEmail.lowercase() &&
                            !it.readBy.contains(currentUserEmail.lowercase())
                        }
                        .size
                    
                    // Show conversation if there's at least one message (regardless of who sent first)
                    if (latestMessage != null) {
                        val tenantUser = tenantUsers.entries.find { it.key.lowercase() == normalizedTenantEmail }?.value
                        android.util.Log.d("LandlordMessagesScreen", "Creating conversation for tenant $normalizedTenantEmail with latest message from ${latestMessage.senderEmail}")
                        TenantConversation(
                            tenantEmail = normalizedTenantEmail,
                            tenantName = tenantUser?.name ?: normalizedTenantEmail.split("@").first().replaceFirstChar { it.uppercase() },
                            latestMessage = latestMessage,
                            unreadCount = unreadCount
                        )
                    } else {
                        android.util.Log.d("LandlordMessagesScreen", "No latest message for tenant $normalizedTenantEmail, skipping conversation")
                        null
                    }
                }
                .sortedByDescending { it.latestMessage.timestamp }
        }
        
        // Combine and sort all conversations
        val allConversations = remember(contractorConversations, tenantConversations) {
            (contractorConversations.map { ConversationItem.Contractor(it) } + 
             tenantConversations.map { ConversationItem.Tenant(it) })
                .sortedByDescending { 
                    when (it) {
                        is ConversationItem.Contractor -> it.conversation.latestMessage.timestamp
                        is ConversationItem.Tenant -> it.conversation.latestMessage.timestamp
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
                        text = "Messages from tenants and contractors will appear here",
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
                        is ConversationItem.Contractor -> {
                            ContractorConversationCard(
                                conversation = item.conversation,
                                onClick = {
                                    // Use the ticketId from the latest message, or "general" if empty
                                    val ticketId = if (item.conversation.latestMessage.ticketId.isNotEmpty() && 
                                                      item.conversation.latestMessage.ticketId != "general") {
                                        item.conversation.latestMessage.ticketId
                                    } else {
                                        null // Will use "general" in navigation
                                    }
                                    onContractorClick(
                                        item.conversation.contractorEmail,
                                        ticketId
                                    )
                                }
                            )
                        }
                        is ConversationItem.Tenant -> {
                            TenantConversationCard(
                                conversation = item.conversation,
                                onClick = {
                                    onTenantClick(item.conversation.tenantEmail)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ContractorConversation(
    val contractorEmail: String,
    val contractorName: String,
    val latestMessage: ContractorLandlordMessage,
    val unreadCount: Int
)

data class TenantConversation(
    val tenantEmail: String,
    val tenantName: String,
    val latestMessage: com.example.mvp.data.DirectMessage,
    val unreadCount: Int
)

sealed class ConversationItem {
    data class Contractor(val conversation: ContractorConversation) : ConversationItem()
    data class Tenant(val conversation: TenantConversation) : ConversationItem()
}

@Composable
fun ContractorConversationCard(
    conversation: ContractorConversation,
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
                        text = conversation.contractorName.split(" ").map { it.first() }.joinToString(""),
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conversation.contractorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal
                        )
                        // Contractor label
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Contractor",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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

@Composable
private fun TenantConversationCard(
    conversation: TenantConversation,
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

