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
            "rating" to (ticket.rating?.toDouble() ?: 0.0), // Save as Double for Firestore
            "createdDate" to (ticket.createdDate ?: ""),
            "priority" to (ticket.priority ?: ""),
            "ticketNumber" to (ticket.ticketNumber ?: ""),
            "viewedByLandlord" to ticket.viewedByLandlord,
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
        val jobMap = mutableMapOf(
            "id" to job.id,
            "ticketId" to job.ticketId,
            "contractorId" to job.contractorId,
            "propertyAddress" to job.propertyAddress,
            "issueType" to job.issueType,
            "date" to job.date,
            "status" to job.status,
            "cost" to (job.cost ?: 0),
            "duration" to (job.duration ?: 0),
            "completionPhotos" to (job.completionPhotos ?: emptyList()),
            "completionNotes" to (job.completionNotes ?: "")
        )
        
        // Only save rating if it's set (not null and > 0)
        job.rating?.let { rating ->
            if (rating > 0f) {
                jobMap["rating"] = rating.toDouble()
                android.util.Log.d("FirebaseRepository", "Job ${job.id} rating: $rating")
            }
        }
        
        // Add optional fields if they exist
        job.scheduledDate?.let { 
            jobMap["scheduledDate"] = it
            android.util.Log.d("FirebaseRepository", "Job ${job.id} scheduledDate: $it")
        }
        job.scheduledTime?.let { 
            jobMap["scheduledTime"] = it
            android.util.Log.d("FirebaseRepository", "Job ${job.id} scheduledTime: $it")
        }
        
        android.util.Log.d("FirebaseRepository", "Saving job ${job.id} with status: ${job.status}, rating: ${job.rating}, scheduledDate: ${job.scheduledDate}, scheduledTime: ${job.scheduledTime}")
        
        try {
            db?.collection("jobs")
                ?.document(job.id)
                ?.set(jobMap)
                ?.await()
            android.util.Log.d("FirebaseRepository", "Successfully saved job ${job.id} with rating: ${job.rating}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error saving job ${job.id}: ${e.message}", e)
        }
    }
    
    suspend fun getAllJobs(): List<Job> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("jobs")?.get()?.await() ?: return emptyList()
            val jobs = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toJob()
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseRepository", "Error converting job document ${doc.id}: ${e.message}", e)
                    null
                }
            }
            android.util.Log.d("FirebaseRepository", "Loaded ${jobs.size} jobs from Firestore")
            jobs
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting all jobs: ${e.message}", e)
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
        if (contractor.serviceAreas.isNotEmpty()) {
            contractorMap["serviceAreas"] = contractor.serviceAreas.mapValues { (_, cities) -> cities }
        }
        
        android.util.Log.d("FirebaseRepository", "Saving contractor ${contractor.id} (${contractor.email}) with rating: ${contractor.rating}")
        
        try {
            db?.collection("contractors")
                ?.document(contractor.id)
                ?.set(contractorMap)
                ?.await()
            android.util.Log.d("FirebaseRepository", "Successfully saved contractor ${contractor.id} with rating: ${contractor.rating}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error saving contractor ${contractor.id}: ${e.message}", e)
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
    
    fun observeContractors(): Flow<List<Contractor>> = callbackFlow {
        if (!isAvailable()) {
            trySend(MockData.mockContractors)
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = db?.collection("contractors")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing contractors: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val contractors = snapshot?.documents?.mapNotNull { it.toContractor() } ?: emptyList()
                trySend(if (contractors.isEmpty()) MockData.mockContractors else contractors)
            }
        
        awaitClose { listener?.remove() }
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
                rating = getDouble("rating")?.toFloat(), // Don't filter out 0 - allow null for unrated jobs
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
                } ?: emptyList(),
                viewedByLandlord = getBoolean("viewedByLandlord") ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toJob(): Job? {
        return try {
            val id = getString("id") ?: return null
            Job(
                id = id,
                ticketId = getString("ticketId") ?: "",
                contractorId = getString("contractorId") ?: "",
                propertyAddress = getString("propertyAddress") ?: "",
                issueType = getString("issueType") ?: "",
                date = getString("date") ?: "",
                status = getString("status") ?: "",
                cost = (getLong("cost")?.toInt())?.takeIf { it > 0 },
                duration = (getLong("duration")?.toInt())?.takeIf { it > 0 },
                rating = getDouble("rating")?.toFloat(), // Don't filter out 0 - allow null for unrated jobs
                completionPhotos = (get("completionPhotos") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                completionNotes = getString("completionNotes")?.takeIf { it.isNotEmpty() },
                scheduledDate = getString("scheduledDate")?.takeIf { it.isNotEmpty() },
                scheduledTime = getString("scheduledTime")?.takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error parsing job: ${e.message}", e)
            null
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toContractor(): Contractor? {
        return try {
            val serviceAreasMap = mutableMapOf<String, List<String>>()
            val serviceAreasData = get("serviceAreas") as? Map<*, *>
            serviceAreasData?.forEach { (state, cities) ->
                if (state is String && cities is List<*>) {
                    serviceAreasMap[state] = cities.mapNotNull { it as? String }
                }
            }
            
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
                state = getString("state"),
                serviceAreas = serviceAreasMap
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Landlord-Tenant Connections
    suspend fun requestConnection(landlordEmail: String, tenantEmail: String, requestedBy: String) {
        if (!isAvailable()) {
            android.util.Log.e("FirebaseRepository", "Firebase not available - cannot save connection")
            return
        }
        
        // CRITICAL: Check if user is authenticated
        val currentUser = auth?.currentUser
        if (currentUser == null) {
            android.util.Log.e("FirebaseRepository", "User not authenticated - cannot save connection")
            throw Exception("User not authenticated. Please sign in first.")
        }
        
        android.util.Log.d("FirebaseRepository", "Current authenticated user: ${currentUser.uid}, email: ${currentUser.email}")
        
        // Normalize emails (trim and lowercase) to ensure consistent matching
        val normalizedLandlordEmail = landlordEmail.trim().lowercase()
        val normalizedTenantEmail = tenantEmail.trim().lowercase()
        val normalizedRequestedBy = requestedBy.trim().lowercase()
        
        val connectionId = "${normalizedLandlordEmail}_${normalizedTenantEmail}"
        val connection = mapOf(
            "id" to connectionId,
            "landlordEmail" to normalizedLandlordEmail,
            "tenantEmail" to normalizedTenantEmail,
            "status" to ConnectionStatus.PENDING.name,
            "requestedBy" to normalizedRequestedBy,
            "requestedAt" to com.example.mvp.utils.DateUtils.getCurrentDateTimeString(),
            "confirmedAt" to ""
        )
        try {
            android.util.Log.d("FirebaseRepository", "Saving connection request: $connectionId (landlord: $normalizedLandlordEmail, tenant: $normalizedTenantEmail)")
            android.util.Log.d("FirebaseRepository", "Authenticated user UID: ${currentUser.uid}")
            db?.collection("connections")
                ?.document(connectionId)
                ?.set(connection)
                ?.await()
            android.util.Log.d("FirebaseRepository", "Connection request saved successfully to collection 'connections'")
        } catch (e: Exception) {
            // Log error for debugging
            android.util.Log.e("FirebaseRepository", "Error saving connection request: ${e.message}", e)
            android.util.Log.e("FirebaseRepository", "Error type: ${e.javaClass.simpleName}")
            throw e // Re-throw to allow caller to handle
        }
    }
    
    suspend fun updateConnectionStatus(connectionId: String, status: ConnectionStatus) {
        if (!isAvailable()) return
        try {
            if (status == ConnectionStatus.REJECTED) {
                // Delete the connection request if rejected
                db?.collection("connections")
                    ?.document(connectionId)
                    ?.delete()
                    ?.await()
            } else {
                // Update status to CONNECTED
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
            }
        } catch (e: Exception) {
            // Error updating/deleting
        }
    }
    
    suspend fun deleteConnection(connectionId: String) {
        if (!isAvailable()) return
        try {
            db?.collection("connections")
                ?.document(connectionId)
                ?.delete()
                ?.await()
            android.util.Log.d("FirebaseRepository", "Deleted connection: $connectionId")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error deleting connection: ${e.message}", e)
        }
    }
    
    suspend fun getConnectionsForUser(userEmail: String, userRole: UserRole): List<LandlordTenantConnection> {
        if (!isAvailable()) return emptyList()
        return try {
            // Normalize email to ensure consistent matching
            val normalizedEmail = userEmail.trim().lowercase()
            val field = if (userRole == UserRole.LANDLORD) "landlordEmail" else "tenantEmail"
            android.util.Log.d("FirebaseRepository", "Getting connections for $normalizedEmail as ${userRole.name}, field: $field")
            val snapshot = db?.collection("connections")
                ?.whereEqualTo(field, normalizedEmail)
                ?.get()
                ?.await() ?: return emptyList()
            val connections = snapshot.documents.mapNotNull { it.toConnection() }
            android.util.Log.d("FirebaseRepository", "Retrieved ${connections.size} connections for $normalizedEmail")
            connections
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting connections: ${e.message}", e)
            emptyList()
        }
    }
    
    fun observeConnections(userEmail: String, userRole: UserRole): Flow<List<LandlordTenantConnection>> = callbackFlow {
        if (!isAvailable()) {
            android.util.Log.e("FirebaseRepository", "Firebase not available - cannot observe connections")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        // CRITICAL: Verify user is authenticated before making query
        val currentAuthUser = auth?.currentUser
        if (currentAuthUser == null) {
            android.util.Log.e("FirebaseRepository", "User not authenticated - cannot observe connections")
            android.util.Log.e("FirebaseRepository", "Auth instance: ${auth != null}, Current user: null")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        android.util.Log.d("FirebaseRepository", "Authenticated user UID: ${currentAuthUser.uid}, email: ${currentAuthUser.email}")
        
        // Normalize email to ensure consistent matching
        val normalizedEmail = userEmail.trim().lowercase()
        val field = if (userRole == UserRole.LANDLORD) "landlordEmail" else "tenantEmail"
        android.util.Log.d("FirebaseRepository", "Observing connections for $normalizedEmail as ${userRole.name}, field: $field")
        android.util.Log.d("FirebaseRepository", "Query: connections where $field == $normalizedEmail")
        
        val listener = db?.collection("connections")
            ?.whereEqualTo(field, normalizedEmail)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing connections: ${error.message}", error)
                    android.util.Log.e("FirebaseRepository", "Error code: ${error.code}, details: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val connections = snapshot?.documents?.mapNotNull { doc ->
                    android.util.Log.d("FirebaseRepository", "Found connection document: ${doc.id}")
                    android.util.Log.d("FirebaseRepository", "Document data: ${doc.data}")
                    val connection = doc.toConnection()
                    android.util.Log.d("FirebaseRepository", "Parsed connection: tenantEmail=${connection?.tenantEmail}, landlordEmail=${connection?.landlordEmail}, status=${connection?.status}")
                    connection
                } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Found ${connections.size} connections for $normalizedEmail (querying by $field)")
                android.util.Log.d("FirebaseRepository", "Connection emails: ${connections.map { "${it.tenantEmail} (status: ${it.status})" }}")
                trySend(connections)
            }
        
        awaitClose { 
            android.util.Log.d("FirebaseRepository", "Removing connections listener for $normalizedEmail")
            listener?.remove() 
        }
    }
    
    // Get all connections (for contractors who need to find landlord emails for any tenant)
    suspend fun getAllConnections(): List<LandlordTenantConnection> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("connections")
                ?.get()
                ?.await() ?: return emptyList()
            val connections = snapshot.documents.mapNotNull { it.toConnection() }
            android.util.Log.d("FirebaseRepository", "Retrieved ${connections.size} total connections")
            connections
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting all connections: ${e.message}", e)
            emptyList()
        }
    }
    
    // Observe all connections (for contractors)
    fun observeAllConnections(): Flow<List<LandlordTenantConnection>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        android.util.Log.d("FirebaseRepository", "Observing all connections")
        
        val listener = db?.collection("connections")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing all connections: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val connections = snapshot?.documents?.mapNotNull { it.toConnection() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${connections.size} total connections")
                trySend(connections)
            }
        
        awaitClose { 
            android.util.Log.d("FirebaseRepository", "Removing all connections listener")
            listener?.remove() 
        }
    }
    
    // Direct Messages
    suspend fun sendDirectMessage(message: DirectMessage) {
        if (!isAvailable()) return
        val messageMap = mutableMapOf<String, Any>(
            "id" to message.id,
            "senderEmail" to message.senderEmail.trim().lowercase(),
            "receiverEmail" to message.receiverEmail.trim().lowercase(),
            "senderName" to message.senderName,
            "text" to message.text,
            "timestamp" to message.timestamp,
            "readBy" to message.readBy.toList()
        )
        // Keep old fields for backward compatibility if they exist
        if (message.landlordEmail.isNotEmpty()) {
            messageMap["landlordEmail"] = message.landlordEmail.trim().lowercase()
        }
        if (message.tenantEmail.isNotEmpty()) {
            messageMap["tenantEmail"] = message.tenantEmail.trim().lowercase()
        }
        try {
            android.util.Log.d("FirebaseRepository", "Sending DirectMessage: id=${message.id}, sender=${message.senderEmail}, landlord=${message.landlordEmail}, tenant=${message.tenantEmail}")
            db?.collection("directMessages")
                ?.document(message.id)
                ?.set(messageMap)
                ?.await()
            android.util.Log.d("FirebaseRepository", "Successfully sent DirectMessage: ${message.id}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error sending DirectMessage: ${e.message}", e)
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
    
    // New generalized function to observe messages between two users
    fun observeDirectMessagesBetweenUsers(user1Email: String, user2Email: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedUser1 = user1Email.trim().lowercase()
        val normalizedUser2 = user2Email.trim().lowercase()
        
        android.util.Log.d("FirebaseRepository", "Observing messages between: $normalizedUser1 and $normalizedUser2")
        
        // Query messages where user1 is sender and user2 is receiver
        val listener1 = db?.collection("directMessages")
            ?.whereEqualTo("senderEmail", normalizedUser1)
            ?.whereEqualTo("receiverEmail", normalizedUser2)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing messages (direction 1): ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                if (messages.isNotEmpty()) {
                    trySend(messages)
                }
            }
        
        // Query messages where user2 is sender and user1 is receiver
        val listener2 = db?.collection("directMessages")
            ?.whereEqualTo("senderEmail", normalizedUser2)
            ?.whereEqualTo("receiverEmail", normalizedUser1)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing messages (direction 2): ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                if (messages.isNotEmpty()) {
                    trySend(messages)
                }
            }
        
        awaitClose { 
            listener1?.remove()
            listener2?.remove()
        }
    }
    
    fun observeDirectMessages(landlordEmail: String, tenantEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            awaitClose { }
            return@callbackFlow
        }
        
        // Normalize emails to ensure consistent matching
        val normalizedLandlordEmail = landlordEmail.trim().lowercase()
        val normalizedTenantEmail = tenantEmail.trim().lowercase()
        
        android.util.Log.d("FirebaseRepository", "Observing direct messages: landlord=$normalizedLandlordEmail, tenant=$normalizedTenantEmail")
        
        // Use new generalized function for new messages, and old format for backward compatibility
        val allMessages = mutableListOf<DirectMessage>()
        
        // Query new format (receiverEmail)
        val listener1 = db?.collection("directMessages")
            ?.whereEqualTo("senderEmail", normalizedLandlordEmail)
            ?.whereEqualTo("receiverEmail", normalizedTenantEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages (new format): ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                allMessages.removeAll { it.senderEmail.lowercase() == normalizedLandlordEmail && it.receiverEmail.lowercase() == normalizedTenantEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        val listener2 = db?.collection("directMessages")
            ?.whereEqualTo("senderEmail", normalizedTenantEmail)
            ?.whereEqualTo("receiverEmail", normalizedLandlordEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages (new format reverse): ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                allMessages.removeAll { it.senderEmail.lowercase() == normalizedTenantEmail && it.receiverEmail.lowercase() == normalizedLandlordEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        // Also query old format for backward compatibility
        val listener3 = db?.collection("directMessages")
            ?.whereEqualTo("landlordEmail", normalizedLandlordEmail)
            ?.whereEqualTo("tenantEmail", normalizedTenantEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages (old format): ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                // Only add if not already in allMessages (avoid duplicates)
                val newMessages = messages.filter { msg ->
                    !allMessages.any { it.id == msg.id }
                }
                allMessages.addAll(newMessages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        awaitClose { 
            android.util.Log.d("FirebaseRepository", "Removing direct messages listeners")
            listener1?.remove()
            listener2?.remove()
            listener3?.remove()
        }
    }
    
    fun observeDirectMessagesByLandlord(landlordEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedLandlordEmail = landlordEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing all direct messages for landlord: $normalizedLandlordEmail")
        
        // Emit empty list initially so combine can work
        trySend(emptyList())
        
        val allMessages = mutableListOf<DirectMessage>()
        
        // Query new format: messages where landlord is receiver (receiverEmail)
        val listener1 = db?.collection("directMessages")
            ?.whereEqualTo("receiverEmail", normalizedLandlordEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages by landlord (receiverEmail): ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                allMessages.removeAll { it.receiverEmail.lowercase() == normalizedLandlordEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        // Query old format: messages where landlordEmail matches (for backward compatibility)
        val listener2 = db?.collection("directMessages")
            ?.whereEqualTo("landlordEmail", normalizedLandlordEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages by landlord (old format): ${error.message}", error)
                    // Don't send empty list on error - let existing messages stay
                    // This prevents overwriting messages when index is building
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                // Only add if not already in allMessages (avoid duplicates)
                val newMessages = messages.filter { msg ->
                    !allMessages.any { it.id == msg.id }
                }
                allMessages.addAll(newMessages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        awaitClose { 
            listener1?.remove()
            listener2?.remove()
        }
    }
    
    // Observe DirectMessages where landlord is the sender
    fun observeDirectMessagesByLandlordAsSender(landlordEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedLandlordEmail = landlordEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing direct messages where landlord is sender: $normalizedLandlordEmail")
        
        // Emit empty list initially so combine can work
        trySend(emptyList())
        
        val listener = db?.collection("directMessages")
            ?.whereEqualTo("senderEmail", normalizedLandlordEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages by landlord as sender: ${error.message}", error)
                    // Don't send empty list on error - let existing messages stay
                    // This prevents overwriting messages when index is building
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${messages.size} direct messages where landlord is sender")
                trySend(messages)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Observe DirectMessages sent by a contractor (for contractor-tenant messaging)
    fun observeDirectMessagesByContractor(contractorEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedContractorEmail = contractorEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing direct messages sent by contractor: $normalizedContractorEmail")
        
        val listener = db?.collection("directMessages")
            ?.whereEqualTo("senderEmail", normalizedContractorEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages by contractor: ${error.message}", error)
                    // Don't send empty list on error - let existing messages stay
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${messages.size} direct messages sent by contractor")
                trySend(messages)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Observe all DirectMessages for a user (where user is sender or receiver)
    fun observeDirectMessagesForUser(userEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedUserEmail = userEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing all direct messages for user: $normalizedUserEmail")
        
        val allMessages = mutableListOf<DirectMessage>()
        
        // Query messages where user is sender (new format)
        val listener1 = db?.collection("directMessages")
            ?.whereEqualTo("senderEmail", normalizedUserEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing messages where user is sender: ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                allMessages.removeAll { it.senderEmail.lowercase() == normalizedUserEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        // Query messages where user is receiver (new format)
        val listener2 = db?.collection("directMessages")
            ?.whereEqualTo("receiverEmail", normalizedUserEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing messages where user is receiver: ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                allMessages.removeAll { it.receiverEmail.lowercase() == normalizedUserEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        awaitClose { 
            listener1?.remove()
            listener2?.remove()
        }
    }
    
    // Observe all DirectMessages for a tenant (where tenant is involved as sender or receiver)
    // Uses new receiverEmail field and old tenantEmail field for backward compatibility
    fun observeDirectMessagesByTenant(tenantEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedTenantEmail = tenantEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing all direct messages for tenant: $normalizedTenantEmail")
        
        val allMessages = mutableListOf<DirectMessage>()
        
        // Query messages where tenant is sender (new format)
        val listener1 = db?.collection("directMessages")
            ?.whereEqualTo("senderEmail", normalizedTenantEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing messages where tenant is sender: ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                allMessages.removeAll { it.senderEmail.lowercase() == normalizedTenantEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        // Query messages where tenant is receiver (new format)
        val listener2 = db?.collection("directMessages")
            ?.whereEqualTo("receiverEmail", normalizedTenantEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing messages where tenant is receiver: ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                allMessages.removeAll { it.receiverEmail.lowercase() == normalizedTenantEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        // Also query old format for backward compatibility
        val listener3 = db?.collection("directMessages")
            ?.whereEqualTo("tenantEmail", normalizedTenantEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages by tenant (old format): ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                // Only add if not already in allMessages
                val newMessages = messages.filter { msg ->
                    !allMessages.any { it.id == msg.id }
                }
                allMessages.addAll(newMessages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        awaitClose { 
            listener1?.remove()
            listener2?.remove()
            listener3?.remove()
        }
    }
    
    // Observe DirectMessages where tenant is sender (for contractors to see tenant messages)
    fun observeDirectMessagesFromTenant(tenantEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedTenantEmail = tenantEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing direct messages from tenant: $normalizedTenantEmail")
        
        // Query messages where tenantEmail matches and senderEmail is the tenant (tenant sent the message)
        val listener = db?.collection("directMessages")
            ?.whereEqualTo("tenantEmail", normalizedTenantEmail)
            ?.whereEqualTo("senderEmail", normalizedTenantEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing direct messages from tenant: ${error.message}", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toDirectMessage() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${messages.size} direct messages from tenant $normalizedTenantEmail")
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
            val readByList = (get("readBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val senderEmail = getString("senderEmail") ?: return null
            // For backward compatibility: if receiverEmail doesn't exist, derive it from old fields
            val receiverEmail = getString("receiverEmail") ?: run {
                // Old messages: determine receiver from landlordEmail/tenantEmail
                val landlordEmail = getString("landlordEmail") ?: ""
                val tenantEmail = getString("tenantEmail") ?: ""
                // If sender is landlord, receiver is tenant; if sender is tenant, receiver is landlord
                when {
                    senderEmail.lowercase() == landlordEmail.lowercase() -> tenantEmail
                    senderEmail.lowercase() == tenantEmail.lowercase() -> landlordEmail
                    else -> landlordEmail.ifEmpty { tenantEmail } // Fallback
                }
            }
            DirectMessage(
                id = getString("id") ?: return null,
                landlordEmail = getString("landlordEmail") ?: "",
                tenantEmail = getString("tenantEmail") ?: "",
                senderEmail = senderEmail,
                receiverEmail = receiverEmail.lowercase(),
                senderName = getString("senderName") ?: "",
                text = getString("text") ?: "",
                timestamp = getString("timestamp") ?: "",
                readBy = readByList.toSet()
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
            "contractorEmail" to message.contractorEmail.trim().lowercase(),
            "landlordEmail" to message.landlordEmail.trim().lowercase(),
            "senderEmail" to message.senderEmail.trim().lowercase(),
            "senderName" to message.senderName,
            "text" to message.text,
            "timestamp" to message.timestamp,
            "readBy" to message.readBy.toList()
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
                    android.util.Log.e("FirebaseRepository", "Error observing contractor-landlord messages by ticketId: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toContractorLandlordMessage() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${messages.size} messages for ticketId: $ticketId")
                trySend(messages)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Observe contractor-landlord messages between two specific parties (like direct messages)
    fun observeContractorLandlordMessagesBetween(
        contractorEmail: String,
        landlordEmail: String,
        ticketId: String? = null
    ): Flow<List<ContractorLandlordMessage>> = callbackFlow {
        if (!isAvailable()) {
            awaitClose { }
            return@callbackFlow
        }
        
        // Normalize emails to ensure consistent matching (like direct messages)
        val normalizedContractorEmail = contractorEmail.trim().lowercase()
        val normalizedLandlordEmail = landlordEmail.trim().lowercase()
        
        android.util.Log.d("FirebaseRepository", "Observing contractor-landlord messages: contractor=$normalizedContractorEmail, landlord=$normalizedLandlordEmail, ticketId=$ticketId")
        
        // Build query - always get ALL messages between contractor and landlord
        // Don't filter by ticketId - we want to show all messages in one conversation
        val query = db?.collection("contractor_landlord_messages")
            ?.whereEqualTo("contractorEmail", normalizedContractorEmail)
            ?.whereEqualTo("landlordEmail", normalizedLandlordEmail)
            ?.orderBy("timestamp")
        
        val listener = query
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing contractor-landlord messages: ${error.message}", error)
                    // Don't send empty list on error - let existing messages stay (like direct messages)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toContractorLandlordMessage() } ?: emptyList()
                
                // Return ALL messages between contractor and landlord, regardless of ticketId
                // This allows contractors to see all messages with a landlord in one conversation
                android.util.Log.d("FirebaseRepository", "Received ${messages.size} contractor-landlord messages (all tickets)")
                trySend(messages)
            }
        
        awaitClose { 
            android.util.Log.d("FirebaseRepository", "Removing contractor-landlord messages listener")
            listener?.remove() 
        }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toContractorLandlordMessage(): ContractorLandlordMessage? {
        return try {
            val readByList = (get("readBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            ContractorLandlordMessage(
                id = getString("id") ?: return null,
                ticketId = getString("ticketId") ?: "",
                contractorEmail = getString("contractorEmail") ?: "",
                landlordEmail = getString("landlordEmail") ?: "",
                senderEmail = getString("senderEmail") ?: "",
                senderName = getString("senderName") ?: "",
                text = getString("text") ?: "",
                timestamp = getString("timestamp") ?: "",
                readBy = readByList.toSet()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Get all contractor-landlord messages for a contractor
    suspend fun getContractorLandlordMessagesByContractor(contractorEmail: String): List<ContractorLandlordMessage> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("contractor_landlord_messages")
                ?.whereEqualTo("contractorEmail", contractorEmail.lowercase())
                ?.orderBy("timestamp")
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toContractorLandlordMessage() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Observe all contractor-landlord messages for a contractor
    fun observeContractorLandlordMessagesByContractor(contractorEmail: String): Flow<List<ContractorLandlordMessage>> = callbackFlow {
        if (!isAvailable()) {
            android.util.Log.w("FirebaseRepository", "Firebase not available for observeContractorLandlordMessagesByContractor")
            // Send empty list initially if Firebase not available, but don't block
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedEmail = contractorEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing messages for contractor: $normalizedEmail")
        
        val allMessages = mutableListOf<ContractorLandlordMessage>()
        
        // Query messages where contractorEmail matches (contractor is recipient)
        val listener1 = db?.collection("contractor_landlord_messages")
            ?.whereEqualTo("contractorEmail", normalizedEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing contractor messages (by contractorEmail): ${error.message}", error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { it.toContractorLandlordMessage() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${messages.size} messages where contractorEmail=$normalizedEmail")
                allMessages.removeAll { it.contractorEmail.lowercase() == normalizedEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        // Query messages where senderEmail matches (contractor is sender)
        val listener2 = db?.collection("contractor_landlord_messages")
            ?.whereEqualTo("senderEmail", normalizedEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing contractor messages (by senderEmail): ${error.message}", error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { it.toContractorLandlordMessage() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${messages.size} messages where senderEmail=$normalizedEmail")
                allMessages.removeAll { it.senderEmail.lowercase() == normalizedEmail }
                allMessages.addAll(messages)
                trySend(allMessages.sortedBy { it.timestamp })
            }
        
        awaitClose { 
            android.util.Log.d("FirebaseRepository", "Removing listeners for contractor: $normalizedEmail")
            listener1?.remove()
            listener2?.remove()
        }
    }
    
    // Observe all contractor-landlord messages for a landlord
    fun observeContractorLandlordMessagesByLandlord(landlordEmail: String): Flow<List<ContractorLandlordMessage>> = callbackFlow {
        if (!isAvailable()) {
            android.util.Log.w("FirebaseRepository", "Firebase not available for observeContractorLandlordMessagesByLandlord")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedEmail = landlordEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing messages for landlord: $normalizedEmail")
        
        val listener = db?.collection("contractor_landlord_messages")
            ?.whereEqualTo("landlordEmail", normalizedEmail)
            ?.orderBy("timestamp")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing landlord messages: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toContractorLandlordMessage() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${messages.size} messages for landlord: $normalizedEmail")
                trySend(messages)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Mark messages as read
    suspend fun markDirectMessagesAsRead(messageIds: List<String>, readerEmail: String) {
        if (!isAvailable()) return
        val normalizedReaderEmail = readerEmail.trim().lowercase()
        try {
            messageIds.forEach { messageId ->
                db?.collection("directMessages")
                    ?.document(messageId)
                    ?.get()
                    ?.await()
                    ?.let { doc ->
                        val currentReadBy = (doc.get("readBy") as? List<*>)?.mapNotNull { it as? String }?.toMutableSet() ?: mutableSetOf()
                        currentReadBy.add(normalizedReaderEmail)
                        doc.reference.update("readBy", currentReadBy.toList()).await()
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error marking direct messages as read: ${e.message}", e)
        }
    }
    
    suspend fun markContractorLandlordMessagesAsRead(messageIds: List<String>, readerEmail: String) {
        if (!isAvailable()) return
        val normalizedReaderEmail = readerEmail.trim().lowercase()
        try {
            messageIds.forEach { messageId ->
                db?.collection("contractor_landlord_messages")
                    ?.document(messageId)
                    ?.get()
                    ?.await()
                    ?.let { doc ->
                        val currentReadBy = (doc.get("readBy") as? List<*>)?.mapNotNull { it as? String }?.toMutableSet() ?: mutableSetOf()
                        currentReadBy.add(normalizedReaderEmail)
                        doc.reference.update("readBy", currentReadBy.toList()).await()
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error marking contractor-landlord messages as read: ${e.message}", e)
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
    
    // Job Invitations
    suspend fun saveJobInvitation(invitation: JobInvitation) {
        if (!isAvailable()) return
        val invitationMap = mapOf(
            "id" to invitation.id,
            "ticketId" to invitation.ticketId,
            "contractorId" to invitation.contractorId,
            "contractorEmail" to invitation.contractorEmail.trim().lowercase(),
            "landlordEmail" to invitation.landlordEmail.trim().lowercase(),
            "invitedAt" to invitation.invitedAt,
            "status" to invitation.status.name
        )
        try {
            db?.collection("jobInvitations")
                ?.document(invitation.id)
                ?.set(invitationMap)
                ?.await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error saving job invitation: ${e.message}", e)
        }
    }
    
    suspend fun getJobInvitations(contractorId: String): List<JobInvitation> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("jobInvitations")
                ?.whereEqualTo("contractorId", contractorId)
                ?.whereEqualTo("status", InvitationStatus.PENDING.name)
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toJobInvitation() }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting job invitations: ${e.message}", e)
            emptyList()
        }
    }
    
    fun observeJobInvitations(contractorId: String): Flow<List<JobInvitation>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        android.util.Log.d("FirebaseRepository", "Observing job invitations for contractorId: $contractorId")
        
        val listener = db?.collection("jobInvitations")
            ?.whereEqualTo("contractorId", contractorId)
            ?.whereEqualTo("status", InvitationStatus.PENDING.name)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing job invitations: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val invitations = snapshot?.documents?.mapNotNull { it.toJobInvitation() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${invitations.size} job invitations for contractorId: $contractorId")
                trySend(invitations)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Observe job invitations by contractor email (more reliable than ID)
    fun observeJobInvitationsByEmail(contractorEmail: String): Flow<List<JobInvitation>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedEmail = contractorEmail.trim().lowercase()
        android.util.Log.d("FirebaseRepository", "Observing job invitations for contractorEmail: $normalizedEmail")
        
        val listener = db?.collection("jobInvitations")
            ?.whereEqualTo("contractorEmail", normalizedEmail)
            ?.whereEqualTo("status", InvitationStatus.PENDING.name)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing job invitations by email: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val invitations = snapshot?.documents?.mapNotNull { it.toJobInvitation() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${invitations.size} job invitations for contractorEmail: $normalizedEmail")
                trySend(invitations)
            }
        
        awaitClose { listener?.remove() }
    }
    
    suspend fun updateInvitationStatus(invitationId: String, status: InvitationStatus) {
        if (!isAvailable()) return
        try {
            db?.collection("jobInvitations")
                ?.document(invitationId)
                ?.update("status", status.name)
                ?.await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error updating invitation status: ${e.message}", e)
        }
    }
    
    suspend fun getAcceptedInvitationsByTicketId(ticketId: String): List<JobInvitation> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("jobInvitations")
                ?.whereEqualTo("ticketId", ticketId)
                ?.whereEqualTo("status", InvitationStatus.ACCEPTED.name)
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toJobInvitation() }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting accepted invitations by ticketId: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun getAcceptedInvitationsByLandlord(landlordEmail: String): List<JobInvitation> {
        if (!isAvailable()) return emptyList()
        return try {
            val normalizedEmail = landlordEmail.trim().lowercase()
            val snapshot = db?.collection("jobInvitations")
                ?.whereEqualTo("landlordEmail", normalizedEmail)
                ?.whereEqualTo("status", InvitationStatus.ACCEPTED.name)
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toJobInvitation() }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting accepted invitations by landlord: ${e.message}", e)
            emptyList()
        }
    }
    
    fun observeAcceptedInvitationsByLandlord(landlordEmail: String): Flow<List<JobInvitation>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedEmail = landlordEmail.trim().lowercase()
        val listener = db?.collection("jobInvitations")
            ?.whereEqualTo("landlordEmail", normalizedEmail)
            ?.whereEqualTo("status", InvitationStatus.ACCEPTED.name)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing accepted invitations: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val invitations = snapshot?.documents?.mapNotNull { it.toJobInvitation() } ?: emptyList()
                trySend(invitations)
            }
        
        awaitClose { listener?.remove() }
    }
    
    // Observe all invitations (pending and accepted) for a landlord
    fun observeAllInvitationsByLandlord(landlordEmail: String): Flow<List<JobInvitation>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val normalizedEmail = landlordEmail.trim().lowercase()
        val listener = db?.collection("jobInvitations")
            ?.whereEqualTo("landlordEmail", normalizedEmail)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing all invitations: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val invitations = snapshot?.documents?.mapNotNull { it.toJobInvitation() } ?: emptyList()
                android.util.Log.d("FirebaseRepository", "Received ${invitations.size} invitations for landlord $normalizedEmail")
                trySend(invitations)
            }
        
        awaitClose { listener?.remove() }
    }
    
    suspend fun getInvitationsByTicket(ticketId: String): List<JobInvitation> {
        if (!isAvailable()) return emptyList()
        return try {
            val snapshot = db?.collection("jobInvitations")
                ?.whereEqualTo("ticketId", ticketId)
                ?.get()
                ?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { it.toJobInvitation() }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error getting invitations by ticket: ${e.message}", e)
            emptyList()
        }
    }
    
    fun observeInvitationsByTicket(ticketId: String): Flow<List<JobInvitation>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = db?.collection("jobInvitations")
            ?.whereEqualTo("ticketId", ticketId)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "Error observing invitations by ticket: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val invitations = snapshot?.documents?.mapNotNull { it.toJobInvitation() } ?: emptyList()
                trySend(invitations)
            }
        
        awaitClose { listener?.remove() }
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toJobInvitation(): JobInvitation? {
        return try {
            JobInvitation(
                id = getString("id") ?: return null,
                ticketId = getString("ticketId") ?: "",
                contractorId = getString("contractorId") ?: "",
                contractorEmail = getString("contractorEmail") ?: "",
                landlordEmail = getString("landlordEmail") ?: "",
                invitedAt = getString("invitedAt") ?: "",
                status = try {
                    InvitationStatus.valueOf(getString("status") ?: "PENDING")
                } catch (e: Exception) {
                    InvitationStatus.PENDING
                }
            )
        } catch (e: Exception) {
            null
        }
    }
}

