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

    // All tickets (for landlords to see all)
    private val _allTickets = MutableStateFlow<List<Ticket>>(emptyList())
    
    // Filtered tickets based on user role
    private val _filteredTickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _filteredTickets.asStateFlow()
    
    private val _contractors = MutableStateFlow(MockData.mockContractors)
    val contractors: StateFlow<List<Contractor>> = _contractors.asStateFlow()

    // All jobs (for contractors to see all)
    private val _allJobs = MutableStateFlow<List<Job>>(emptyList())
    
    // Filtered jobs based on user role
    private val _filteredJobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _filteredJobs.asStateFlow()
    
    init {
        // Update filtered tickets when user or all tickets change
        viewModelScope.launch {
            combine(
                _currentUser,
                _allTickets
            ) { user, allTickets ->
                if (user != null) {
                    when (user.role) {
                        UserRole.TENANT -> {
                            // Tenants see only their own tickets
                            allTickets.filter { it.submittedBy == user.email }
                        }
                        UserRole.LANDLORD -> {
                            // Landlords see all tickets
                            allTickets
                        }
                        UserRole.CONTRACTOR -> {
                            // Contractors see tickets assigned to them
                            val contractorId = getContractorIdForUser(user)
                            allTickets.filter { 
                                it.assignedTo == contractorId || it.assignedContractor == contractorId
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
                    _allTickets.value = tickets
                }
            }
            
            viewModelScope.launch {
                firebaseRepository.observeJobs().collect { jobs ->
                    _allJobs.value = jobs
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

    fun createAccount(name: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _authError.value = null
            val firebaseResult = FirebaseAuthManager.createUserWithEmailAndPassword(email, password)
            
            firebaseResult.fold(
                onSuccess = { firebaseUser ->
                    val user = User(
                        email = firebaseUser.email ?: email,
                        role = role,
                        name = name
                    )
                    _currentUser.value = user
                    if (useFirebase) {
                        firebaseRepository.saveUser(user)
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
                            name = name
                        )
                        _currentUser.value = user
                        if (useFirebase) {
                            firebaseRepository.saveUser(user)
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
                firebaseRepository.saveTicket(ticket)
                // Real-time listener will update _allTickets
            } else {
                val updatedTickets = _allTickets.value + ticket
                _allTickets.value = updatedTickets
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    dataRepository.saveTickets(currentUser.email, updatedTickets)
                }
            }
        }
    }

    fun updateTicket(id: String, updates: Ticket) {
        viewModelScope.launch {
            if (useFirebase) {
                firebaseRepository.saveTicket(updates)
                // Real-time listener will update _allTickets
            } else {
                val updatedTickets = _allTickets.value.map { if (it.id == id) updates else it }
                _allTickets.value = updatedTickets
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    dataRepository.saveTickets(currentUser.email, updatedTickets)
                }
            }
        }
    }

    fun assignContractor(ticketId: String, contractorId: String) {
        viewModelScope.launch {
            val ticket = _allTickets.value.find { it.id == ticketId }
            if (ticket != null && ticket.assignedTo == null) {
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
}

