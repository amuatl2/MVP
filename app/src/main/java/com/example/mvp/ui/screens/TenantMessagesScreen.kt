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
import com.example.mvp.data.DirectMessage
import com.example.mvp.data.User
import com.example.mvp.utils.DateUtils

// Data class to represent a conversation with another party (for tenant's messages screen)
private data class TenantMessageConversation(
    val otherPartyEmail: String,
    val otherPartyName: String,
    val otherPartyRole: com.example.mvp.data.UserRole,
    val latestMessage: DirectMessage,
    val unreadCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantMessagesScreen(
    messages: List<DirectMessage>,
    landlordUser: User?,
    contractorUsers: Map<String, User> = emptyMap(),
    currentUserEmail: String,
    onLandlordClick: () -> Unit,
    onContractorClick: ((String) -> Unit)? = null // contractorEmail
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        val normalizedCurrentEmail = currentUserEmail.lowercase()
        
        // Group messages by the other party (landlord or contractor)
        // For landlord: otherPartyEmail = landlordEmail
        // For contractor: otherPartyEmail = senderEmail (when sender is contractor)
        val conversations = remember(messages, landlordUser, contractorUsers, normalizedCurrentEmail) {
            val conversationMap = mutableMapOf<String, MutableList<DirectMessage>>()
            
            // Filter messages to only include those where the current tenant is involved
            val filteredMessages = messages.filter { message ->
                // Only show messages where tenantEmail matches the current user's email
                message.tenantEmail.lowercase() == normalizedCurrentEmail
            }
            
            filteredMessages.forEach { message ->
                // Determine the other party
                // The landlordEmail field in DirectMessage always contains the landlord's email
                val messageLandlordEmail = message.landlordEmail.lowercase()
                
                val otherPartyEmail = when {
                    // If sender is the tenant, the other party is the landlord
                    message.senderEmail.lowercase() == normalizedCurrentEmail -> {
                        messageLandlordEmail
                    }
                    // If sender is not the tenant, check if sender is a contractor
                    else -> {
                        // Check if sender is a contractor (contractors send messages where senderEmail is contractor email)
                        val senderIsContractor = contractorUsers.containsKey(message.senderEmail.lowercase())
                        if (senderIsContractor) {
                            // Sender is a contractor
                            message.senderEmail.lowercase()
                        } else {
                            // Sender is the landlord (or default to landlordEmail)
                            messageLandlordEmail
                        }
                    }
                }
                
                conversationMap.getOrPut(otherPartyEmail) { mutableListOf() }.add(message)
            }
            
            // Convert to conversation list
            conversationMap.mapNotNull { (otherPartyEmail, messageList) ->
                val latestMessage = messageList.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                
                // Determine other party name and role
                // Check if otherPartyEmail matches the landlordEmail from any message in this conversation
                val isLandlord = messageList.any { it.landlordEmail.lowercase() == otherPartyEmail }
                
                val otherPartyName: String
                val otherPartyRole: com.example.mvp.data.UserRole
                
                if (isLandlord) {
                    // This is the landlord - use landlordEmail from message
                    val landlordEmailFromMessage = messageList.first().landlordEmail.lowercase()
                    otherPartyName = landlordUser?.name 
                        ?: landlordEmailFromMessage.split("@").first().replaceFirstChar { it.uppercase() }
                    otherPartyRole = com.example.mvp.data.UserRole.LANDLORD
                } else {
                    // This is a contractor
                    val contractorUser = contractorUsers.entries.find { it.key.lowercase() == otherPartyEmail }?.value
                    otherPartyName = contractorUser?.name ?: otherPartyEmail.split("@").first().replaceFirstChar { it.uppercase() }
                    otherPartyRole = com.example.mvp.data.UserRole.CONTRACTOR
                }
                
                // Calculate unread count
                val unreadCount = messageList.count {
                    it.senderEmail.lowercase() != normalizedCurrentEmail &&
                    !it.readBy.contains(normalizedCurrentEmail)
                }
                
                TenantMessageConversation(
                    otherPartyEmail = otherPartyEmail,
                    otherPartyName = otherPartyName,
                    otherPartyRole = otherPartyRole,
                    latestMessage = latestMessage,
                    unreadCount = unreadCount
                )
            }.sortedByDescending { it.latestMessage.timestamp }
        }
        
        if (conversations.isEmpty()) {
            // No messages
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Your conversations will appear here",
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
                items(conversations) { conversation ->
                    Card(
                        onClick = {
                            when (conversation.otherPartyRole) {
                                com.example.mvp.data.UserRole.LANDLORD -> onLandlordClick()
                                com.example.mvp.data.UserRole.CONTRACTOR -> onContractorClick?.invoke(conversation.otherPartyEmail)
                                else -> {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = when (conversation.otherPartyRole) {
                                    com.example.mvp.data.UserRole.LANDLORD -> MaterialTheme.colorScheme.primaryContainer
                                    com.example.mvp.data.UserRole.CONTRACTOR -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = conversation.otherPartyName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = when (conversation.otherPartyRole) {
                                            com.example.mvp.data.UserRole.LANDLORD -> MaterialTheme.colorScheme.onPrimaryContainer
                                            com.example.mvp.data.UserRole.CONTRACTOR -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Message info
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conversation.otherPartyName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    when (conversation.otherPartyRole) {
                                        com.example.mvp.data.UserRole.CONTRACTOR -> {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "Contractor",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        com.example.mvp.data.UserRole.LANDLORD -> {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "Landlord",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = conversation.latestMessage.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                            
                            // Timestamp
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = DateUtils.formatRelativeTime(conversation.latestMessage.timestamp) ?: "",
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

