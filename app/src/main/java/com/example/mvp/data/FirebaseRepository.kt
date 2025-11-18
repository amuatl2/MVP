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
            db?.collection("users")
                ?.document(userId)
                ?.set(
                    mapOf(
                        "email" to user.email,
                        "role" to user.role.name,
                        "name" to user.name
                    )
                )
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
                    name = doc.getString("name") ?: ""
                )
            } else {
                null
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
    
    // Contractors collection (read-only, managed separately)
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
                completedJobs = (getLong("completedJobs") ?: 0L).toInt()
            )
        } catch (e: Exception) {
            null
        }
    }
}

