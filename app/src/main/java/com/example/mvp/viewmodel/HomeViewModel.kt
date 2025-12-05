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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
    private var directMessagesJob: kotlinx.coroutines.Job? = null
    
    // Contractor-Landlord Messages
    private val _contractorLandlordMessages = MutableStateFlow<List<ContractorLandlordMessage>>(emptyList())
    val contractorLandlordMessages: StateFlow<List<ContractorLandlordMessage>> = _contractorLandlordMessages.asStateFlow()
    private var contractorLandlordMessagesJob: kotlinx.coroutines.Job? = null
    
    // All contractor-landlord messages for a contractor (for messages list)
    private val _allContractorLandlordMessages = MutableStateFlow<List<ContractorLandlordMessage>>(emptyList())
    val allContractorLandlordMessages: StateFlow<List<ContractorLandlordMessage>> = _allContractorLandlordMessages.asStateFlow()
    private var allContractorLandlordMessagesJob: kotlinx.coroutines.Job? = null
    
    // All contractor-landlord messages for a landlord (for messages list)
    private val _allLandlordContractorMessages = MutableStateFlow<List<ContractorLandlordMessage>>(emptyList())
    val allLandlordContractorMessages: StateFlow<List<ContractorLandlordMessage>> = _allLandlordContractorMessages.asStateFlow()
    private var allLandlordContractorMessagesJob: kotlinx.coroutines.Job? = null
    
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
    
    // Job Invitations (for contractors)
    private val _jobInvitations = MutableStateFlow<List<JobInvitation>>(emptyList())
    val jobInvitations: StateFlow<List<JobInvitation>> = _jobInvitations.asStateFlow()
    private var invitationsJob: kotlinx.coroutines.Job? = null
    
    // Accepted Invitations (for landlords to track Assignment Pending)
    private val _acceptedInvitations = MutableStateFlow<List<JobInvitation>>(emptyList())
    val acceptedInvitations: StateFlow<List<JobInvitation>> = _acceptedInvitations.asStateFlow()
    private var acceptedInvitationsJob: kotlinx.coroutines.Job? = null
    
    // All Invitations (pending and accepted) for landlords to track Assignment Pending
    private val _allInvitations = MutableStateFlow<List<JobInvitation>>(emptyList())
    val allInvitations: StateFlow<List<JobInvitation>> = _allInvitations.asStateFlow()
    private var allInvitationsJob: kotlinx.coroutines.Job? = null
    
    // Invitations by ticket (for landlords to see invitation status)
    private val _invitationsByTicket = MutableStateFlow<Map<String, List<JobInvitation>>>(emptyMap())
    val invitationsByTicket: StateFlow<Map<String, List<JobInvitation>>> = _invitationsByTicket.asStateFlow()
    private val invitationsByTicketJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    
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
                                .filter { 
                                    it.status == ConnectionStatus.CONNECTED && 
                                    it.landlordEmail == user.email
                                }
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
                    android.util.Log.d("HomeViewModel", "Filtering jobs for contractor. contractorId=$contractorId, totalJobs=${allJobs.size}")
                    val filtered = allJobs.filter { job ->
                        val matches = job.contractorId == contractorId
                        if (!matches) {
                            android.util.Log.d("HomeViewModel", "Job ${job.id} filtered out: contractorId=${job.contractorId} != $contractorId, status=${job.status}")
                        }
                        matches
                    }
                    android.util.Log.d("HomeViewModel", "Filtered to ${filtered.size} jobs for contractor. Jobs: ${filtered.map { "${it.id}(${it.status})" }}")
                    filtered
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
                    android.util.Log.d("HomeViewModel", "Observed ${uniqueJobs.size} jobs from Firestore")
                    _allJobs.value = uniqueJobs
                }
            }
            
            viewModelScope.launch {
                firebaseRepository.observeContractors().collect { contractors ->
                    // Remove duplicates by ID
                    val uniqueContractors = contractors.distinctBy { it.id }
                    _contractors.value = uniqueContractors
                }
            }
            
            // Set up connections listener - restart when user changes
            // Contractors also need connections to find landlord emails for tenants
            viewModelScope.launch {
                _currentUser
                    .flatMapLatest { user ->
                        if (user != null) {
                            when (user.role) {
                                UserRole.LANDLORD -> {
                                    // Landlords observe their own connections
                                    flow {
                                        emit(firebaseRepository.getConnectionsForUser(user.email, user.role))
                                        firebaseRepository.observeConnections(user.email, user.role).collect { 
                                            emit(it)
                                        }
                                    }
                                }
                                UserRole.TENANT -> {
                                    // Tenants observe their own connections
                                    flow {
                                        emit(firebaseRepository.getConnectionsForUser(user.email, user.role))
                                        firebaseRepository.observeConnections(user.email, user.role).collect { 
                                            emit(it)
                                        }
                                    }
                                }
                                UserRole.CONTRACTOR -> {
                                    // Contractors need ALL connections to find landlord emails for any tenant
                                    // We'll observe all connections and filter in memory
                                    flow {
                                        // Load all connections initially
                                        val allConnections = try {
                                            firebaseRepository.getAllConnections()
                                        } catch (e: Exception) {
                                            android.util.Log.e("HomeViewModel", "Error loading all connections: ${e.message}", e)
                                            emptyList()
                                        }
                                        emit(allConnections)
                                        // Then observe all connections for real-time updates
                                        firebaseRepository.observeAllConnections().collect {
                                            emit(it)
                                        }
                                    }
                                }
                            }
                        } else {
                            flowOf(emptyList())
                        }
                    }
                    .collect { connections ->
                        _connections.value = connections
                        android.util.Log.d("HomeViewModel", "Connections updated: ${connections.size} connections")
                        connections.forEach { conn ->
                            android.util.Log.d("HomeViewModel", "  Connection: tenant=${conn.tenantEmail}, landlord=${conn.landlordEmail}, status=${conn.status}")
                        }
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
                    // Firebase authentication successful - fetch user from Firestore to get their actual role
                    val userId = firebaseUser.uid
                    val storedUser = firebaseRepository.getUser(userId)
                    
                    if (storedUser != null) {
                        // User exists in Firestore - verify role matches
                        if (storedUser.role != role) {
                            _authError.value = "This email is registered as a ${storedUser.role.name.lowercase().replaceFirstChar { it.uppercase() }}. Please sign in with the correct role."
                            return@launch
                        }
                        // Role matches - use stored user data
                        _currentUser.value = storedUser
                        rememberMe = remember
                        loadUserData(storedUser)
                    } else {
                        // User doesn't exist in Firestore yet - this shouldn't happen, but handle it
                        _authError.value = "Account not found. Please create an account first."
                    }
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
            
            // If contractor, recalculate rating from tickets AFTER contractors are loaded
            if (user.role == UserRole.CONTRACTOR) {
                val contractorId = getContractorIdForUser(user)
                if (contractorId != null) {
                    android.util.Log.d("HomeViewModel", "Contractor signed in, recalculating rating for $contractorId")
                    // Small delay to ensure tickets are fully loaded
                    kotlinx.coroutines.delay(200)
                    updateContractorRatingFromTickets(contractorId)
                } else {
                    android.util.Log.w("HomeViewModel", "Could not find contractor ID for user ${user.email}")
                }
            }
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
            
            // Check if email already exists in Firestore first
            if (useFirebase) {
                val existingUser = firebaseRepository.getUserByEmail(email)
                if (existingUser != null) {
                    _authError.value = "This email address is already registered. Please sign in instead."
                    return@launch
                }
            }
            
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
                        // Check if error is about email already in use
                        val errorMessage = error.message ?: ""
                        if (errorMessage.contains("already in use", ignoreCase = true) || 
                            errorMessage.contains("email-already-in-use", ignoreCase = true)) {
                            // Email exists in Firebase Auth but not in Firestore
                            // Try to sign in with the provided password to complete account setup
                            val signInResult = FirebaseAuthManager.signInWithEmailAndPassword(email, password)
                            signInResult.fold(
                                onSuccess = { firebaseUser ->
                                    // Sign in successful - create/update Firestore user document
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
                                    }
                                    loadUserData(user)
                                },
                                onFailure = { signInError ->
                                    // Sign in failed - wrong password or other issue
                                    val signInErrorMessage = signInError.message ?: ""
                                    if (signInErrorMessage.contains("wrong-password", ignoreCase = true) ||
                                        signInErrorMessage.contains("invalid", ignoreCase = true) ||
                                        signInErrorMessage.contains("credential", ignoreCase = true)) {
                                        _authError.value = "This email is already registered with a different password. Please sign in instead."
                                    } else {
                                        _authError.value = "This email address is already registered. Please sign in instead."
                                    }
                                }
                            )
                        } else {
                            _authError.value = errorMessage.ifEmpty { "Account creation failed" }
                        }
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
    
    fun markTicketAsViewedByLandlord(ticketId: String) {
        viewModelScope.launch {
            val ticket = _allTickets.value.find { it.id == ticketId }
            if (ticket != null && ticket.status == TicketStatus.SUBMITTED && !ticket.viewedByLandlord) {
                val updatedTicket = ticket.copy(viewedByLandlord = true)
                updateTicket(ticketId, updatedTicket)
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
            val currentUser = _currentUser.value
            android.util.Log.d("HomeViewModel", "assignContractor called: ticketId=$ticketId, contractorId=$contractorId")
            
            if (ticket == null) {
                android.util.Log.e("HomeViewModel", "Ticket not found: $ticketId")
                return@launch
            }
            
            if (ticket.assignedTo != null) {
                android.util.Log.e("HomeViewModel", "Ticket already assigned: $ticketId")
                return@launch
            }
            
            if (currentUser == null) {
                android.util.Log.e("HomeViewModel", "Current user is null")
                return@launch
            }
            
            if (!useFirebase) {
                android.util.Log.e("HomeViewModel", "Firebase not available")
                return@launch
            }
            
            // Get contractor email
            val contractor = _contractors.value.find { it.id == contractorId }
            if (contractor == null) {
                android.util.Log.e("HomeViewModel", "Contractor not found: $contractorId")
                return@launch
            }
            
            val contractorEmail = contractor.email
            if (contractorEmail == null || contractorEmail.isEmpty()) {
                android.util.Log.e("HomeViewModel", "Contractor email is null or empty: $contractorId")
                return@launch
            }
            
            android.util.Log.d("HomeViewModel", "Creating invitation: ticketId=$ticketId, contractorId=$contractorId, contractorEmail=$contractorEmail")
            
            // Create job invitation instead of directly assigning
            val invitation = JobInvitation(
                id = "invitation-${System.currentTimeMillis()}",
                ticketId = ticketId,
                contractorId = contractorId,
                contractorEmail = contractorEmail,
                landlordEmail = currentUser.email,
                invitedAt = com.example.mvp.utils.DateUtils.getCurrentDateTimeString(),
                status = InvitationStatus.PENDING
            )
            
            try {
                firebaseRepository.saveJobInvitation(invitation)
                android.util.Log.d("HomeViewModel", "Invitation saved successfully: ${invitation.id}")
                
                // Mark ticket as viewed by landlord when assigning contractor
                // This moves it from "Needs Assignment" to "Assignment Pending"
                if (!ticket.viewedByLandlord) {
                    val updatedTicket = ticket.copy(viewedByLandlord = true)
                    updateTicket(ticketId, updatedTicket)
                    android.util.Log.d("HomeViewModel", "Marked ticket $ticketId as viewed by landlord")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error saving invitation: ${e.message}", e)
            }
        }
    }
    
    fun startObservingInvitations(contractorId: String) {
        invitationsJob?.cancel()
        invitationsJob = null
        
        invitationsJob = viewModelScope.launch {
            val currentUser = _currentUser.value
            if (useFirebase && currentUser != null) {
                // Try to observe by email first (more reliable), then fallback to ID
                val contractor = _contractors.value.find { it.id == contractorId }
                val contractorEmail = contractor?.email ?: currentUser.email
                
                android.util.Log.d("HomeViewModel", "Starting to observe invitations for contractorId: $contractorId, email: $contractorEmail")
                
                // Prefer email-based observation as it's more reliable
                if (contractorEmail != null) {
                    firebaseRepository.observeJobInvitationsByEmail(contractorEmail).collect { invitations ->
                        android.util.Log.d("HomeViewModel", "Received ${invitations.size} invitations by email")
                        _jobInvitations.value = invitations
                    }
                } else {
                    // Fallback to ID only
                    firebaseRepository.observeJobInvitations(contractorId).collect { invitations ->
                        android.util.Log.d("HomeViewModel", "Received ${invitations.size} invitations by ID")
                        _jobInvitations.value = invitations
                    }
                }
            }
        }
    }
    
    fun startObservingAcceptedInvitations(landlordEmail: String) {
        acceptedInvitationsJob?.cancel()
        acceptedInvitationsJob = null
        
        acceptedInvitationsJob = viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.observeAcceptedInvitationsByLandlord(landlordEmail).collect { invitations ->
                    _acceptedInvitations.value = invitations
                }
            }
        }
    }
    
    // Start observing all invitations (pending and accepted) for a landlord
    fun startObservingAllInvitations(landlordEmail: String) {
        allInvitationsJob?.cancel()
        allInvitationsJob = null
        
        allInvitationsJob = viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.observeAllInvitationsByLandlord(landlordEmail).collect { invitations ->
                    _allInvitations.value = invitations
                }
            }
        }
    }
    
    fun startObservingInvitationsForTicket(ticketId: String) {
        invitationsByTicketJobs[ticketId]?.cancel()
        
        invitationsByTicketJobs[ticketId] = viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.observeInvitationsByTicket(ticketId).collect { invitations ->
                    _invitationsByTicket.value = _invitationsByTicket.value + (ticketId to invitations)
                }
            }
        }
    }
    
    fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            android.util.Log.d("HomeViewModel", "acceptInvitation called: invitationId=$invitationId")
            val invitation = _jobInvitations.value.find { it.id == invitationId }
            if (invitation == null) {
                android.util.Log.e("HomeViewModel", "Invitation not found: $invitationId")
                return@launch
            }
            
            if (!useFirebase) {
                android.util.Log.e("HomeViewModel", "Firebase not available")
                return@launch
            }
            
            android.util.Log.d("HomeViewModel", "Accepting invitation: $invitationId for ticket: ${invitation.ticketId}")
            
            // Update invitation status
            try {
                firebaseRepository.updateInvitationStatus(invitationId, InvitationStatus.ACCEPTED)
                android.util.Log.d("HomeViewModel", "Invitation status updated to ACCEPTED")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error updating invitation status: ${e.message}", e)
                return@launch
            }
            
            // Assign the ticket to the contractor
            val ticket = _allTickets.value.find { it.id == invitation.ticketId }
            if (ticket == null) {
                android.util.Log.e("HomeViewModel", "Ticket not found: ${invitation.ticketId}")
                return@launch
            }
            
            if (ticket.assignedTo != null) {
                android.util.Log.w("HomeViewModel", "Ticket already assigned: ${invitation.ticketId}")
            }
            
            try {
                updateTicket(
                    invitation.ticketId,
                    ticket.copy(
                        assignedTo = invitation.contractorId,
                        assignedContractor = invitation.contractorId,
                        status = TicketStatus.ASSIGNED
                    )
                )
                android.util.Log.d("HomeViewModel", "Ticket updated and assigned to contractor")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error updating ticket: ${e.message}", e)
            }
            
            // Create job if it doesn't exist
            val existingJob = _allJobs.value.find { it.ticketId == invitation.ticketId }
            if (existingJob == null) {
                val newJob = Job(
                    id = "job-${System.currentTimeMillis()}",
                    ticketId = invitation.ticketId,
                    contractorId = invitation.contractorId,
                    propertyAddress = ticket.title,
                    issueType = ticket.category,
                    date = com.example.mvp.utils.DateUtils.getCurrentDateString(),
                    status = "assigned"
                )
                android.util.Log.d("HomeViewModel", "Creating new job: ${newJob.id} for ticket: ${invitation.ticketId}, contractorId: ${invitation.contractorId}")
                
                if (useFirebase) {
                    try {
                        firebaseRepository.saveJob(newJob)
                        android.util.Log.d("HomeViewModel", "Job saved to Firebase: ${newJob.id}")
                        // Optimistically add to local state immediately so it shows up right away
                        _allJobs.value = _allJobs.value + newJob
                        android.util.Log.d("HomeViewModel", "Job added to local state. Total jobs: ${_allJobs.value.size}")
                        // Real-time listener will update _allJobs when Firestore confirms
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error saving job: ${e.message}", e)
                    }
                } else {
                    _allJobs.value = _allJobs.value + newJob
                    val currentUser = _currentUser.value
                    if (currentUser != null) {
                        dataRepository.saveJobs(currentUser.email, _allJobs.value)
                    }
                }
            } else {
                android.util.Log.d("HomeViewModel", "Job already exists: ${existingJob.id}")
            }
        }
    }
    
    fun declineInvitation(invitationId: String) {
        viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.updateInvitationStatus(invitationId, InvitationStatus.DECLINED)
            }
        }
    }

    fun completeJob(jobId: String, photos: List<String> = emptyList(), notes: String? = null) {
        viewModelScope.launch {
            android.util.Log.d("HomeViewModel", "completeJob called: jobId=$jobId, photos=${photos.size}, notes=${notes?.take(50)}")
            val job = _allJobs.value.find { it.id == jobId }
            if (job == null) {
                android.util.Log.e("HomeViewModel", "Job not found: $jobId")
                return@launch
            }
            
            val updatedJob = job.copy(
                status = "completed",
                completionPhotos = photos,
                completionNotes = notes
            )
            
            // Optimistically update local state immediately
            _allJobs.value = _allJobs.value.map { if (it.id == jobId) updatedJob else it }
            
            if (useFirebase) {
                try {
                    firebaseRepository.saveJob(updatedJob)
                    android.util.Log.d("HomeViewModel", "Job saved to Firestore successfully")
                    // Real-time listener will update _allJobs if there are any changes
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error saving job: ${e.message}", e)
                }
            } else {
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    dataRepository.saveJobs(currentUser.email, _allJobs.value)
                }
            }
            
            val ticket = _allTickets.value.find { it.id == job.ticketId }
            if (ticket == null) {
                android.util.Log.e("HomeViewModel", "Ticket not found for job $jobId with ticketId ${job.ticketId}")
            } else {
                // Update ticket status - this will save all tickets
                val updatedTicket = ticket.copy(
                    status = TicketStatus.COMPLETED, 
                    completedDate = com.example.mvp.utils.DateUtils.getCurrentDateString()
                )
                // Optimistically update local state immediately
                _allTickets.value = _allTickets.value.map { if (it.id == ticket.id) updatedTicket else it }
                // Then save to Firebase
                updateTicket(ticket.id, updatedTicket)
                android.util.Log.d("HomeViewModel", "Ticket updated to COMPLETED")
            }
        }
    }

    fun rateJob(jobId: String, rating: Float) {
        // Alias for addRating for consistency
        addRating(jobId, rating)
    }
    
    fun updateTicketRating(ticketId: String, rating: Float) {
        viewModelScope.launch {
            val ticket = _allTickets.value.find { it.id == ticketId }
            if (ticket != null) {
                // Update ticket rating
                updateTicket(ticketId, ticket.copy(rating = rating))
                
                // Also update contractor rating immediately from tickets
                val contractorId = ticket.assignedContractor ?: ticket.assignedTo
                if (contractorId != null) {
                    updateContractorRatingFromTickets(contractorId)
                }
            }
        }
    }
    
    // Update contractor rating by calculating from tickets
    private fun updateContractorRatingFromTickets(contractorId: String) {
        viewModelScope.launch {
            val contractor = _contractors.value.find { it.id == contractorId }
            if (contractor == null) {
                android.util.Log.e("HomeViewModel", "Contractor not found: $contractorId")
                return@launch
            }
            
            // Get all tickets assigned to this contractor
            val contractorTickets = _allTickets.value.filter { 
                it.assignedContractor == contractorId || it.assignedTo == contractorId
            }
            
            // Get only tickets with ratings
            val ratedTickets = contractorTickets.filter { 
                it.rating != null && it.rating!! > 0f
            }
            
            if (ratedTickets.isEmpty()) {
                android.util.Log.d("HomeViewModel", "No rated tickets for contractor $contractorId")
                return@launch
            }
            
            // Calculate average: sum / count
            val ratings = ratedTickets.mapNotNull { it.rating }
            val sum = ratings.sum()
            val count = ratings.size
            val averageRating = sum / count.toFloat()
            
            android.util.Log.d("HomeViewModel", "=== UPDATING CONTRACTOR RATING ===")
            android.util.Log.d("HomeViewModel", "Contractor ID: $contractorId")
            android.util.Log.d("HomeViewModel", "Ratings: $ratings")
            android.util.Log.d("HomeViewModel", "Sum: $sum, Count: $count, Average: $averageRating")
            
            val updatedContractor = contractor.copy(rating = averageRating)
            
            if (useFirebase) {
                try {
                    firebaseRepository.saveContractor(updatedContractor)
                    android.util.Log.d("HomeViewModel", "✓ Contractor rating saved to Firestore: $averageRating")
                    // Don't update local state here - let the real-time listener handle it
                    // This ensures manual Firestore changes are reflected in the app
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "✗ Error saving contractor rating: ${e.message}", e)
                }
            } else {
                // Only update local state if not using Firebase
                _contractors.value = _contractors.value.map { if (it.id == contractorId) updatedContractor else it }
            }
        }
    }
    
    fun addRating(jobId: String, rating: Float) {
        viewModelScope.launch {
            val job = _allJobs.value.find { it.id == jobId }
            if (job != null) {
                val updatedJob = job.copy(rating = rating)
                android.util.Log.d("HomeViewModel", "Adding rating $rating to job ${job.id} (ticketId: ${job.ticketId}, contractorId: ${job.contractorId})")
                
                // Optimistically update job in local state immediately
                _allJobs.value = _allJobs.value.map { if (it.id == jobId) updatedJob else it }
                
                if (useFirebase) {
                    try {
                        firebaseRepository.saveJob(updatedJob)
                        android.util.Log.d("HomeViewModel", "Job rating saved to Firestore: jobId=${job.id}, rating=$rating")
                        // Real-time listener will update _allJobs if there are any changes
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error saving job rating to Firestore: ${e.message}", e)
                    }
                } else {
                    val currentUser = _currentUser.value
                    if (currentUser != null) {
                        dataRepository.saveJobs(currentUser.email, _allJobs.value)
                    }
                }
                
                // Also update the ticket rating - updateTicket already saves to Firestore
                val ticket = _allTickets.value.find { t -> t.id == job.ticketId }
                if (ticket != null) {
                    val updatedTicket = ticket.copy(rating = rating)
                    android.util.Log.d("HomeViewModel", "Updating ticket ${ticket.id} with rating: $rating (job already updated)")
                    
                    // Optimistically update ticket in local state immediately so contractor rating calculation uses the new rating
                    _allTickets.value = _allTickets.value.map { if (it.id == ticket.id) updatedTicket else it }
                    
                    updateTicket(ticket.id, updatedTicket) // This will save to Firestore via updateTicket
                } else {
                    android.util.Log.e("HomeViewModel", "Ticket not found for job $jobId with ticketId ${job.ticketId}")
                }
                
                // Update contractor rating - SIMPLE: get all tickets with ratings, sum them, divide by count
                val contractor = _contractors.value.find { it.id == job.contractorId }
                if (contractor == null) {
                    android.util.Log.e("HomeViewModel", "Contractor not found for job $jobId with contractorId ${job.contractorId}")
                    return@launch
                }
                
                // Step 1: Get ALL tickets assigned to this contractor
                val contractorTickets = _allTickets.value.filter { 
                    it.assignedContractor == contractor.id || it.assignedTo == contractor.id
                }
                android.util.Log.d("HomeViewModel", "Contractor ${contractor.id} has ${contractorTickets.size} total tickets")
                
                // Step 2: Get only COMPLETED tickets that have been reviewed (have ratings)
                val ratedTickets = contractorTickets.filter { 
                    it.status == TicketStatus.COMPLETED && 
                    it.rating != null && it.rating!! > 0f
                }
                android.util.Log.d("HomeViewModel", "Found ${ratedTickets.size} tickets with ratings")
                
                // Step 3: Extract ratings and calculate average
                val ratings = ratedTickets.mapNotNull { it.rating }
                if (ratings.isEmpty()) {
                    android.util.Log.w("HomeViewModel", "No ratings found for contractor ${contractor.id}")
                    return@launch
                }
                
                val sum = ratings.sum()
                val count = ratings.size
                val averageRating = sum / count.toFloat()
                
                android.util.Log.d("HomeViewModel", "=== RATING CALCULATION ===")
                android.util.Log.d("HomeViewModel", "Contractor: ${contractor.id} (${contractor.email})")
                android.util.Log.d("HomeViewModel", "Ratings: $ratings")
                android.util.Log.d("HomeViewModel", "Sum: $sum, Count: $count")
                android.util.Log.d("HomeViewModel", "Average: $averageRating")
                android.util.Log.d("HomeViewModel", "Old rating: ${contractor.rating}, New rating: $averageRating")
                
                // Step 4: Update contractor with new rating
                val updatedContractor = contractor.copy(rating = averageRating)
                
                if (useFirebase) {
                    try {
                        firebaseRepository.saveContractor(updatedContractor)
                        android.util.Log.d("HomeViewModel", "✓ Contractor rating saved to Firestore")
                        // Don't update local state here - let the real-time listener handle it
                        // This ensures manual Firestore changes are reflected in the app
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "✗ Error saving contractor rating: ${e.message}", e)
                    }
                } else {
                    // Only update local state if not using Firebase
                    _contractors.value = _contractors.value.map { if (it.id == contractor.id) updatedContractor else it }
                    android.util.Log.d("HomeViewModel", "✓ Contractor rating updated locally")
                }
            } else {
                android.util.Log.e("HomeViewModel", "Job not found: $jobId")
            }
        }
    }

    // Simple function to recalculate contractor rating from tickets
    fun recalculateContractorRating(contractorId: String) {
        viewModelScope.launch {
            val contractor = _contractors.value.find { it.id == contractorId }
            if (contractor == null) {
                android.util.Log.e("HomeViewModel", "Contractor not found: $contractorId")
                return@launch
            }
            
            // Get all tickets assigned to this contractor
            val contractorTickets = _allTickets.value.filter { 
                it.assignedContractor == contractorId || it.assignedTo == contractorId
            }
            
            // Get only COMPLETED tickets that have been reviewed (have ratings)
            val ratedTickets = contractorTickets.filter { 
                it.status == TicketStatus.COMPLETED && 
                it.rating != null && it.rating!! > 0f
            }
            
            if (ratedTickets.isEmpty()) {
                android.util.Log.d("HomeViewModel", "No completed and rated tickets for contractor $contractorId")
                return@launch
            }
            
            // Calculate average: sum / count
            val ratings = ratedTickets.mapNotNull { it.rating }
            val sum = ratings.sum()
            val count = ratings.size
            val averageRating = sum / count.toFloat()
            
            android.util.Log.d("HomeViewModel", "Recalculated rating for contractor $contractorId: $averageRating (from $count ratings)")
            
            val updatedContractor = contractor.copy(rating = averageRating)
            
            if (useFirebase) {
                try {
                    firebaseRepository.saveContractor(updatedContractor)
                    // Don't update local state here - let the real-time listener handle it
                    // This ensures manual Firestore changes are reflected in the app
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error saving contractor rating: ${e.message}", e)
                }
            } else {
                // Only update local state if not using Firebase
                _contractors.value = _contractors.value.map { if (it.id == contractorId) updatedContractor else it }
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
    
    fun scheduleJob(jobId: String, date: String, time: String) {
        viewModelScope.launch {
            android.util.Log.d("HomeViewModel", "Scheduling job $jobId with date=$date, time=$time")
            val job = _allJobs.value.find { it.id == jobId }
            if (job == null) {
                android.util.Log.e("HomeViewModel", "Job not found: $jobId")
                return@launch
            }
            
            val updatedJob = job.copy(
                scheduledDate = date,
                scheduledTime = time,
                status = "scheduled" // Change status to scheduled
            )
            
            android.util.Log.d("HomeViewModel", "Updating job ${job.id}: status=${updatedJob.status}, scheduledDate=${updatedJob.scheduledDate}, scheduledTime=${updatedJob.scheduledTime}")
            
            // Optimistically update job in local state immediately
            _allJobs.value = _allJobs.value.map { if (it.id == jobId) updatedJob else it }
            
            if (useFirebase) {
                try {
                    firebaseRepository.saveJob(updatedJob)
                    android.util.Log.d("HomeViewModel", "Job saved to Firestore successfully")
                    // Real-time listener will update _allJobs if there are any changes
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error scheduling job: ${e.message}", e)
                }
            } else {
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    dataRepository.saveJobs(currentUser.email, _allJobs.value)
                }
            }
            
            // Update ticket status to SCHEDULED and scheduledDate
            val ticket = _allTickets.value.find { it.id == job.ticketId }
            if (ticket == null) {
                android.util.Log.e("HomeViewModel", "Ticket not found for job $jobId with ticketId ${job.ticketId}")
            } else {
                val scheduledDateTime = "$date $time"
                val updatedTicket = ticket.copy(
                    status = TicketStatus.SCHEDULED,
                    scheduledDate = scheduledDateTime
                )
                android.util.Log.d("HomeViewModel", "Updating ticket ${ticket.id}: status=${updatedTicket.status}, scheduledDate=${updatedTicket.scheduledDate}")
                // Optimistically update local state immediately
                _allTickets.value = _allTickets.value.map { if (it.id == job.ticketId) updatedTicket else it }
                // Then save to Firebase
                updateTicket(job.ticketId, updatedTicket)
                android.util.Log.d("HomeViewModel", "Ticket update called")
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

    fun updateContractorService(contractorId: String, specialization: List<String>, serviceAreas: Map<String, List<String>>) {
        viewModelScope.launch {
            val contractor = _contractors.value.find { it.id == contractorId }
            if (contractor != null && useFirebase) {
                val updatedContractor = contractor.copy(
                    specialization = specialization,
                    serviceAreas = serviceAreas
                )
                firebaseRepository.saveContractor(updatedContractor)
                // Update local state
                _contractors.value = _contractors.value.map { 
                    if (it.id == contractorId) updatedContractor else it 
                }
            }
        }
    }
    
    fun saveLastViewedTimestamps(timestamps: Map<String, String>) {
        viewModelScope.launch {
            dataRepository.saveLastViewedTimestamps(timestamps)
        }
    }
    
    suspend fun loadLastViewedTimestamps(): Map<String, String> {
        return dataRepository.getLastViewedTimestamps()
    }
    
    fun markDirectMessagesAsRead(messageIds: List<String>, readerEmail: String) {
        viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.markDirectMessagesAsRead(messageIds, readerEmail)
            }
            // For DataStore, we'd need to update messages in place, but since we're using Firebase, skip for now
        }
    }
    
    fun markContractorLandlordMessagesAsRead(messageIds: List<String>, readerEmail: String) {
        viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.markContractorLandlordMessagesAsRead(messageIds, readerEmail)
            }
            // For DataStore, we'd need to update messages in place, but since we're using Firebase, skip for now
        }
    }
    
    fun getContractorIdForUser(user: User?): String? {
        if (user?.role != UserRole.CONTRACTOR) return null
        // Try to match contractor by email first (most reliable)
        val matchedById = _contractors.value.find { contractor ->
            contractor.email?.lowercase() == user.email.lowercase()
        }?.id
        
        if (matchedById != null) {
            android.util.Log.d("HomeViewModel", "Matched contractor by email: $matchedById for user ${user.email}")
            return matchedById
        }
        
        // Try to match by name or email prefix
        val matchedByName = _contractors.value.find { 
            it.name.contains(user.name, ignoreCase = true) || 
            it.name.contains(user.email.split("@").first(), ignoreCase = true)
        }?.id
        
        if (matchedByName != null) {
            android.util.Log.d("HomeViewModel", "Matched contractor by name: $matchedByName for user ${user.email}")
            return matchedByName
        }
        
        // Fallback to first contractor (for testing/development)
        val fallbackId = _contractors.value.firstOrNull()?.id
        android.util.Log.d("HomeViewModel", "Using fallback contractor ID: $fallbackId for user ${user.email}")
        return fallbackId
    }
    
    // Landlord-Tenant Connection functions
    fun requestTenantConnection(tenantEmail: String) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && currentUser.role == UserRole.LANDLORD) {
                if (useFirebase) {
                    // Verify Firebase Auth user exists before making request
                    val firebaseAuthUser = FirebaseAuthManager.getCurrentUser()
                    if (firebaseAuthUser == null) {
                        android.util.Log.e("HomeViewModel", "Firebase Auth user is null - user not authenticated")
                        _authError.value = "You are not authenticated. Please sign out and sign in again."
                        return@launch
                    }
                    
                    android.util.Log.d("HomeViewModel", "Firebase Auth user: ${firebaseAuthUser.uid}, email: ${firebaseAuthUser.email}")
                    android.util.Log.d("HomeViewModel", "Current app user: ${currentUser.email}, role: ${currentUser.role}")
                    
                    try {
                        firebaseRepository.requestConnection(
                            landlordEmail = currentUser.email,
                            tenantEmail = tenantEmail,
                            requestedBy = currentUser.email
                        )
                        android.util.Log.d("HomeViewModel", "Connection request sent successfully")
                        // Connection request saved successfully
                    } catch (e: Exception) {
                        // Handle error - could show a message to user
                        android.util.Log.e("HomeViewModel", "Error requesting connection: ${e.message}", e)
                        android.util.Log.e("HomeViewModel", "Error stack trace: ${e.stackTraceToString()}")
                        _authError.value = "Failed to send connection request: ${e.message}"
                    }
                }
            }
        }
    }
    
    fun confirmConnection(connectionId: String, accept: Boolean) {
        viewModelScope.launch {
            if (useFirebase) {
                val status = if (accept) ConnectionStatus.CONNECTED else ConnectionStatus.REJECTED
                firebaseRepository.updateConnectionStatus(connectionId, status)
                refreshConnections()
            }
        }
    }
    
    fun cancelConnectionRequest(connectionId: String) {
        viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.deleteConnection(connectionId)
                refreshConnections()
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
                android.util.Log.d("HomeViewModel", "sendDirectMessage called: role=${currentUser.role}, tenantEmail=$tenantEmail, messageText=$messageText")
                val landlordEmail = when (currentUser.role) {
                    UserRole.LANDLORD -> currentUser.email
                    UserRole.CONTRACTOR -> {
                        // For contractors messaging tenants, get the tenant's landlord from connection
                        val connection = _connections.value.find { 
                            it.tenantEmail.lowercase() == tenantEmail.lowercase() && 
                            it.status == ConnectionStatus.CONNECTED
                        }
                        if (connection == null) {
                            android.util.Log.e("HomeViewModel", "No connection found for tenant: $tenantEmail")
                            android.util.Log.d("HomeViewModel", "Available connections: ${_connections.value.map { "${it.tenantEmail} -> ${it.landlordEmail}" }}")
                        }
                        connection?.landlordEmail ?: run {
                            android.util.Log.e("HomeViewModel", "Cannot send message: no landlord found for tenant $tenantEmail")
                            return@launch
                        }
                    }
                    UserRole.TENANT -> getLandlordEmail() ?: return@launch
                }
                val tenant = if (currentUser.role == UserRole.TENANT) {
                    currentUser.email
                } else {
                    tenantEmail
                }
                
                // Determine receiver based on sender role
                val receiverEmail = when (currentUser.role) {
                    UserRole.LANDLORD -> tenant // Landlord sends to tenant
                    UserRole.CONTRACTOR -> tenant // Contractor sends to tenant
                    UserRole.TENANT -> landlordEmail // Tenant sends to landlord
                }
                
                android.util.Log.d("HomeViewModel", "Creating DirectMessage: landlord=$landlordEmail, tenant=$tenant, sender=${currentUser.email}, receiver=$receiverEmail")
                
                val message = DirectMessage(
                    id = "msg-${System.currentTimeMillis()}",
                    landlordEmail = landlordEmail,
                    tenantEmail = tenant,
                    senderEmail = currentUser.email,
                    receiverEmail = receiverEmail,
                    senderName = currentUser.name,
                    text = messageText,
                    timestamp = com.example.mvp.utils.DateUtils.getCurrentDateTimeString()
                )
                
                // Optimistically add message to local state immediately
                val currentMessages = _directMessages.value.toMutableList()
                currentMessages.add(message)
                _directMessages.value = currentMessages.sortedBy { it.timestamp }
                
                // Also update "all messages" StateFlows so the message appears in messages list immediately
                when (currentUser.role) {
                    UserRole.LANDLORD -> {
                        // Add to allDirectMessages for landlord's messages list
                        val currentAllDirectMessages = _allDirectMessages.value.toMutableList()
                        if (!currentAllDirectMessages.any { it.id == message.id }) {
                            currentAllDirectMessages.add(message)
                            _allDirectMessages.value = currentAllDirectMessages
                        }
                    }
                    UserRole.TENANT -> {
                        // Add to allTenantMessages for tenant's messages list
                        val currentAllTenantMessages = _allTenantMessages.value.toMutableList()
                        if (!currentAllTenantMessages.any { it.id == message.id }) {
                            currentAllTenantMessages.add(message)
                            _allTenantMessages.value = currentAllTenantMessages
                        }
                    }
                    UserRole.CONTRACTOR -> {
                        // Add to allContractorTenantMessages for contractor's messages list
                        val currentAllContractorTenantMessages = _allContractorTenantMessages.value.toMutableList()
                        if (!currentAllContractorTenantMessages.any { it.id == message.id }) {
                            currentAllContractorTenantMessages.add(message)
                            _allContractorTenantMessages.value = currentAllContractorTenantMessages.sortedBy { it.timestamp }
                        }
                    }
                }
                
                // Also update tenant-contractor messages if we're in a tenant-contractor conversation
                // Check if we're observing tenant-contractor messages for this tenant
                val currentObservedEmail = observedTenantContractorEmail
                if (currentObservedEmail != null) {
                    val parts = currentObservedEmail.split(":")
                    val observedTenant = parts.getOrNull(0)?.lowercase()
                    val observedContractor = parts.getOrNull(1)?.lowercase()
                    
                    // If this message is part of the current tenant-contractor conversation, add it
                    if (observedTenant == tenant.lowercase() && 
                        (message.senderEmail.lowercase() == tenant.lowercase() || 
                         message.senderEmail.lowercase() == observedContractor)) {
                        val currentTenantContractorMessages = _tenantContractorMessages.value.toMutableList()
                        currentTenantContractorMessages.add(message)
                        _tenantContractorMessages.value = currentTenantContractorMessages.sortedBy { it.timestamp }
                    }
                }
                
                android.util.Log.d("HomeViewModel", "Message added to local state, now saving to Firebase")
                
                // Then save to Firebase (real-time listener will update if needed)
                firebaseRepository.sendDirectMessage(message)
            } else {
                android.util.Log.e("HomeViewModel", "Cannot send message: currentUser is null or useFirebase is false")
            }
        }
    }
    
    // Send message from tenant to contractor
    fun sendMessageToContractor(contractorEmail: String, messageText: String) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && currentUser.role == UserRole.TENANT && useFirebase) {
                // Get landlord email from connection
                val landlordEmail = getLandlordEmail() ?: run {
                    android.util.Log.e("HomeViewModel", "Cannot send message to contractor: no landlord found")
                    return@launch
                }
                
                android.util.Log.d("HomeViewModel", "Sending message from tenant ${currentUser.email} to contractor $contractorEmail")
                
                // Use DirectMessage structure with receiverEmail for tenant-contractor messages
                val message = DirectMessage(
                    id = "msg-${System.currentTimeMillis()}",
                    landlordEmail = landlordEmail, // Keep for backward compatibility
                    tenantEmail = currentUser.email, // Keep for backward compatibility
                    senderEmail = currentUser.email,
                    receiverEmail = contractorEmail, // Receiver is the contractor
                    senderName = currentUser.name,
                    text = messageText,
                    timestamp = com.example.mvp.utils.DateUtils.getCurrentDateTimeString()
                )
                
                // Optimistically add message to local state immediately
                // Update _directMessages for general tenant messages
                val currentDirectMessages = _directMessages.value.toMutableList()
                currentDirectMessages.add(message)
                _directMessages.value = currentDirectMessages
                
                // Add to allTenantMessages so it appears in tenant's messages list immediately
                val currentAllTenantMessages = _allTenantMessages.value.toMutableList()
                if (!currentAllTenantMessages.any { it.id == message.id }) {
                    currentAllTenantMessages.add(message)
                    _allTenantMessages.value = currentAllTenantMessages
                }
                
                // Also update _tenantContractorMessages for the conversation screen
                val currentTenantContractorMessages = _tenantContractorMessages.value.toMutableList()
                currentTenantContractorMessages.add(message)
                _tenantContractorMessages.value = currentTenantContractorMessages.sortedBy { it.timestamp }
                
                android.util.Log.d("HomeViewModel", "Message to contractor added to local state, now saving to Firebase")
                
                // Save to Firebase
                firebaseRepository.sendDirectMessage(message)
            } else {
                android.util.Log.e("HomeViewModel", "Cannot send message to contractor: currentUser is null or not a tenant or useFirebase is false")
            }
        }
    }
    
    fun startObservingDirectMessages(tenantEmail: String) {
        // Cancel previous observation if any
        directMessagesJob?.cancel()
        directMessagesJob = null
        
        // Keep existing messages visible while loading new ones to prevent flash
        // Only clear when explicitly requested via clearDirectMessagesData()
        
        directMessagesJob = viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && useFirebase) {
                when (currentUser.role) {
                    UserRole.TENANT -> {
                        // For tenants, observe ALL their messages (from landlord and contractors)
                        android.util.Log.d("HomeViewModel", "Starting to observe all direct messages for tenant: ${currentUser.email}")
                        try {
                            firebaseRepository.observeDirectMessagesByTenant(currentUser.email).collect { messages ->
                                android.util.Log.d("HomeViewModel", "Received ${messages.size} direct messages for tenant")
                                // Sort by timestamp to ensure correct order
                                _directMessages.value = messages.sortedBy { it.timestamp }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            android.util.Log.d("HomeViewModel", "Direct messages observation cancelled")
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.e("HomeViewModel", "Error observing direct messages: ${e.message}", e)
                        }
                    }
                    UserRole.LANDLORD -> {
                        // For landlords, observe messages with a specific tenant
                        val tenant = tenantEmail
                        android.util.Log.d("HomeViewModel", "Starting to observe direct messages: landlord=${currentUser.email}, tenant=$tenant")
                        try {
                            firebaseRepository.observeDirectMessages(currentUser.email, tenant).collect { messages ->
                                android.util.Log.d("HomeViewModel", "Received ${messages.size} direct messages")
                                _directMessages.value = messages.sortedBy { it.timestamp }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            android.util.Log.d("HomeViewModel", "Direct messages observation cancelled")
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.e("HomeViewModel", "Error observing direct messages: ${e.message}", e)
                        }
                    }
                    UserRole.CONTRACTOR -> {
                        // For contractors viewing tenant messages, get the tenant's landlord from connection
                        val landlordEmail = _connections.value.find { 
                            it.tenantEmail.lowercase() == tenantEmail.lowercase() && 
                            it.status == ConnectionStatus.CONNECTED
                        }?.landlordEmail ?: return@launch
                        val tenant = tenantEmail
                        android.util.Log.d("HomeViewModel", "Starting to observe direct messages: landlord=$landlordEmail, tenant=$tenant, role=CONTRACTOR")
                        try {
                            firebaseRepository.observeDirectMessages(landlordEmail, tenant).collect { messages ->
                                android.util.Log.d("HomeViewModel", "Received ${messages.size} direct messages")
                                _directMessages.value = messages.sortedBy { it.timestamp }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            android.util.Log.d("HomeViewModel", "Direct messages observation cancelled")
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.e("HomeViewModel", "Error observing direct messages: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }
    
    fun clearDirectMessages() {
        // Cancel any ongoing observation
        directMessagesJob?.cancel()
        directMessagesJob = null
        android.util.Log.d("HomeViewModel", "Stopped observing direct messages")
    }
    
    fun stopObservingDirectMessages() {
        directMessagesJob?.cancel()
        directMessagesJob = null
        android.util.Log.d("HomeViewModel", "Stopped observing direct messages")
    }
    
    fun clearDirectMessagesData() {
        clearDirectMessages()
        _directMessages.value = emptyList()
        android.util.Log.d("HomeViewModel", "Cleared direct messages data")
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
    fun sendContractorLandlordMessage(ticketId: String, otherPartyEmailOrId: String, messageText: String) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && useFirebase) {
                val ticket = _allTickets.value.find { it.id == ticketId }
                
                // Determine contractor email
                val contractorEmail = if (currentUser.role == UserRole.CONTRACTOR) {
                    currentUser.email
                } else {
                    // For landlord: otherPartyEmailOrId could be contractor email or ID
                    // Try to find contractor by ID first, then use email directly
                    val contractor = _contractors.value.find { 
                        it.id == otherPartyEmailOrId || it.email == otherPartyEmailOrId 
                    }
                    contractor?.email ?: otherPartyEmailOrId
                }
                
                // Determine landlord email
                val landlordEmail = if (currentUser.role == UserRole.LANDLORD) {
                    currentUser.email
                } else {
                    // For contractor, use the provided landlord email
                    otherPartyEmailOrId
                }
                
                val message = ContractorLandlordMessage(
                    id = "cl-msg-${System.currentTimeMillis()}",
                    ticketId = ticketId,
                    contractorEmail = contractorEmail.trim().lowercase(),
                    landlordEmail = landlordEmail.trim().lowercase(),
                    senderEmail = currentUser.email.trim().lowercase(),
                    senderName = currentUser.name,
                    text = messageText,
                    timestamp = com.example.mvp.utils.DateUtils.getCurrentDateTimeString()
                )
                
                // Optimistically add message to local state immediately (like direct messages)
                val currentMessages = _contractorLandlordMessages.value.toMutableList()
                currentMessages.add(message)
                _contractorLandlordMessages.value = currentMessages.sortedBy { it.timestamp }
                
                // Also update "all messages" StateFlows so the message appears in messages list immediately
                when (currentUser.role) {
                    UserRole.CONTRACTOR -> {
                        // Add to allContractorLandlordMessages for contractor's messages list
                        val currentAllContractorMessages = _allContractorLandlordMessages.value.toMutableList()
                        if (!currentAllContractorMessages.any { it.id == message.id }) {
                            currentAllContractorMessages.add(message)
                            _allContractorLandlordMessages.value = currentAllContractorMessages.sortedBy { it.timestamp }
                        }
                    }
                    UserRole.LANDLORD -> {
                        // Add to allLandlordContractorMessages for landlord's messages list
                        val currentAllLandlordMessages = _allLandlordContractorMessages.value.toMutableList()
                        if (!currentAllLandlordMessages.any { it.id == message.id }) {
                            currentAllLandlordMessages.add(message)
                            _allLandlordContractorMessages.value = currentAllLandlordMessages.sortedBy { it.timestamp }
                        }
                    }
                    else -> {}
                }
                
                // Then save to Firebase (real-time listener will update if needed)
                firebaseRepository.sendContractorLandlordMessage(message)
            }
        }
    }
    
    fun startObservingContractorLandlordMessages(ticketId: String, otherPartyEmail: String? = null) {
        val currentUser = _currentUser.value
        if (currentUser == null) return
        
        // Determine contractor and landlord emails
        val contractorEmail = if (currentUser.role == UserRole.CONTRACTOR) {
            currentUser.email
        } else {
            // For landlord: try to get contractor email from otherPartyEmail, ticket, or contractors list
            otherPartyEmail ?: run {
                if (ticketId.isNotEmpty() && ticketId != "general") {
                    val ticket = _allTickets.value.find { it.id == ticketId }
                    ticket?.assignedTo?.let { contractorId ->
                        _contractors.value.find { it.id == contractorId }?.email
                    }
                } else null
            } ?: return
        }
        
        val landlordEmail = if (currentUser.role == UserRole.LANDLORD) {
            currentUser.email
        } else {
            // For contractor: use otherPartyEmail (landlord email) or get from ticket
            otherPartyEmail ?: run {
                if (ticketId.isNotEmpty() && ticketId != "general") {
                    getLandlordEmailForTicket(ticketId)
                } else null
            } ?: return
        }
        
        // Use contractor and landlord emails as the key (not ticketId)
        // This ensures all messages between the same contractor and landlord are in one conversation
        val key = "$contractorEmail:$landlordEmail"
        if (observedContractorLandlordKey == key && contractorLandlordMessagesJob?.isActive == true) {
            android.util.Log.d("HomeViewModel", "Already observing contractor-landlord messages for $key, skipping restart")
            return
        }
        
        // Cancel previous observation only if it's a different conversation
        contractorLandlordMessagesJob?.cancel()
        contractorLandlordMessagesJob = null
        observedContractorLandlordKey = key
        
        contractorLandlordMessagesJob = viewModelScope.launch {
            if (useFirebase) {
                android.util.Log.d("HomeViewModel", "Starting to observe ALL contractor-landlord messages: contractor=$contractorEmail, landlord=$landlordEmail (ignoring ticketId)")
                
                // Initialize with existing messages from allContractorLandlordMessages if available
                // This ensures messages show immediately, even before the observation starts
                val existingMessages = if (currentUser.role == UserRole.CONTRACTOR) {
                    _allContractorLandlordMessages.value.filter { 
                        it.landlordEmail.lowercase() == landlordEmail.lowercase() 
                    }
                } else {
                    _allLandlordContractorMessages.value.filter { 
                        it.contractorEmail.lowercase() == contractorEmail.lowercase() 
                    }
                }
                
                if (existingMessages.isNotEmpty()) {
                    android.util.Log.d("HomeViewModel", "Initializing with ${existingMessages.size} existing messages")
                    _contractorLandlordMessages.value = existingMessages.sortedBy { it.timestamp }
                }
                
                try {
                    // Observe ALL messages between contractor and landlord, regardless of ticketId
                    // Use "general" to get all messages, or query without ticketId filter
                    firebaseRepository.observeContractorLandlordMessagesBetween(
                        contractorEmail = contractorEmail,
                        landlordEmail = landlordEmail,
                        ticketId = "general" // Use "general" to get all messages between these two parties
                    ).collect { messages ->
                        android.util.Log.d("HomeViewModel", "Received ${messages.size} contractor-landlord messages (all tickets)")
                        // Sort by timestamp to ensure correct order
                        _contractorLandlordMessages.value = messages.sortedBy { it.timestamp }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("HomeViewModel", "Contractor-landlord messages observation cancelled")
                    throw e // Re-throw cancellation
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error observing contractor-landlord messages", e)
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
    
    // Track the email we're currently observing to prevent unnecessary restarts
    private var observedContractorEmail: String? = null
    
    // Track the contractor-landlord conversation key to prevent unnecessary restarts
    private var observedContractorLandlordKey: String? = null
    
    // Start observing all contractor-landlord messages for a contractor
    fun startObservingAllContractorLandlordMessages(contractorEmail: String) {
        val normalizedEmail = contractorEmail.trim().lowercase()
        
        // Don't restart if already observing the same email
        if (allContractorLandlordMessagesJob?.isActive == true && observedContractorEmail == normalizedEmail) {
            android.util.Log.d("HomeViewModel", "Already observing contractor messages for: $normalizedEmail, skipping restart")
            return
        }
        
        android.util.Log.d("HomeViewModel", "Starting to observe contractor messages for: $normalizedEmail")
        android.util.Log.d("HomeViewModel", "Current messages count before observation: ${_allContractorLandlordMessages.value.size}")
        
        // Cancel previous observation but keep existing messages
        val previousJob = allContractorLandlordMessagesJob
        if (previousJob != null && previousJob.isActive) {
            previousJob.cancel()
        }
        
        // Start new observation
        observedContractorEmail = normalizedEmail
        allContractorLandlordMessagesJob = viewModelScope.launch {
            if (useFirebase) {
                try {
                    firebaseRepository.observeContractorLandlordMessagesByContractor(normalizedEmail).collect { messages ->
                        // Filter to only show messages where:
                        // 1. contractorEmail matches (contractor is the recipient), OR
                        // 2. senderEmail matches (contractor sent it)
                        // This ensures contractors only see messages where they are the sender or intended recipient
                        val filteredMessages = messages.filter { message ->
                            message.contractorEmail.lowercase() == normalizedEmail || 
                            message.senderEmail.lowercase() == normalizedEmail
                        }
                        android.util.Log.d("HomeViewModel", "Filtered ${filteredMessages.size} contractor messages from ${messages.size} total for contractor $normalizedEmail")
                        _allContractorLandlordMessages.value = filteredMessages
                        android.util.Log.d("HomeViewModel", "StateFlow updated, current count: ${_allContractorLandlordMessages.value.size}")
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("HomeViewModel", "Contractor messages observation cancelled")
                    // Don't clear messages on cancellation
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error observing contractor messages: ${e.message}", e)
                    // Don't clear messages on error - keep what we have
                }
            } else {
                android.util.Log.w("HomeViewModel", "Firebase not available, cannot observe messages")
            }
        }
    }
    
    // Stop observing all contractor-landlord messages
    fun stopObservingAllContractorLandlordMessages() {
        android.util.Log.d("HomeViewModel", "Stopping observation of contractor messages")
        allContractorLandlordMessagesJob?.cancel()
        allContractorLandlordMessagesJob = null
        observedContractorEmail = null
        // Don't clear messages - keep them in case user comes back
    }
    
    // Get unique landlords who have messaged the contractor
    fun getUniqueLandlordsForContractor(): List<String> {
        return _allContractorLandlordMessages.value
            .map { it.landlordEmail }
            .distinct()
    }
    
    // Get latest message for each landlord
    fun getLatestMessageForLandlord(landlordEmail: String): ContractorLandlordMessage? {
        return _allContractorLandlordMessages.value
            .filter { it.landlordEmail == landlordEmail }
            .maxByOrNull { it.timestamp }
    }
    
    // Get all messages for a specific landlord
    fun getMessagesForLandlord(landlordEmail: String): List<ContractorLandlordMessage> {
        return _allContractorLandlordMessages.value
            .filter { it.landlordEmail == landlordEmail }
            .sortedBy { it.timestamp }
    }
    
    // Start observing all contractor-landlord messages for a landlord
    fun startObservingAllLandlordContractorMessages(landlordEmail: String) {
        val normalizedEmail = landlordEmail.lowercase()
        allLandlordContractorMessagesJob?.cancel()
        allLandlordContractorMessagesJob = viewModelScope.launch {
            if (useFirebase) {
                    firebaseRepository.observeContractorLandlordMessagesByLandlord(normalizedEmail).collect { messages ->
                        // Filter to only show messages where:
                        // 1. landlordEmail matches (landlord is the recipient), OR
                        // 2. senderEmail matches (landlord sent it)
                        // This ensures landlords only see messages where they are the sender or intended recipient
                        val filteredMessages = messages.filter { message ->
                            message.landlordEmail.lowercase() == normalizedEmail || 
                            message.senderEmail.lowercase() == normalizedEmail
                        }
                    android.util.Log.d("HomeViewModel", "Filtered ${filteredMessages.size} landlord-contractor messages from ${messages.size} total for landlord $normalizedEmail")
                    _allLandlordContractorMessages.value = filteredMessages
                }
            }
        }
    }
    
    // Stop observing all landlord-contractor messages
    fun stopObservingAllLandlordContractorMessages() {
        allLandlordContractorMessagesJob?.cancel()
        allLandlordContractorMessagesJob = null
    }
    
    // All direct messages for a landlord (for messages list)
    private val _allDirectMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val allDirectMessages: StateFlow<List<DirectMessage>> = _allDirectMessages.asStateFlow()
    private var allDirectMessagesJob: kotlinx.coroutines.Job? = null
    private var observedLandlordEmail: String? = null
    
    // Start observing all direct messages for a landlord
    fun startObservingAllDirectMessages(landlordEmail: String) {
        val normalizedEmail = landlordEmail.lowercase()
        
        // Only restart if we're observing a different landlord
        if (observedLandlordEmail == normalizedEmail && allDirectMessagesJob?.isActive == true) {
            android.util.Log.d("HomeViewModel", "Already observing direct messages for landlord $normalizedEmail, skipping restart")
            return
        }
        
        // Cancel previous observation if different landlord
        allDirectMessagesJob?.cancel()
        observedLandlordEmail = normalizedEmail
        
        allDirectMessagesJob = viewModelScope.launch {
            if (useFirebase) {
                try {
                    // Query messages where landlord is recipient AND messages where landlord is sender
                    // Use combine to merge both flows and get all messages involving the landlord
                    val recipientFlow = firebaseRepository.observeDirectMessagesByLandlord(normalizedEmail)
                    val senderFlow = firebaseRepository.observeDirectMessagesByLandlordAsSender(normalizedEmail)
                    
                    // Use combine to merge both flows - it will emit whenever either flow emits
                    // Use catch to handle errors gracefully and keep existing messages
                    combine(recipientFlow, senderFlow) { recipientMessages, senderMessages ->
                        // Combine and deduplicate messages
                        (recipientMessages + senderMessages).distinctBy { it.id }
                    }
                    .catch { e ->
                        android.util.Log.e("HomeViewModel", "Error in combine flow: ${e.message}", e)
                        // Emit current messages to prevent clearing
                        emit(_allDirectMessages.value)
                    }
                    .collect { allMessages ->
                        val asRecipient = allMessages.count { 
                            it.landlordEmail.lowercase() == normalizedEmail || 
                            it.receiverEmail.lowercase() == normalizedEmail 
                        }
                        val asSender = allMessages.count { it.senderEmail.lowercase() == normalizedEmail }
                        android.util.Log.d("HomeViewModel", "Combined ${allMessages.size} direct messages for landlord $normalizedEmail ($asRecipient as recipient, $asSender as sender)")
                        // Only update if we have messages or if this is the first emission
                        // This prevents clearing messages when index is building
                        if (allMessages.isNotEmpty() || _allDirectMessages.value.isEmpty()) {
                            _allDirectMessages.value = allMessages.sortedBy { it.timestamp }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("HomeViewModel", "Direct messages observation cancelled")
                    throw e // Re-throw cancellation
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error observing all direct messages: ${e.message}", e)
                    // Don't clear messages on error - keep what we have
                }
            } else {
                // If not using Firebase, keep existing messages
                // Don't clear them
            }
        }
    }
    
    // Stop observing all direct messages
    fun stopObservingAllDirectMessages() {
        allDirectMessagesJob?.cancel()
        allDirectMessagesJob = null
        observedLandlordEmail = null
    }
    
    // Contractor-Tenant Messages (for contractors viewing all tenant conversations)
    private val _allContractorTenantMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val allContractorTenantMessages: StateFlow<List<DirectMessage>> = _allContractorTenantMessages.asStateFlow()
    private var allContractorTenantMessagesJob: kotlinx.coroutines.Job? = null
    private var observedContractorTenantEmail: String? = null

    fun startObservingAllContractorTenantMessages(contractorEmail: String) {
        val normalizedEmail = contractorEmail.lowercase()

        if (observedContractorTenantEmail == normalizedEmail && allContractorTenantMessagesJob?.isActive == true) {
            android.util.Log.d("HomeViewModel", "Already observing all contractor-tenant messages for $normalizedEmail, skipping restart")
            return
        }

        allContractorTenantMessagesJob?.cancel()
        observedContractorTenantEmail = normalizedEmail

        allContractorTenantMessagesJob = viewModelScope.launch {
            if (useFirebase) {
                try {
                    // Find all tenants assigned to this contractor via jobs
                    val contractor = _contractors.value.find { it.email?.lowercase() == normalizedEmail }
                    val contractorId = contractor?.id ?: ""
                    val contractorJobs = _allJobs.value.filter { 
                        it.contractorId == contractorId || 
                        it.contractorId == normalizedEmail
                    }
                    val tenantEmails = contractorJobs.mapNotNull { job ->
                        val ticket = _allTickets.value.find { it.id == job.ticketId }
                        ticket?.submittedBy?.takeIf { ticket.submittedByRole == UserRole.TENANT }
                    }.distinct()
                    
                    // Get messages where contractor is sender (contractor -> tenant)
                    val contractorSentFlow = firebaseRepository.observeDirectMessagesByContractor(normalizedEmail)
                    
                    // Get messages where contractor is receiver (tenant -> contractor) - NEW: using receiverEmail
                    val contractorReceivedFlow = firebaseRepository.observeDirectMessagesForUser(normalizedEmail).map { allMessages ->
                        // Filter to only messages where contractor is receiver and sender is a tenant assigned to this contractor
                        allMessages.filter { message ->
                            message.receiverEmail.lowercase() == normalizedEmail &&
                            tenantEmails.contains(message.senderEmail.lowercase())
                        }
                    }
                    
                    // Get messages where tenants are senders (tenant -> contractor) - OLD: for backward compatibility
                    // Combine flows for all tenants
                    val tenantSentFlows = tenantEmails.map { tenantEmail ->
                        firebaseRepository.observeDirectMessagesFromTenant(tenantEmail.lowercase())
                    }
                    
                    if (tenantSentFlows.isEmpty()) {
                        // No tenants assigned, combine contractor-sent and contractor-received messages
                        combine(contractorSentFlow, contractorReceivedFlow) { sent: List<DirectMessage>, received: List<DirectMessage> ->
                            (sent + received).distinctBy { it.id }
                        }.collect { allMessages ->
                            // Filter to only show messages where contractor is sender or receiver
                            // EXCLUDE tenant-landlord messages (where contractor is NOT involved)
                            val filteredMessages = allMessages.filter { message ->
                                val sender = message.senderEmail.lowercase()
                                val receiver = message.receiverEmail.lowercase()
                                val tenant = message.tenantEmail.lowercase()
                                val landlord = message.landlordEmail.lowercase()
                                
                                // EXCLUDE tenant-landlord messages (where contractor is NOT sender or receiver)
                                val isTenantLandlordMessage = (sender == tenant && receiver == landlord) || 
                                                              (sender == landlord && receiver == tenant)
                                val contractorInvolved = sender == normalizedEmail || receiver == normalizedEmail
                                
                                if (isTenantLandlordMessage && !contractorInvolved) {
                                    false
                                } else {
                                    sender == normalizedEmail || receiver == normalizedEmail
                                }
                            }
                            android.util.Log.d("HomeViewModel", "Filtered ${filteredMessages.size} contractor-tenant messages (contractor-sent and contractor-received)")
                            _allContractorTenantMessages.value = filteredMessages.sortedBy { it.timestamp }
                        }
                    } else {
                        // Combine contractor-sent, contractor-received, and all tenant-sent flows
                        val allFlows = listOf(contractorSentFlow, contractorReceivedFlow) + tenantSentFlows
                        combine(allFlows) { messageLists: Array<List<DirectMessage>> ->
                            messageLists.flatMap { it }.distinctBy { message: DirectMessage -> message.id }
                        }.collect { allMessages: List<DirectMessage> ->
                            // Filter to only show messages where:
                            // 1. Contractor is the sender (senderEmail == contractorEmail) - they sent it
                            // 2. Contractor is the receiver (receiverEmail == contractorEmail) - tenant sent it to contractor
                            // 3. Tenant sent the message (senderEmail == tenantEmail) AND tenantEmail is in assigned tenants (old format)
                            // EXCLUDE: Messages between tenant and landlord (where contractor is NOT involved)
                            // Then, only include conversations where contractor has sent at least one message OR received at least one message
                            val allFilteredMessages = allMessages.filter { message ->
                                val sender = message.senderEmail.lowercase()
                                val receiver = message.receiverEmail.lowercase()
                                val tenant = message.tenantEmail.lowercase()
                                val landlord = message.landlordEmail.lowercase()
                                
                                // EXCLUDE tenant-landlord messages (where contractor is NOT sender or receiver)
                                // If both sender and receiver are tenant and landlord (and neither is contractor), exclude it
                                val isTenantLandlordMessage = (sender == tenant && receiver == landlord) || 
                                                              (sender == landlord && receiver == tenant)
                                val contractorInvolved = sender == normalizedEmail || receiver == normalizedEmail
                                
                                if (isTenantLandlordMessage && !contractorInvolved) {
                                    // This is a tenant-landlord message, exclude it
                                    false
                                } else {
                                    // Contractor sent it OR contractor received it OR tenant sent it (for assigned tenants, old format)
                                    sender == normalizedEmail || 
                                    receiver == normalizedEmail ||
                                    (sender == tenant && tenantEmails.contains(tenant))
                                }
                            }
                            
                            // Group by tenant (use tenantEmail for old messages, senderEmail/receiverEmail for new messages)
                            val tenantGroups = allFilteredMessages.groupBy { message ->
                                when {
                                    message.senderEmail.lowercase() == normalizedEmail -> message.receiverEmail.lowercase()
                                    message.receiverEmail.lowercase() == normalizedEmail -> message.senderEmail.lowercase()
                                    else -> message.tenantEmail.lowercase() // Old format
                                }
                            }
                            
                            // Only keep conversations where contractor has sent at least one message OR received at least one message
                            val conversationsWithContractorMessages = tenantGroups.filter { (_, messages) ->
                                messages.any { 
                                    it.senderEmail.lowercase() == normalizedEmail || 
                                    it.receiverEmail.lowercase() == normalizedEmail 
                                }
                            }
                            
                            val filteredMessages = conversationsWithContractorMessages.values.flatten()
                            android.util.Log.d("HomeViewModel", "Filtered ${filteredMessages.size} contractor-tenant messages from ${allMessages.size} total for contractor $normalizedEmail")
                            _allContractorTenantMessages.value = filteredMessages.sortedBy { message: DirectMessage -> message.timestamp }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("HomeViewModel", "Contractor-tenant messages observation cancelled")
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error observing contractor-tenant messages: ${e.message}", e)
                }
            }
        }
    }

    fun stopObservingAllContractorTenantMessages() {
        allContractorTenantMessagesJob?.cancel()
        allContractorTenantMessagesJob = null
        observedContractorTenantEmail = null
    }
    
    // Tenant-Contractor Messages (for viewing conversation with specific contractor)
    private val _tenantContractorMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val tenantContractorMessages: StateFlow<List<DirectMessage>> = _tenantContractorMessages.asStateFlow()
    private var tenantContractorMessagesJob: kotlinx.coroutines.Job? = null
    private var observedTenantContractorEmail: String? = null

    fun startObservingTenantContractorMessages(tenantEmail: String, contractorEmail: String) {
        val normalizedTenantEmail = tenantEmail.lowercase()
        val normalizedContractorEmail = contractorEmail.lowercase()
        val key = "$normalizedTenantEmail:$normalizedContractorEmail"

        if (observedTenantContractorEmail == key && tenantContractorMessagesJob?.isActive == true) {
            android.util.Log.d("HomeViewModel", "Already observing tenant-contractor messages for $key, skipping restart")
            return
        }

        tenantContractorMessagesJob?.cancel()
        observedTenantContractorEmail = key

        tenantContractorMessagesJob = viewModelScope.launch {
            if (useFirebase) {
                try {
                    // Use new generalized function to observe messages between tenant and contractor
                    firebaseRepository.observeDirectMessagesBetweenUsers(normalizedTenantEmail, normalizedContractorEmail).collect { messages ->
                        android.util.Log.d("HomeViewModel", "Received ${messages.size} messages between tenant $normalizedTenantEmail and contractor $normalizedContractorEmail")
                        _tenantContractorMessages.value = messages.sortedBy { it.timestamp }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("HomeViewModel", "Tenant-contractor messages observation cancelled")
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error observing tenant-contractor messages: ${e.message}", e)
                }
            }
        }
    }

    fun stopObservingTenantContractorMessages() {
        tenantContractorMessagesJob?.cancel()
        tenantContractorMessagesJob = null
        observedTenantContractorEmail = null
    }
    
    // Tenant Messages (for tenants viewing all their conversations)
    private val _allTenantMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val allTenantMessages: StateFlow<List<DirectMessage>> = _allTenantMessages.asStateFlow()
    private var allTenantMessagesJob: kotlinx.coroutines.Job? = null
    private var observedTenantEmail: String? = null

    fun startObservingAllTenantMessages(tenantEmail: String) {
        val normalizedEmail = tenantEmail.lowercase()

        if (observedTenantEmail == normalizedEmail && allTenantMessagesJob?.isActive == true) {
            android.util.Log.d("HomeViewModel", "Already observing all tenant messages for $normalizedEmail, skipping restart")
            return
        }

        allTenantMessagesJob?.cancel()
        observedTenantEmail = normalizedEmail

        allTenantMessagesJob = viewModelScope.launch {
            if (useFirebase) {
                try {
                    firebaseRepository.observeDirectMessagesByTenant(normalizedEmail).collect { messages ->
                        // Filter to only include messages where:
                        // 1. tenantEmail matches exactly (tenant is the recipient), AND
                        // 2. senderEmail is either the tenant (they sent it) or landlord/contractor (they received it)
                        // This ensures tenants only see messages where they are the intended recipient
                        val filteredMessages = messages.filter { message ->
                            message.tenantEmail.lowercase() == normalizedEmail
                        }
                        android.util.Log.d("HomeViewModel", "Received ${messages.size} tenant messages, filtered to ${filteredMessages.size} for $normalizedEmail")
                        _allTenantMessages.value = filteredMessages
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("HomeViewModel", "Tenant messages observation cancelled")
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error observing tenant messages: ${e.message}", e)
                }
            }
        }
    }

    fun stopObservingAllTenantMessages() {
        allTenantMessagesJob?.cancel()
        allTenantMessagesJob = null
        observedTenantEmail = null
    }
}

