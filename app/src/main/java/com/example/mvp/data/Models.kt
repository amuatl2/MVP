package com.example.mvp.data

data class User(
    val email: String,
    val role: UserRole,
    val name: String,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val companyName: String? = null // For contractors
)

enum class UserRole {
    TENANT, LANDLORD, CONTRACTOR
}

data class Message(
    val id: String,
    val text: String,
    val senderEmail: String,
    val senderName: String,
    val timestamp: String
)

data class Ticket(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val status: TicketStatus,
    val submittedBy: String,
    val submittedByRole: UserRole,
    val assignedTo: String? = null,
    val aiDiagnosis: String? = null,
    val photos: List<String> = emptyList(),
    val createdAt: String,
    val scheduledDate: String? = null,
    val completedDate: String? = null,
    val rating: Float? = null,
    val assignedContractor: String? = null,
    val createdDate: String? = null,
    val priority: String? = null,
    val ticketNumber: String? = null,
    val messages: List<Message> = emptyList(),
    val viewedByLandlord: Boolean = false
)

enum class TicketStatus {
    SUBMITTED, ASSIGNED, SCHEDULED, COMPLETED
}

data class Contractor(
    val id: String,
    val name: String,
    val company: String,
    val specialization: List<String>,
    val rating: Float,
    val distance: Float,
    val preferred: Boolean,
    val completedJobs: Int,
    val email: String? = null,
    val city: String? = null,
    val state: String? = null,
    val serviceAreas: Map<String, List<String>> = emptyMap() // Map of state -> list of cities
)

data class Job(
    val id: String,
    val ticketId: String,
    val contractorId: String,
    val propertyAddress: String,
    val issueType: String,
    val date: String,
    val status: String,
    val cost: Int? = null,
    val duration: Int? = null,
    val rating: Float? = null,
    val completionPhotos: List<String> = emptyList(),
    val completionNotes: String? = null,
    val scheduledDate: String? = null, // Format: "yyyy-MM-dd"
    val scheduledTime: String? = null // Format: "HH:mm"
)

data class JobApplication(
    val id: String,
    val ticketId: String,
    val contractorId: String,
    val contractorName: String,
    val contractorEmail: String,
    val appliedAt: String,
    val status: ApplicationStatus = ApplicationStatus.PENDING
)

enum class ApplicationStatus {
    PENDING, ACCEPTED, REJECTED
}

data class JobInvitation(
    val id: String,
    val ticketId: String,
    val contractorId: String,
    val contractorEmail: String,
    val landlordEmail: String,
    val invitedAt: String,
    val status: InvitationStatus = InvitationStatus.PENDING
)

enum class InvitationStatus {
    PENDING, ACCEPTED, DECLINED
}

data class LandlordTenantConnection(
    val id: String,
    val landlordEmail: String,
    val tenantEmail: String,
    val status: ConnectionStatus,
    val requestedBy: String, // Email of who requested
    val requestedAt: String,
    val confirmedAt: String? = null
)

enum class ConnectionStatus {
    PENDING, // Tenant needs to confirm
    CONNECTED, // Both parties confirmed
    REJECTED // Tenant rejected
}

data class DirectMessage(
    val id: String,
    val landlordEmail: String = "", // Kept for backward compatibility
    val tenantEmail: String = "", // Kept for backward compatibility
    val senderEmail: String,
    val receiverEmail: String, // New: the recipient of the message (generalized)
    val senderName: String,
    val text: String,
    val timestamp: String,
    val readBy: Set<String> = emptySet() // Set of emails who have read this message
)

data class ContractorLandlordMessage(
    val id: String,
    val ticketId: String,
    val contractorEmail: String,
    val landlordEmail: String,
    val senderEmail: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val readBy: Set<String> = emptySet() // Set of emails who have read this message
)

