package com.example.mvp.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.example.mvp.auth.FirebaseAuthManager
import com.example.mvp.data.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dataRepository = DataRepository(application)
    private val firebaseRepository = FirebaseRepository()
    private var useFirebase = false
    
    init {
        // Check if Firebase is initialized by trying to get Firestore instance
        useFirebase = try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // All tickets (from Firebase/DataStore - unfiltered)
    private val _allTickets = MutableStateFlow<List<Ticket>>(emptyList())
    
    // Expose all tickets for screens that need them (e.g., marketplace for contractors)
    val allTickets: StateFlow<List<Ticket>> = _allTickets.asStateFlow()
    
    // Filtered tickets - only tickets created by current user
    private val _filteredTickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _filteredTickets.asStateFlow()
    
    // Landlord-Tenant Connections
    private val _connections = MutableStateFlow<List<LandlordTenantConnection>>(emptyList())
    val connections: StateFlow<List<LandlordTenantConnection>> = _connections.asStateFlow()
    
    // Direct Messages
    private val _directMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val directMessages: StateFlow<List<DirectMessage>> = _directMessages.asStateFlow()
    
    // Contractor-Landlord Messages
    private val _contractorLandlordMessages = MutableStateFlow<List<ContractorLandlordMessage>>(emptyList())
    val contractorLandlordMessages: StateFlow<List<ContractorLandlordMessage>> = _contractorLandlordMessages.asStateFlow()
    
    private val _contractors = MutableStateFlow(MockData.mockContractors)
    val contractors: StateFlow<List<Contractor>> = _contractors.asStateFlow()

    // All jobs (for contractors to see all)
    private val _allJobs = MutableStateFlow<List<Job>>(emptyList())
    
    // Filtered jobs based on user role
    private val _filteredJobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _filteredJobs.asStateFlow()
    
    // Job Applications
    private val _jobApplications = MutableStateFlow<List<JobApplication>>(emptyList())
    val jobApplications: StateFlow<List<JobApplication>> = _jobApplications.asStateFlow()
    
    // Users cache for location lookups
    private val _usersCache = MutableStateFlow<Map<String, User>>(emptyMap())
    
    suspend fun getUserByEmail(email: String): User? {
        if (_usersCache.value.containsKey(email)) {
            return _usersCache.value[email]
        }
        if (useFirebase) {
            val user = firebaseRepository.getUserByEmail(email)
            if (user != null) {
                _usersCache.value = _usersCache.value + (email to user)
            }
            return user
        }
        return null
    }
    
    init {
        // Update filtered tickets when user or all tickets change
        viewModelScope.launch {
            combine(
                _currentUser,
                _allTickets,
                _connections
            ) { user, allTickets, connections ->
                if (user != null) {
                    when (user.role) {
                        UserRole.TENANT -> {
                            // Tenants see only their own tickets
                            allTickets.filter { 
                                it.submittedBy == user.email && it.submittedByRole == user.role
                            }
                        }
                        UserRole.LANDLORD -> {
                            // Landlords see tickets from their connected tenants only
                            val connectedTenantEmails = connections
                                .filter { it.status == ConnectionStatus.CONNECTED }
                                .map { it.tenantEmail }
                            allTickets.filter { 
                                it.submittedByRole == UserRole.TENANT && 
                                it.submittedBy in connectedTenantEmails
                            }
                        }
                        UserRole.CONTRACTOR -> {
                            // Contractors see tickets assigned to them or unassigned
                            val contractorId = getContractorIdForUser(user)
                            allTickets.filter { 
                                val isAssignedToMe = it.assignedTo == contractorId || it.assignedContractor == contractorId
                                val isUnassigned = it.assignedTo == null && it.status == TicketStatus.SUBMITTED
                                isAssignedToMe || isUnassigned
                            }
                        }
                    }
                } else {
                    emptyList()
                }
            }.collect { filtered ->
                _filteredTickets.value = filtered
            }
        }
        
        // Update filtered jobs when user or all jobs change
        viewModelScope.launch {
            combine(
                _currentUser,
                _allJobs
            ) { user, allJobs ->
                if (user != null && user.role == UserRole.CONTRACTOR) {
                    val contractorId = getContractorIdForUser(user)
                    allJobs.filter { it.contractorId == contractorId }
                } else {
                    emptyList()
                }
            }.collect { filtered ->
                _filteredJobs.value = filtered
            }
        }
        
        // Load saved user and data
        viewModelScope.launch {
            if (useFirebase) {
                // Try Firebase Auth first
                try {
                    val currentFirebaseUser = FirebaseAuthManager.getCurrentUser()
                    if (currentFirebaseUser != null) {
                        val userId = currentFirebaseUser.uid
                        val user = firebaseRepository.getUser(userId)
                        if (user != null) {
                            _currentUser.value = user
                            loadUserData(user)
                        }
                    }
                } catch (e: Exception) {
                    // Firebase not initialized
                }
            } else {
                // Use DataStore
                val savedUser = dataRepository.getCurrentUser()
                if (savedUser != null) {
                    _currentUser.value = savedUser
                    loadUserData(savedUser)
                } else {
                    // Check if user is already logged in (Firebase persistence)
                    try {
                        val currentFirebaseUser = FirebaseAuthManager.getCurrentUser()
                        if (currentFirebaseUser != null) {
                            val user = User(
                                email = currentFirebaseUser.email ?: "",
                                role = UserRole.TENANT, // Default, can be enhanced
                                name = currentFirebaseUser.displayName ?: currentFirebaseUser.email?.split("@")?.first() ?: "User"
                            )
                            _currentUser.value = user
                            dataRepository.saveCurrentUser(user)
                            loadUserData(user)
                        }
                    } catch (e: Exception) {
                        // Firebase not initialized - continue without pre-login
                    }
                }
            }
        }
        
        // Set up real-time listeners if using Firebase
        if (useFirebase) {
            viewModelScope.launch {
                firebaseRepository.observeTickets().collect { tickets ->
                    // Remove duplicates by ID (in case of any race conditions)
                    val uniqueTickets = tickets.distinctBy { it.id }
                    _allTickets.value = uniqueTickets
                }
            }
            
            viewModelScope.launch {
                firebaseRepository.observeJobs().collect { jobs ->
                    // Remove duplicates by ID
                    val uniqueJobs = jobs.distinctBy { it.id }
                    _allJobs.value = uniqueJobs
                }
            }
            
            // Set up connections listener - restart when user changes
            viewModelScope.launch {
                _currentUser
                    .flatMapLatest { user ->
                        if (user != null && (user.role == UserRole.LANDLORD || user.role == UserRole.TENANT)) {
                            // First emit initial connections, then observe
                            flow {
                                // Load and emit initial connections immediately
                                emit(firebaseRepository.getConnectionsForUser(user.email, user.role))
                                // Then continue with real-time updates
                                firebaseRepository.observeConnections(user.email, user.role).collect { 
                                    emit(it)
                                }
                            }
                        } else {
                            flowOf(emptyList())
                        }
                    }
                    .collect { connections ->
                        _connections.value = connections
                    }
            }
        }
    }

    var rememberMe by mutableStateOf(false)
        private set

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun login(email: String, password: String, role: UserRole, remember: Boolean) {
        viewModelScope.launch {
            _authError.value = null
            
            // For testing: If email/password is empty, always use simulation mode
            val useTestMode = email.isEmpty() || password.isEmpty()
            
            if (useTestMode) {
                // Test mode - simulate login (allows testing without real credentials)
                val user = User(
                    email = email.ifEmpty { "user@example.com" },
                    role = role,
                    name = email.ifEmpty { "User" }.split("@").first()
                )
                _currentUser.value = user
                if (useFirebase) {
                    firebaseRepository.saveUser(user)
                } else {
                    dataRepository.saveCurrentUser(user)
                }
                rememberMe = remember
                loadUserData(user)
                return@launch
            }
            
            // If Firebase is not configured, use simulation
            if (!useFirebase) {
                val user = User(
                    email = email,
                    role = role,
                    name = email.split("@").first()
                )
                _currentUser.value = user
                dataRepository.saveCurrentUser(user)
                rememberMe = remember
                loadUserData(user)
                return@launch
            }
            
            // Try Firebase authentication with real credentials
            val firebaseResult = FirebaseAuthManager.signInWithEmailAndPassword(email, password)
            
            firebaseResult.fold(
                onSuccess = { firebaseUser ->
                    // Firebase authentication successful
                    val user = User(
                        email = firebaseUser.email ?: email,
                        role = role,
                        name = firebaseUser.displayName ?: email.split("@").first()
                    )
                    _currentUser.value = user
                    firebaseRepository.saveUser(user)
                    rememberMe = remember
                    loadUserData(user)
                },
                onFailure = { error ->
                    // Check if this is a credential error
                    val isCredentialError = error.message?.contains("credential", ignoreCase = true) == true ||
                                          error.message?.contains("invalid", ignoreCase = true) == true ||
                                          error.message?.contains("user-not-found", ignoreCase = true) == true ||
                                          error.message?.contains("wrong-password", ignoreCase = true) == true ||
                                          error.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true
                    
                    if (error.message?.contains("FirebaseApp") == true || 
                        error.message?.contains("not initialized") == true) {
                        // Firebase not configured - use simulation
                        val user = User(
                            email = email,
                            role = role,
                            name = email.split("@").first()
                        )
                        _currentUser.value = user
                        dataRepository.saveCurrentUser(user)
                        rememberMe = remember
                        loadUserData(user)
                    } else if (isCredentialError) {
                        // Real credential error - suggest creating account
                        _authError.value = "Invalid email or password. Please create an account first."
                    } else {
                        // Other Firebase error
                        _authError.value = error.message ?: "Authentication failed"
                    }
                }
            )
        }
    }
    
    private suspend fun loadUserData(user: User) {
        if (useFirebase) {
            // Load from Firebase
            _allTickets.value = firebaseRepository.getAllTickets()
            _allJobs.value = firebaseRepository.getAllJobs()
            _contractors.value = firebaseRepository.getAllContractors()
        } else {
            // Load from DataStore
            _allTickets.value = dataRepository.getAllTickets()
            
            when (user.role) {
                UserRole.CONTRACTOR -> {
                    _allJobs.value = dataRepository.getAllJobs()
                }
                else -> {
                    _allJobs.value = emptyList()
                }
            }
        }
    }

    fun createAccount(name: String, email: String, password: String, role: UserRole, address: String?, city: String?, state: String?, companyName: String?) {
        viewModelScope.launch {
            _authError.value = null
            val firebaseResult = FirebaseAuthManager.createUserWithEmailAndPassword(email, password)
            
            firebaseResult.fold(
                onSuccess = { firebaseUser ->
                    val user = User(
                        email = firebaseUser.email ?: email,
                        role = role,
                        name = name,
                        address = address,
                        city = city,
                        state = state,
                        companyName = companyName
                    )
                    _currentUser.value = user
                    if (useFirebase) {
                        firebaseRepository.saveUser(user)
                        // If contractor, also create/update contractor entry
                        if (role == UserRole.CONTRACTOR) {
                            val contractorId = "contractor-${user.email.replace("@", "-").replace(".", "-")}"
                            val contractor = Contractor(
                                id = contractorId,
                                name = name,
                                company = companyName ?: name,
                                specialization = emptyList(),
                                rating = 0f,
                                distance = 0f,
                                preferred = false,
                                completedJobs = 0,
                                email = user.email,
                                city = city,
                                state = state
                            )
                            firebaseRepository.saveContractor(contractor)
                        }
                    } else {
                        dataRepository.saveCurrentUser(user)
                    }
                    loadUserData(user)
                },
                onFailure = { error ->
                    if (error.message?.contains("FirebaseApp") == true) {
                        // Firebase not configured - simulate
                        val user = User(
                            email = email,
                            role = role,
                            name = name,
                            address = address,
                            city = city,
                            state = state,
                            companyName = companyName
                        )
                        _currentUser.value = user
                        if (useFirebase) {
                            firebaseRepository.saveUser(user)
                            // If contractor, also create/update contractor entry
                            if (role == UserRole.CONTRACTOR) {
                                val contractorId = "contractor-${user.email.replace("@", "-").replace(".", "-")}"
                                val contractor = Contractor(
                                    id = contractorId,
                                    name = name,
                                    company = companyName ?: name,
                                    specialization = emptyList(),
                                    rating = 0f,
                                    distance = 0f,
                                    preferred = false,
                                    completedJobs = 0,
                                    email = user.email,
                                    city = city,
                                    state = state
                                )
                                firebaseRepository.saveContractor(contractor)
                            }
                        } else {
                            dataRepository.saveCurrentUser(user)
                        }
                        loadUserData(user)
                    } else {
                        _authError.value = error.message ?: "Account creation failed"
                    }
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            FirebaseAuthManager.signOut()
            _currentUser.value = null
            _allTickets.value = emptyList()
            _allJobs.value = emptyList()
        }
    }


    fun addTicket(ticket: Ticket) {
        viewModelScope.launch {
            if (useFirebase) {
                // Save to Firebase
                firebaseRepository.saveTicket(ticket)
                // Update local state immediately for instant UI feedback
                // The real-time listener will also update, but distinctBy prevents duplicates
                val currentTickets = _allTickets.value
                if (currentTickets.none { it.id == ticket.id }) {
                    _allTickets.value = currentTickets + ticket
                }
            } else {
                // Save to DataStore
                val updatedTickets = _allTickets.value + ticket
                _allTickets.value = updatedTickets
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    // Save all tickets (shared across users)
                    dataRepository.saveTickets(currentUser.email, updatedTickets)
                }
            }
        }
    }

    fun updateTicket(id: String, updates: Ticket) {
        viewModelScope.launch {
            if (useFirebase) {
                // Save to Firebase - the real-time listener will update _allTickets automatically
                firebaseRepository.saveTicket(updates)
                // Don't update local state here to avoid duplicates - let the listener handle it
            } else {
                // Save to DataStore
                val updatedTickets = _allTickets.value.map { if (it.id == id) updates else it }
                _allTickets.value = updatedTickets
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    // Save all tickets (shared across users)
                    dataRepository.saveTickets(currentUser.email, updatedTickets)
                }
            }
        }
    }

    fun applyToJob(ticketId: String, contractorId: String, contractorName: String, contractorEmail: String) {
        viewModelScope.launch {
            val ticket = _allTickets.value.find { it.id == ticketId }
            if (ticket != null && ticket.assignedTo == null) {
                // Check if already applied
                val existingApplication = _jobApplications.value.find { 
                    it.ticketId == ticketId && it.contractorId == contractorId 
                }
                if (existingApplication == null && useFirebase) {
                    val application = JobApplication(
                        id = "app-${ticketId}-${contractorId}-${System.currentTimeMillis()}",
                        ticketId = ticketId,
                        contractorId = contractorId,
                        contractorName = contractorName,
                        contractorEmail = contractorEmail,
                        appliedAt = com.example.mvp.utils.DateUtils.getCurrentDateTimeString(),
                        status = ApplicationStatus.PENDING
                    )
                    firebaseRepository.saveJobApplication(application)
                }
            }
        }
    }
    
    fun startObservingJobApplications(ticketId: String) {
        viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.observeJobApplications(ticketId).collect { applications ->
                    _jobApplications.value = _jobApplications.value.filter { it.ticketId != ticketId } + applications
                }
            }
        }
    }
    
    fun assignContractor(ticketId: String, contractorId: String) {
        viewModelScope.launch {
            val ticket = _allTickets.value.find { it.id == ticketId }
            if (ticket != null && ticket.assignedTo == null) {
                // Update application status to ACCEPTED if it exists
                val application = _jobApplications.value.find { 
                    it.ticketId == ticketId && it.contractorId == contractorId 
                }
                if (application != null && useFirebase) {
                    firebaseRepository.updateApplicationStatus(application.id, ApplicationStatus.ACCEPTED)
                    // Reject all other applications for this ticket
                    _jobApplications.value.filter { 
                        it.ticketId == ticketId && it.contractorId != contractorId 
                    }.forEach { otherApp ->
                        firebaseRepository.updateApplicationStatus(otherApp.id, ApplicationStatus.REJECTED)
                    }
                }
                
                updateTicket(
                    ticketId,
                    ticket.copy(
                        assignedTo = contractorId,
                        assignedContractor = contractorId,
                        status = TicketStatus.ASSIGNED
                    )
                )
                // Create job if it doesn't exist
                val existingJob = _allJobs.value.find { it.ticketId == ticketId }
                if (existingJob == null) {
                    val newJob = Job(
                        id = "job${_allJobs.value.size + 1}",
                        ticketId = ticketId,
                        contractorId = contractorId,
                        propertyAddress = "123 Main St, Apt 4B",
                        issueType = ticket.category,
                        date = com.example.mvp.utils.DateUtils.getCurrentDateString(),
                        status = "assigned"
                    )
                    if (useFirebase) {
                        firebaseRepository.saveJob(newJob)
                        // Real-time listener will update _allJobs
                    } else {
                        _allJobs.value = _allJobs.value + newJob
                        val currentUser = _currentUser.value
                        if (currentUser != null) {
                            dataRepository.saveJobs(currentUser.email, _allJobs.value)
                        }
                    }
                }
            }
        }
    }

    fun completeJob(jobId: String) {
        viewModelScope.launch {
            val job = _allJobs.value.find { it.id == jobId }
            if (job != null) {
                val updatedJob = job.copy(status = "completed")
                if (useFirebase) {
                    firebaseRepository.saveJob(updatedJob)
                    // Real-time listener will update _allJobs
                } else {
                    _allJobs.value = _allJobs.value.map { if (it.id == jobId) updatedJob else it }
                    val currentUser = _currentUser.value
                    if (currentUser != null) {
                        dataRepository.saveJobs(currentUser.email, _allJobs.value)
                    }
                }
                
                val ticket = _allTickets.value.find { it.id == job.ticketId }
                if (ticket != null) {
                    // Update ticket status - this will save all tickets
                    updateTicket(
                        ticket.id,
                        ticket.copy(status = TicketStatus.COMPLETED, completedDate = com.example.mvp.utils.DateUtils.getCurrentDateString())
                    )
                }
            }
        }
    }

    fun addRating(jobId: String, rating: Float) {
        viewModelScope.launch {
            val job = _allJobs.value.find { it.id == jobId }
            if (job != null) {
                val updatedJob = job.copy(rating = rating)
                if (useFirebase) {
                    firebaseRepository.saveJob(updatedJob)
                    // Real-time listener will update _allJobs
                } else {
                    _allJobs.value = _allJobs.value.map { if (it.id == jobId) updatedJob else it }
                    val currentUser = _currentUser.value
                    if (currentUser != null) {
                        dataRepository.saveJobs(currentUser.email, _allJobs.value)
                    }
                }
                
                // Also update the ticket rating
                val ticket = _allTickets.value.find { t -> t.id == job.ticketId }
                ticket?.let { t ->
                    updateTicket(t.id, t.copy(rating = rating))
                }
                
                // Update contractor rating - calculate average from all completed jobs
                val contractor = _contractors.value.find { it.id == job.contractorId }
                contractor?.let { c ->
                    val contractorJobs = _allJobs.value.filter { it.contractorId == c.id && it.rating != null }
                    val averageRating = if (contractorJobs.isNotEmpty()) {
                        contractorJobs.mapNotNull { it.rating }.average().toFloat()
                    } else {
                        rating // First rating
                    }
                    
                    val updatedContractor = c.copy(rating = averageRating)
                    if (useFirebase) {
                        firebaseRepository.saveContractor(updatedContractor)
                    } else {
                        _contractors.value = _contractors.value.map { if (it.id == c.id) updatedContractor else it }
                    }
                }
            }
        }
    }

    fun scheduleTicket(ticketId: String, date: String, time: String) {
        viewModelScope.launch {
            val ticket = _allTickets.value.find { it.id == ticketId }
            if (ticket != null) {
                val scheduledDateTime = "$date $time"
                updateTicket(
                    ticketId,
                    ticket.copy(
                        status = TicketStatus.SCHEDULED,
                        scheduledDate = scheduledDateTime
                    )
                )
            }
        }
    }
    
    fun addMessageToTicket(ticketId: String, message: Message) {
        viewModelScope.launch {
            val ticket = _allTickets.value.find { it.id == ticketId }
            if (ticket != null) {
                val updatedMessages = ticket.messages + message
                updateTicket(
                    ticketId,
                    ticket.copy(messages = updatedMessages)
                )
            }
        }
    }

    fun getContractorIdForUser(user: User?): String? {
        if (user?.role != UserRole.CONTRACTOR) return null
        // Try to match contractor by name or email
        return _contractors.value.find { 
            it.name.contains(user.name, ignoreCase = true) || 
            it.name.contains(user.email.split("@").first(), ignoreCase = true)
        }?.id ?: _contractors.value.firstOrNull()?.id
    }
    
    // Landlord-Tenant Connection functions
    fun requestTenantConnection(tenantEmail: String) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && currentUser.role == UserRole.LANDLORD) {
                if (useFirebase) {
                    firebaseRepository.requestConnection(
                        landlordEmail = currentUser.email,
                        tenantEmail = tenantEmail,
                        requestedBy = currentUser.email
                    )
                }
            }
        }
    }
    
    fun confirmConnection(connectionId: String, accept: Boolean) {
        viewModelScope.launch {
            if (useFirebase) {
                val status = if (accept) ConnectionStatus.CONNECTED else ConnectionStatus.REJECTED
                firebaseRepository.updateConnectionStatus(connectionId, status)
            }
        }
    }
    
    fun getConnectedTenants(): List<String> {
        val currentUser = _currentUser.value
        return if (currentUser?.role == UserRole.LANDLORD) {
            _connections.value
                .filter { it.status == ConnectionStatus.CONNECTED }
                .map { it.tenantEmail }
        } else {
            emptyList()
        }
    }
    
    fun getLandlordEmail(): String? {
        val currentUser = _currentUser.value
        return if (currentUser?.role == UserRole.TENANT) {
            _connections.value
                .filter { it.status == ConnectionStatus.CONNECTED }
                .firstOrNull()
                ?.landlordEmail
        } else {
            null
        }
    }
    
    // Direct Messaging functions
    fun sendDirectMessage(tenantEmail: String, messageText: String) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && useFirebase) {
                val landlordEmail = if (currentUser.role == UserRole.LANDLORD) {
                    currentUser.email
                } else {
                    getLandlordEmail() ?: return@launch
                }
                val tenant = if (currentUser.role == UserRole.TENANT) {
                    currentUser.email
                } else {
                    tenantEmail
                }
                
                val message = DirectMessage(
                    id = "msg-${System.currentTimeMillis()}",
                    landlordEmail = landlordEmail,
                    tenantEmail = tenant,
                    senderEmail = currentUser.email,
                    senderName = currentUser.name,
                    text = messageText,
                    timestamp = com.example.mvp.utils.DateUtils.getCurrentDateTimeString()
                )
                firebaseRepository.sendDirectMessage(message)
            }
        }
    }
    
    fun startObservingDirectMessages(tenantEmail: String) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && useFirebase) {
                val landlordEmail = if (currentUser.role == UserRole.LANDLORD) {
                    currentUser.email
                } else {
                    getLandlordEmail() ?: return@launch
                }
                val tenant = if (currentUser.role == UserRole.TENANT) {
                    currentUser.email
                } else {
                    tenantEmail
                }
                
                firebaseRepository.observeDirectMessages(landlordEmail, tenant).collect { messages ->
                    _directMessages.value = messages
                }
            }
        }
    }
    
    fun clearDirectMessages() {
        _directMessages.value = emptyList()
    }
    
    fun refreshConnections() {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && (currentUser.role == UserRole.LANDLORD || currentUser.role == UserRole.TENANT)) {
                if (useFirebase) {
                    _connections.value = firebaseRepository.getConnectionsForUser(currentUser.email, currentUser.role)
                }
            }
        }
    }
    
    // Contractor-Landlord Messaging functions
    fun sendContractorLandlordMessage(ticketId: String, otherPartyEmail: String, messageText: String) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && useFirebase) {
                val ticket = _allTickets.value.find { it.id == ticketId }
                val contractorEmail = if (currentUser.role == UserRole.CONTRACTOR) {
                    currentUser.email
                } else {
                    // For landlord, we need to get contractor email from ticket
                    // Since we only have contractor ID, we'll use a workaround
                    // In production, contractors should have email stored
                    ticket?.assignedTo ?: ticket?.assignedContractor ?: ""
                }
                
                val landlordEmail = if (currentUser.role == UserRole.LANDLORD) {
                    currentUser.email
                } else {
                    // For contractor, use the provided landlord email
                    otherPartyEmail
                }
                
                val message = ContractorLandlordMessage(
                    id = "cl-msg-${System.currentTimeMillis()}",
                    ticketId = ticketId,
                    contractorEmail = contractorEmail,
                    landlordEmail = landlordEmail,
                    senderEmail = currentUser.email,
                    senderName = currentUser.name,
                    text = messageText,
                    timestamp = com.example.mvp.utils.DateUtils.getCurrentDateTimeString()
                )
                firebaseRepository.sendContractorLandlordMessage(message)
            }
        }
    }
    
    fun startObservingContractorLandlordMessages(ticketId: String) {
        viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.observeContractorLandlordMessages(ticketId).collect { messages ->
                    _contractorLandlordMessages.value = messages
                }
            }
        }
    }
    
    fun clearContractorLandlordMessages() {
        _contractorLandlordMessages.value = emptyList()
    }
    
    fun getLandlordEmailForTicket(ticketId: String): String? {
        val ticket = _allTickets.value.find { it.id == ticketId }
        // Find the landlord connected to the tenant who submitted the ticket
        if (ticket != null && ticket.submittedByRole == UserRole.TENANT) {
            val connection = _connections.value.find { 
                it.tenantEmail == ticket.submittedBy && it.status == ConnectionStatus.CONNECTED
            }
            return connection?.landlordEmail
        }
        return null
    }
    
    fun getContractorEmailForTicket(ticketId: String): String? {
        val ticket = _allTickets.value.find { it.id == ticketId }
        if (ticket != null && ticket.assignedTo != null) {
            // We need to get contractor email, but we only have contractor ID
            // For now, return null and handle in UI
            return null
        }
        return null
    }
}

