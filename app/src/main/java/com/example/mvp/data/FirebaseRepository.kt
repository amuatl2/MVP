package com.example.mvp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        null
    }
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }
    
    private fun isAvailable(): Boolean = db != null && auth != null
    
    // Users collection
    suspend fun saveUser(user: User) {
        if (!isAvailable()) return
        val userId = auth?.currentUser?.uid ?: return
        try {
            val userMap = mutableMapOf<String, Any>(
                "email" to user.email,
                "role" to user.role.name,
                "name" to user.name
            )
            user.address?.let { userMap["address"] = it }
            user.city?.let { userMap["city"] = it }
            user.state?.let { userMap["state"] = it }
            user.companyName?.let { userMap["companyName"] = it }
            
            db?.collection("users")
                ?.document(userId)
                ?.set(userMap)
                ?.await()
        } catch (e: Exception) {
            // Firebase not initialized or error - ignore
        }
    }
    
    suspend fun getUser(userId: String): User? {
        if (!isAvailable()) return null
        return try {
            val doc = db?.collection("users")?.document(userId)?.get()?.await() ?: return null
            if (doc.exists()) {
                User(
                    email = doc.getString("email") ?: "",
                    role = UserRole.valueOf(doc.getString("role") ?: "TENANT"),
                    name = doc.getString("name") ?: "",
                    address = doc.getString("address"),
                    city = doc.getString("city"),
                    state = doc.getString("state"),
                    companyName = doc.getString("companyName")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getUserByEmail(email: String): User? {
        if (!isAvailable()) return null
        return try {
            val snapshot = db?.collection("users")
                ?.whereEqualTo("email", email)
                ?.limit(1)
                ?.get()
                ?.await() ?: return null
            snapshot.documents.firstOrNull()?.let { doc ->
                User(
                    email = doc.getString("email") ?: "",
                    role = UserRole.valueOf(doc.getString("role") ?: "TENANT"),
                    name = doc.getString("name") ?: "",
                    address = doc.getString("address"),
                    city = doc.getString("city"),
                    state = doc.getString("state"),
                    companyName = doc.getString("companyName")
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Tickets collection
    suspend fun saveTicket(ticket: Ticket) {
        if (!isAvailable()) return
        val ticketMap = mapOf(
            "id" to ticket.id,
            "title" to ticket.title,
            "description" to ticket.description,
            "category" to ticket.category,
            "status" to ticket.status.name,
            "submittedBy" to ticket.submittedBy,
            "submittedByRole" to ticket.submittedByRole.name,
            "assignedTo" to (ticket.assignedTo ?: ""),
            "assignedContractor" to (ticket.assignedContractor ?: ""),
            "aiDiagnosis" to (ticket.aiDiagnosis ?: ""),
            "photos" to ticket.photos,
            "createdAt" to ticket.createdAt,
            "scheduledDate" to (ticket.scheduledDate ?: ""),
            "completedDate" to (ticket.completedDate ?: ""),
            "rating" to (ticket.rating ?: 0f),
            "createdDate" to (ticket.createdDate ?: ""),
            "priority" to (ticket.priority ?: ""),
            "ticketNumber" to (ticket.ticketNumber ?: ""),
            "messages" to ticket.messages.map { msg ->
                mapOf(
                    "id" to msg.id,
                    "text" to msg.text,
                    "senderEmail" to msg.senderEmail,
                    "senderName" to msg.senderName,
                    "timestamp" to msg.timestamp
                )
            }
        )
        
        try {
            db?.collection("tickets")
                ?.document(ticket.id)
                ?.set(ticketMap)
                ?.await()
        } catch (e: Exception) {
            // Error saving - ignore
        }
    }
    
    suspend fun getTicket(ticketId: String): Ticket? {
        if (!isAvailable()) return null
        return try {
            val doc = db?.collection("tickets")?.document(ticketId)?.get()?.await() ?: return null
            doc.toTicket()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getAllTickets(): List<Ticket> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("tickets")?.get()?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toTicket() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun observeTickets(): Flow<List<Ticket>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = db?.collection("tickets")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val tickets = snapshot?.documents?.mapNotNull { it.toTicket() } ?: emptyList()
                trySend(tickets)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Jobs collection
    suspend fun saveJob(job: Job) {
        if (!isAvailable()) return
        val jobMap = mapOf(
            "id" to job.id,
            "ticketId" to job.ticketId,
            "contractorId" to job.contractorId,
            "propertyAddress" to job.propertyAddress,
            "issueType" to job.issueType,
            "date" to job.date,
            "status" to job.status,
            "cost" to (job.cost ?: 0),
            "duration" to (job.duration ?: 0),
            "rating" to (job.rating ?: 0f)
        )
        
        try {
            db?.collection("jobs")
                ?.document(job.id)
                ?.set(jobMap)
                ?.await()
        } catch (e: Exception) {
            // Error saving - ignore
        }
    }
    
    suspend fun getAllJobs(): List<Job> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("jobs")?.get()?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toJob() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun observeJobs(): Flow<List<Job>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = db?.collection("jobs")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { it.toJob() } ?: emptyList()
                trySend(jobs)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Contractors collection
    suspend fun saveContractor(contractor: Contractor) {
        if (!isAvailable()) return
        val contractorMap = mutableMapOf<String, Any>(
            "id" to contractor.id,
            "name" to contractor.name,
            "company" to contractor.company,
            "specialization" to contractor.specialization,
            "rating" to contractor.rating.toDouble(),
            "distance" to contractor.distance.toDouble(),
            "preferred" to contractor.preferred,
            "completedJobs" to contractor.completedJobs.toLong()
        )
        contractor.email?.let { contractorMap["email"] = it }
        contractor.city?.let { contractorMap["city"] = it }
        contractor.state?.let { contractorMap["state"] = it }
        try {
            db?.collection("contractors")
                ?.document(contractor.id)
                ?.set(contractorMap)
                ?.await()
        } catch (e: Exception) {
            // Error saving - ignore
        }
    }
    
    suspend fun getAllContractors(): List<Contractor> {
        if (!isAvailable()) return MockData.mockContractors
        return try {
            val snapshot = db?.collection("contractors")?.get()?.await() ?: return MockData.mockContractors
            val contractors = snapshot.documents.mapNotNull { it.toContractor() }
            if (contractors.isEmpty()) {
                MockData.mockContractors // Fallback to mock data
            } else {
                contractors
            }
        } catch (e: Exception) {
            // Fallback to mock data if Firestore not set up
            MockData.mockContractors
        }
    }
    
    // Helper extension functions to convert Firestore documents to data classes
    private fun com.google.firebase.firestore.DocumentSnapshot.toTicket(): Ticket? {
        return try {
            Ticket(
                id = getString("id") ?: return null,
                title = getString("title") ?: "",
                description = getString("description") ?: "",
                category = getString("category") ?: "",
                status = TicketStatus.valueOf(getString("status") ?: "SUBMITTED"),
                submittedBy = getString("submittedBy") ?: "",
                submittedByRole = try {
                    UserRole.valueOf(getString("submittedByRole") ?: "TENANT")
                } catch (e: Exception) {
                    UserRole.TENANT // Default for backwards compatibility
                },
                assignedTo = getString("assignedTo")?.takeIf { it.isNotEmpty() },
                assignedContractor = getString("assignedContractor")?.takeIf { it.isNotEmpty() },
                aiDiagnosis = getString("aiDiagnosis")?.takeIf { it.isNotEmpty() },
                photos = (get("photos") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                createdAt = getString("createdAt") ?: "",
                scheduledDate = getString("scheduledDate")?.takeIf { it.isNotEmpty() },
                completedDate = getString("completedDate")?.takeIf { it.isNotEmpty() },
                rating = (getDouble("rating")?.toFloat())?.takeIf { it > 0 },
                createdDate = getString("createdDate")?.takeIf { it.isNotEmpty() },
                priority = getString("priority")?.takeIf { it.isNotEmpty() },
                ticketNumber = getString("ticketNumber")?.takeIf { it.isNotEmpty() },
                messages = (get("messages") as? List<*>)?.mapNotNull { msgMap ->
                    (msgMap as? Map<*, *>)?.let {
                        Message(
                            id = it["id"] as? String ?: "",
                            text = it["text"] as? String ?: "",
                            senderEmail = it["senderEmail"] as? String ?: "",
                            senderName = it["senderName"] as? String ?: "",
                            timestamp = it["timestamp"] as? String ?: ""
                        )
                    }
                } ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toJob(): Job? {
        return try {
            Job(
                id = getString("id") ?: return null,
                ticketId = getString("ticketId") ?: "",
                contractorId = getString("contractorId") ?: "",
                propertyAddress = getString("propertyAddress") ?: "",
                issueType = getString("issueType") ?: "",
                date = getString("date") ?: "",
                status = getString("status") ?: "",
                cost = (getLong("cost")?.toInt())?.takeIf { it > 0 },
                duration = (getLong("duration")?.toInt())?.takeIf { it > 0 },
                rating = (getDouble("rating")?.toFloat())?.takeIf { it > 0 }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toContractor(): Contractor? {
        return try {
            Contractor(
                id = getString("id") ?: return null,
                name = getString("name") ?: "",
                company = getString("company") ?: "",
                specialization = (get("specialization") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                rating = (getDouble("rating") ?: 0.0).toFloat(),
                distance = (getDouble("distance") ?: 0.0).toFloat(),
                preferred = getBoolean("preferred") ?: false,
                completedJobs = (getLong("completedJobs") ?: 0L).toInt(),
                email = getString("email"),
                city = getString("city"),
                state = getString("state")
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Landlord-Tenant Connections
    suspend fun requestConnection(landlordEmail: String, tenantEmail: String, requestedBy: String) {
        if (!isAvailable()) return
        val connectionId = "${landlordEmail}_${tenantEmail}"
        val connection = mapOf(
            "id" to connectionId,
            "landlordEmail" to landlordEmail,
            "tenantEmail" to tenantEmail,
            "status" to ConnectionStatus.PENDING.name,
            "requestedBy" to requestedBy,
            "requestedAt" to com.example.mvp.utils.DateUtils.getCurrentDateTimeString(),
            "confirmedAt" to ""
        )
        try {
            db?.collection("connections")
                ?.document(connectionId)
                ?.set(connection)
                ?.await()
        } catch (e: Exception) {
            // Error saving
        }
    }
    
    suspend fun updateConnectionStatus(connectionId: String, status: ConnectionStatus) {
        if (!isAvailable()) return
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name
            )
            if (status == ConnectionStatus.CONNECTED) {
                updates["confirmedAt"] = com.example.mvp.utils.DateUtils.getCurrentDateTimeString()
            }
            db?.collection("connections")
                ?.document(connectionId)
                ?.update(updates)
                ?.await()
        } catch (e: Exception) {
            // Error updating
        }
    }
    
    suspend fun getConnectionsForUser(userEmail: String, userRole: UserRole): List<LandlordTenantConnection> {
        if (!isAvailable()) return emptyList()
        return try {
            val field = if (userRole == UserRole.LANDLORD) "landlordEmail" else "tenantEmail"
            val snapshot = db?.collection("connections")
                ?.whereEqualTo(field, userEmail)
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toConnection() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun observeConnections(userEmail: String, userRole: UserRole): Flow<List<LandlordTenantConnection>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val field = if (userRole == UserRole.LANDLORD) "landlordEmail" else "tenantEmail"
        val listener = db?.collection("connections")
            ?.whereEqualTo(field, userEmail)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val connections = snapshot?.documents?.mapNotNull { it.toConnection() } ?: emptyList()
                trySend(connections)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Direct Messages
    suspend fun sendDirectMessage(message: DirectMessage) {
        if (!isAvailable()) return
        val messageMap = mapOf(
            "id" to message.id,
            "landlordEmail" to message.landlordEmail,
            "tenantEmail" to message.tenantEmail,
            "senderEmail" to message.senderEmail,
            "senderName" to message.senderName,
            "text" to message.text,
            "timestamp" to message.timestamp
        )
        try {
            db?.collection("directMessages")
                ?.document(message.id)
                ?.set(messageMap)
                ?.await()
        } catch (e: Exception) {
            // Error saving
        }
    }
    
    suspend fun getDirectMessages(landlordEmail: String, tenantEmail: String): List<DirectMessage> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("directMessages")
                ?.whereEqualTo("landlordEmail", landlordEmail)
                ?.whereEqualTo("tenantEmail", tenantEmail)
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toDirectMessage() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun observeDirectMessages(landlordEmail: String, tenantEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = db?.collection("directMessages")
            ?.whereEqualTo("landlordEmail", landlordEmail)
            ?.whereEqualTo("tenantEmail", tenantEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                trySend(messages)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Helper functions
    private fun com.google.firebase.firestore.DocumentSnapshot.toConnection(): LandlordTenantConnection? {
        return try {
            LandlordTenantConnection(
                id = getString("id") ?: return null,
                landlordEmail = getString("landlordEmail") ?: "",
                tenantEmail = getString("tenantEmail") ?: "",
                status = ConnectionStatus.valueOf(getString("status") ?: "PENDING"),
                requestedBy = getString("requestedBy") ?: "",
                requestedAt = getString("requestedAt") ?: "",
                confirmedAt = getString("confirmedAt")?.takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toDirectMessage(): DirectMessage? {
        return try {
            DirectMessage(
                id = getString("id") ?: return null,
                landlordEmail = getString("landlordEmail") ?: "",
                tenantEmail = getString("tenantEmail") ?: "",
                senderEmail = getString("senderEmail") ?: "",
                senderName = getString("senderName") ?: "",
                text = getString("text") ?: "",
                timestamp = getString("timestamp") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Contractor-Landlord Messages
    suspend fun sendContractorLandlordMessage(message: ContractorLandlordMessage) {
        if (!isAvailable()) return
        val messageMap = mapOf(
            "id" to message.id,
            "ticketId" to message.ticketId,
            "contractorEmail" to message.contractorEmail,
            "landlordEmail" to message.landlordEmail,
            "senderEmail" to message.senderEmail,
            "senderName" to message.senderName,
            "text" to message.text,
            "timestamp" to message.timestamp
        )
        try {
            db?.collection("contractor_landlord_messages")
                ?.document(message.id)
                ?.set(messageMap)
                ?.await()
        } catch (e: Exception) {
            // Error saving
        }
    }
    
    suspend fun getContractorLandlordMessages(ticketId: String): List<ContractorLandlordMessage> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("contractor_landlord_messages")
                ?.whereEqualTo("ticketId", ticketId)
                ?.orderBy("timestamp")
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toContractorLandlordMessage() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun observeContractorLandlordMessages(ticketId: String): Flow<List<ContractorLandlordMessage>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = db?.collection("contractor_landlord_messages")
            ?.whereEqualTo("ticketId", ticketId)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toContractorLandlordMessage() } ?: emptyList()
                trySend(messages)
            }
        
        awaitClose { listener?.remove() }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toContractorLandlordMessage(): ContractorLandlordMessage? {
        return try {
            ContractorLandlordMessage(
                id = getString("id") ?: return null,
                ticketId = getString("ticketId") ?: "",
                contractorEmail = getString("contractorEmail") ?: "",
                landlordEmail = getString("landlordEmail") ?: "",
                senderEmail = getString("senderEmail") ?: "",
                senderName = getString("senderName") ?: "",
                text = getString("text") ?: "",
                timestamp = getString("timestamp") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Job Applications
    suspend fun saveJobApplication(application: JobApplication) {
        if (!isAvailable()) return
        val applicationMap = mapOf(
            "id" to application.id,
            "ticketId" to application.ticketId,
            "contractorId" to application.contractorId,
            "contractorName" to application.contractorName,
            "contractorEmail" to application.contractorEmail,
            "appliedAt" to application.appliedAt,
            "status" to application.status.name
        )
        try {
            db?.collection("jobApplications")
                ?.document(application.id)
                ?.set(applicationMap)
                ?.await()
        } catch (e: Exception) {
            // Error saving
        }
    }
    
    suspend fun getJobApplications(ticketId: String): List<JobApplication> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("jobApplications")
                ?.whereEqualTo("ticketId", ticketId)
                ?.whereEqualTo("status", ApplicationStatus.PENDING.name)
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toJobApplication() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun observeJobApplications(ticketId: String): Flow<List<JobApplication>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = db?.collection("jobApplications")
            ?.whereEqualTo("ticketId", ticketId)
            ?.whereEqualTo("status", ApplicationStatus.PENDING.name)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val applications = snapshot?.documents?.mapNotNull { it.toJobApplication() } ?: emptyList()
                trySend(applications)
            }
        
        awaitClose { listener?.remove() }
    }
    
    suspend fun updateApplicationStatus(applicationId: String, status: ApplicationStatus) {
        if (!isAvailable()) return
        try {
            db?.collection("jobApplications")
                ?.document(applicationId)
                ?.update("status", status.name)
                ?.await()
        } catch (e: Exception) {
            // Error updating
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toJobApplication(): JobApplication? {
        return try {
            JobApplication(
                id = getString("id") ?: return null,
                ticketId = getString("ticketId") ?: "",
                contractorId = getString("contractorId") ?: "",
                contractorName = getString("contractorName") ?: "",
                contractorEmail = getString("contractorEmail") ?: "",
                appliedAt = getString("appliedAt") ?: "",
                status = try {
                    ApplicationStatus.valueOf(getString("status") ?: "PENDING")
                } catch (e: Exception) {
                    ApplicationStatus.PENDING
                }
            )
        } catch (e: Exception) {
            null
        }
    }
}

