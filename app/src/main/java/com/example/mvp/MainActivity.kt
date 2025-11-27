package com.example.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mvp.data.FirebaseRepository
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mvp.data.*
import com.example.mvp.navigation.Screen
import com.example.mvp.ui.components.HomeBottomNavigation
import com.example.mvp.ui.components.TopNavigationBar
import com.example.mvp.ui.screens.*
import com.example.mvp.ui.screens.ContractorLandlordChatScreen
import com.example.mvp.ui.screens.ContractorLandlordConversationScreen
import com.example.mvp.ui.screens.JobCompletionScreen
import com.example.mvp.ui.theme.MVPTheme
import com.example.mvp.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MVPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeApp() {
    val viewModel: HomeViewModel = viewModel()
    val navController = rememberNavController()

    val currentUser by viewModel.currentUser.collectAsState()
    val tickets by viewModel.tickets.collectAsState()
    val allTickets by viewModel.allTickets.collectAsState()
    val contractors by viewModel.contractors.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val connections by viewModel.connections.collectAsState()
    val directMessages by viewModel.directMessages.collectAsState()
    val authErrorState by viewModel.authError.collectAsState()
    
    var selectedTenantEmail by remember { mutableStateOf<String?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Calculate unread message counts for landlords and contractors
    val allLandlordMessages by viewModel.allLandlordContractorMessages.collectAsState()
    val allTenantMessages by viewModel.allDirectMessages.collectAsState()
    val allContractorMessages by viewModel.allContractorLandlordMessages.collectAsState()
    
    // Track last viewed timestamps globally (persisted across app restarts)
    val globalLastViewedTimestamps = remember { mutableStateMapOf<String, String>() }
    var timestampsLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Helper function to save timestamps (debounced)
    fun saveTimestamps() {
        if (timestampsLoaded) {
            saveJob?.cancel()
            saveJob = scope.launch {
                kotlinx.coroutines.delay(500) // Debounce saves
                viewModel.saveLastViewedTimestamps(globalLastViewedTimestamps.toMap())
            }
        }
    }
    
    // Load persisted timestamps when app starts
    LaunchedEffect(Unit) {
        if (!timestampsLoaded) {
            val loaded = viewModel.loadLastViewedTimestamps()
            loaded.forEach { (key, value) ->
                globalLastViewedTimestamps[key] = value
            }
            timestampsLoaded = true
        }
    }
    
    // Calculate total unread count for landlords
    val landlordUnreadCount = remember(allLandlordMessages, allTenantMessages, currentUser, globalLastViewedTimestamps) {
        val user = currentUser
        if (user?.role == UserRole.LANDLORD) {
            val normalizedCurrentEmail = user.email.lowercase()
            
            // Count unread contractor messages
            val contractorUnread = allLandlordMessages
                .groupBy { it.contractorEmail.lowercase() }
                .map { (contractorEmail, messages) ->
                    messages.count { message ->
                        message.senderEmail.lowercase() != normalizedCurrentEmail &&
                        !message.readBy.contains(normalizedCurrentEmail)
                    }
                }
                .sum()
            
            // Count unread tenant messages
            val tenantUnread = allTenantMessages
                .filter { it.landlordEmail.lowercase() == normalizedCurrentEmail }
                .groupBy { it.tenantEmail.lowercase() }
                .map { (tenantEmail, messages) ->
                    messages.count { message ->
                        message.senderEmail.lowercase() != normalizedCurrentEmail &&
                        !message.readBy.contains(normalizedCurrentEmail)
                    }
                }
                .sum()
            
            contractorUnread + tenantUnread
        } else 0
    }
    
    // Calculate total unread count for contractors
    val contractorUnreadCount = remember(allContractorMessages, currentUser, globalLastViewedTimestamps) {
        val user = currentUser
        if (user?.role == UserRole.CONTRACTOR) {
            val normalizedCurrentEmail = user.email.lowercase()
            
            allContractorMessages
                .groupBy { it.landlordEmail.lowercase() }
                .map { (landlordEmail, messages) ->
                    messages.count { message ->
                        message.senderEmail.lowercase() != normalizedCurrentEmail &&
                        !message.readBy.contains(normalizedCurrentEmail)
                    }
                }
                .sum()
        } else 0
    }
    
    val totalUnreadCount = remember(timestampsLoaded, landlordUnreadCount, contractorUnreadCount, currentUser?.role) {
        // Only calculate unread count after timestamps are loaded
        if (!timestampsLoaded) {
            0
        } else {
            when (currentUser?.role) {
                UserRole.LANDLORD -> landlordUnreadCount
                UserRole.CONTRACTOR -> contractorUnreadCount
                else -> 0
            }
        }
    }

    Scaffold(
        topBar = {
            if (currentUser != null && currentRoute != Screen.Login.route) {
                TopNavigationBar(
                    currentRoute = currentRoute,
                    userRole = currentUser!!.role,
                    onNavigate = { route ->
                        when (route) {
                            "dashboard" -> navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                            "create_ticket" -> {
                                // Only allow tenants to create tickets
                                if (currentUser?.role == UserRole.TENANT) {
                                    navController.navigate(Screen.CreateTicket.route)
                                }
                            }
                            "marketplace" -> navController.navigate(Screen.Marketplace.createRoute(null))
                            "ai_diagnosis" -> navController.navigate(Screen.AIDiagnosis.route)
                            "contractor_dashboard" -> navController.navigate(Screen.ContractorDashboard.route)
                            "schedule" -> navController.navigate(Screen.Schedule.createRoute(null))
                            "contractor_messages" -> navController.navigate(Screen.ContractorMessages.route)
                            "landlord_messages" -> navController.navigate(Screen.LandlordMessages.route)
                            "history" -> navController.navigate(Screen.History.route)
                            "chat" -> navController.navigate(Screen.Chat.route)
                            "tenant_landlord" -> navController.navigate(Screen.TenantLandlord.route)
                            "landlord_tenants" -> navController.navigate(Screen.LandlordTenants.createRoute(null))
                            "rating" -> {
                                val completedJob = jobs.find { it.status == "completed" }
                                if (completedJob != null) {
                                    navController.navigate(Screen.Rating.createRoute(completedJob.id))
                                } else {
                                    navController.navigate(Screen.History.route)
                                }
                            }
                        }
                    },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    unreadMessageCount = totalUnreadCount
                )
            }
        },
        bottomBar = {
            if (currentUser != null && currentRoute != Screen.Login.route) {
                HomeBottomNavigation(
                    currentRoute = currentRoute,
                    userRole = currentUser!!.role,
                    onNavigate = { route ->
                        when (route) {
                            "dashboard" -> navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                            "create_ticket" -> {
                                // Only allow tenants to create tickets
                                if (currentUser?.role == UserRole.TENANT) {
                                    navController.navigate(Screen.CreateTicket.route)
                                }
                            }
                            "marketplace" -> navController.navigate(Screen.Marketplace.createRoute(null))
                            "landlord_messages" -> navController.navigate(Screen.LandlordMessages.route)
                            "contractor_dashboard" -> navController.navigate(Screen.ContractorDashboard.route)
                            "schedule" -> navController.navigate(Screen.Schedule.createRoute(null))
                            "contractor_messages" -> navController.navigate(Screen.ContractorMessages.route)
                            "history" -> navController.navigate(Screen.History.route)
                            "chat" -> navController.navigate(Screen.Chat.route)
                            "tenant_landlord" -> navController.navigate(Screen.TenantLandlord.route)
                            "landlord_tenants" -> navController.navigate(Screen.LandlordTenants.createRoute(null))
                            "tenant_review" -> navController.navigate(Screen.TenantReview.route)
                            "tenant_messages" -> navController.navigate(Screen.TenantMessages.route)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Login.route) {
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
                
                LoginScreen(
                    onLogin = { email, password, role, remember ->
                        viewModel.login(email, password, role, remember)
                    },
                    onCreateAccount = {
                        navController.navigate(Screen.CreateAccount.route)
                    },
                    authError = authErrorState
                )
            }

            composable(Screen.CreateAccount.route) {
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                
                CreateAccountScreen(
                    onBack = { navController.popBackStack() },
                    onCreateAccount = { name, email, password, role, address, city, state, companyName ->
                        viewModel.createAccount(name, email, password, role, address, city, state, companyName)
                    },
                    authError = authErrorState
                )
            }

            composable(Screen.Dashboard.route) {
                val scope = rememberCoroutineScope()
                when (currentUser?.role) {
                    UserRole.TENANT -> TenantDashboardScreen(
                        tickets = tickets,
                        jobs = jobs,
                        onCreateTicket = {
                            navController.navigate(Screen.CreateTicket.route)
                        },
                        onTicketClick = { ticketId ->
                            navController.navigate(Screen.TicketDetail.createRoute(ticketId))
                        },
                        onLeaveReview = { ticketId ->
                            // Navigate to review section and auto-open this ticket
                            navController.navigate(Screen.TenantReview.route) {
                                launchSingleTop = true
                            }
                            // After a short delay, navigate to the review detail
                            scope.launch {
                                kotlinx.coroutines.delay(300)
                                navController.navigate(Screen.ReviewDetail.createRoute(ticketId))
                            }
                        }
                    )
                    UserRole.LANDLORD -> {
                        var tenantUsersMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                        
                        // Load tenant user info for all tickets
                        LaunchedEffect(tickets) {
                            val usersMap = mutableMapOf<String, User>()
                            tickets.forEach { ticket ->
                                if (ticket.submittedByRole == UserRole.TENANT && !usersMap.containsKey(ticket.submittedBy)) {
                                    viewModel.getUserByEmail(ticket.submittedBy)?.let {
                                        usersMap[ticket.submittedBy] = it
                                    }
                                }
                            }
                            tenantUsersMap = usersMap
                        }
                        
                        val acceptedInvitations by viewModel.acceptedInvitations.collectAsState()
                        val allInvitations by viewModel.allInvitations.collectAsState()
                        
                        LaunchedEffect(currentUser?.email) {
                            currentUser?.email?.let { email ->
                                if (currentUser?.role == UserRole.LANDLORD) {
                                    viewModel.startObservingAcceptedInvitations(email)
                                    viewModel.startObservingAllInvitations(email)
                                }
                            }
                        }
                        
                        LandlordDashboardScreen(
                            tickets = tickets,
                            acceptedInvitations = acceptedInvitations,
                            allInvitations = allInvitations,
                            onTicketClick = { ticketId ->
                                // Mark ticket as viewed when landlord opens it
                                viewModel.markTicketAsViewedByLandlord(ticketId)
                                navController.navigate(Screen.LandlordTicketDetail.createRoute(ticketId))
                            },
                            onAIDiagnosis = {
                                navController.navigate(Screen.AIDiagnosis.route)
                            },
                            onMarketplace = {
                                navController.navigate(Screen.Marketplace.route)
                            },
                            onChatWithContractor = { ticketId ->
                                // Find the contractor for this ticket and navigate to chat
                                val ticket = tickets.find { it.id == ticketId }
                                val contractorId = ticket?.assignedContractor ?: ticket?.assignedTo ?: ""
                                navController.navigate(Screen.ContractorLandlordChat.createRoute(ticketId, contractorId))
                            },
                            tenantUsers = tenantUsersMap
                        )
                    }
                    UserRole.CONTRACTOR -> {
                        val invitations by viewModel.jobInvitations.collectAsState()
                        val allTickets by viewModel.allTickets.collectAsState()
                        val contractorId = viewModel.getContractorIdForUser(currentUser)
                        
                        // Start observing invitations when contractor is logged in
                        LaunchedEffect(contractorId) {
                            contractorId?.let {
                                viewModel.startObservingInvitations(it)
                            }
                        }
                        
                        // Load tenant users for invitation tickets
                        var tenantUsersMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                        LaunchedEffect(invitations, allTickets) {
                            val usersMap = mutableMapOf<String, User>()
                            invitations.forEach { invitation ->
                                val ticket = allTickets.find { it.id == invitation.ticketId }
                                ticket?.submittedBy?.let { tenantEmail ->
                                    val normalizedEmail = tenantEmail.lowercase()
                                    if (!usersMap.containsKey(normalizedEmail)) {
                                        // Get user by email (suspend function)
                                        val user = viewModel.getUserByEmail(tenantEmail)
                                        user?.let {
                                            usersMap[normalizedEmail] = it
                                        }
                                    }
                                }
                            }
                            tenantUsersMap = usersMap
                        }
                        
                        ContractorDashboardScreen(
                            jobs = jobs,
                            tickets = allTickets, // Use allTickets so invitations can find their tickets
                            invitations = invitations,
                            tenantUsers = tenantUsersMap, // Pass tenant users for location info
                            onJobClick = { jobId ->
                                navController.navigate(Screen.JobDetail.createRoute(jobId))
                            },
                            onAcceptInvitation = { invitationId ->
                                viewModel.acceptInvitation(invitationId)
                            },
                            onDeclineInvitation = { invitationId ->
                                viewModel.declineInvitation(invitationId)
                            },
                            onServiceClick = {
                                navController.navigate(Screen.ContractorService.route)
                            }
                        )
                    }
                    null -> {}
                }
            }

            composable(Screen.CreateTicket.route) {
                // Only tenants can create tickets
                if (currentUser?.role == UserRole.TENANT) {
                    val connections by viewModel.connections.collectAsState()
                    val hasConnectedLandlord = remember(connections, currentUser?.email) {
                        connections.any { 
                            it.tenantEmail.lowercase() == currentUser?.email?.lowercase() && 
                            it.status == ConnectionStatus.CONNECTED
                        }
                    }
                    
                    CreateTicketScreen(
                        onBack = { navController.popBackStack() },
                        onSubmit = { title, description, category, priority ->
                            // Double-check connection before submitting
                            if (hasConnectedLandlord) {
                                val dateStr = com.example.mvp.utils.DateUtils.getCurrentDateString()
                                val ticketId = "ticket-${System.currentTimeMillis()}"
                                val newTicket = Ticket(
                                    id = ticketId,
                                    title = title,
                                    description = description,
                                    category = category,
                                    status = TicketStatus.SUBMITTED,
                                    submittedBy = currentUser?.email ?: "",
                                    submittedByRole = currentUser?.role ?: UserRole.TENANT,
                                    aiDiagnosis = "AI Suggestion: $category - Auto-detected",
                                    createdAt = com.example.mvp.utils.DateUtils.getCurrentDateTimeString(),
                                    createdDate = dateStr,
                                    priority = priority,
                                    ticketNumber = "${System.currentTimeMillis() % 100000}"
                                )
                                viewModel.addTicket(newTicket)
                                // Navigate to the ticket detail page after submission
                                navController.navigate(Screen.TicketDetail.createRoute(ticketId)) {
                                    // Pop the create ticket screen from the back stack
                                    popUpTo(Screen.CreateTicket.route) { inclusive = true }
                                }
                            }
                        },
                        hasConnectedLandlord = hasConnectedLandlord
                    )
                } else {
                    // Redirect non-tenants to dashboard
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                }
            }

            composable(
                route = Screen.TicketDetail.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
                val ticket = tickets.find { it.id == ticketId }
                val contractor = ticket?.assignedTo?.let { 
                    contractors.find { c -> c.id == it }
                }
                var tenantUser by remember { mutableStateOf<User?>(null) }

                // Load tenant user info if ticket was submitted by a tenant
                LaunchedEffect(ticket?.submittedBy) {
                    if (ticket?.submittedByRole == UserRole.TENANT) {
                        ticket.submittedBy.let { tenantEmail ->
                            viewModel.getUserByEmail(tenantEmail)?.let {
                                tenantUser = it
                            }
                        }
                    }
                }

                if (ticket != null) {
                    // Find the job for this ticket to get rating info
                    val job = jobs.find { it.ticketId == ticketId }
                    val canRate = ticket.status == TicketStatus.COMPLETED && 
                                  ticket.rating == null && 
                                  ticket.assignedTo != null &&
                                  (currentUser?.role == UserRole.TENANT || currentUser?.role == UserRole.LANDLORD)
                    
                    val scope = rememberCoroutineScope()
                    TicketDetailScreen(
                        ticket = ticket,
                        contractor = contractor,
                        onBack = { navController.popBackStack() },
                        onAssignContractor = {
                            navController.navigate(Screen.Marketplace.createRoute(ticketId))
                        },
                        onScheduleVisit = {
                            navController.navigate(Screen.Schedule.createRoute(ticketId))
                        },
                        onMessageContractor = if (ticket.assignedTo != null && contractor?.email != null) {
                            {
                                // Navigate to tenant-contractor conversation
                                navController.navigate(Screen.TenantContractorConversation.createRoute(contractor.email ?: ""))
                            }
                        } else null,
                        onRateJob = if (canRate && job != null) {
                            {
                                navController.navigate(Screen.Rating.createRoute(job.id))
                            }
                        } else null,
                        userRole = currentUser?.role ?: UserRole.TENANT,
                        currentUserEmail = currentUser?.email,
                        currentUserName = currentUser?.name,
                        tenantUser = tenantUser
                    )
                }
            }

            composable(
                route = Screen.Marketplace.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val scope = rememberCoroutineScope()
                val ticketId = backStackEntry.arguments?.getString("ticketId")?.takeIf { it != "null" }
                val applications by viewModel.jobApplications.collectAsState()
                val ticketApplications = ticketId?.let { 
                    applications.filter { it.ticketId == ticketId } 
                } ?: emptyList()
                
                val ticket = ticketId?.let { allTickets.find { it.id == ticketId } }
                var tenantUser by remember { mutableStateOf<User?>(null) }
                var contractorUsersMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                
                LaunchedEffect(ticketId) {
                    ticketId?.let {
                        viewModel.startObservingJobApplications(it)
                        viewModel.startObservingInvitationsForTicket(it)
                    }
                }
                
                val invitationsByTicket: Map<String, List<JobInvitation>> by viewModel.invitationsByTicket.collectAsState()
                val ticketInvitations = ticketId?.let { invitationsByTicket[it] } ?: emptyList<JobInvitation>()
                
                var tenantUsersForMarketplace by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                
                // Load tenant user info
                LaunchedEffect(ticket?.submittedBy) {
                    ticket?.submittedBy?.let { tenantEmail ->
                        viewModel.getUserByEmail(tenantEmail)?.let {
                            tenantUser = it
                        }
                    }
                }
                
                // Load all tenant users for landlords
                LaunchedEffect(tickets, currentUser?.role) {
                    if (currentUser?.role == UserRole.LANDLORD) {
                        val allTenantUsers = mutableMapOf<String, User>()
                        tickets.forEach { ticket ->
                            if (ticket.submittedByRole == UserRole.TENANT && 
                                !allTenantUsers.containsKey(ticket.submittedBy)) {
                                viewModel.getUserByEmail(ticket.submittedBy)?.let {
                                    allTenantUsers[ticket.submittedBy] = it
                                }
                            }
                        }
                        tenantUsersForMarketplace = allTenantUsers
                    }
                }
                
                // Load contractor user info
                LaunchedEffect(contractors) {
                    val usersMap = mutableMapOf<String, User>()
                    contractors.forEach { contractor ->
                        contractor.email?.let { email ->
                            viewModel.getUserByEmail(email)?.let { user ->
                                usersMap[contractor.id] = user
                            }
                        }
                    }
                    contractorUsersMap = usersMap
                }
                
                MarketplaceScreen(
                    contractors = contractors,
                    tickets = if (currentUser?.role == UserRole.CONTRACTOR) {
                        // Contractors need to see all unassigned tickets in marketplace
                        allTickets
                    } else {
                        // Other users see only their own tickets
                        tickets
                    },
                    onContractorClick = { contractorId ->
                        navController.navigate(Screen.ContractorProfile.createRoute(contractorId))
                    },
                    onAssign = { contractorId ->
                        ticketId?.let {
                            viewModel.assignContractor(ticketId, contractorId)
                            navController.popBackStack()
                        }
                    },
                    onMessage = { ticketIdParam, contractorId ->
                        // Navigate to messages tab first, then open conversation with this contractor
                        // Get contractor email
                        val contractor = contractors.find { it.id == contractorId }
                        val contractorEmail = contractor?.email
                        
                        if (contractorEmail != null) {
                            // First navigate to messages tab to ensure it's selected in bottom nav
                            navController.navigate(Screen.LandlordMessages.route) {
                                launchSingleTop = true
                            }
                            // Then immediately navigate to the conversation
                            scope.launch {
                                kotlinx.coroutines.delay(150)
                                navController.navigate(
                                    Screen.ContractorLandlordConversation.createRoute("general", contractorEmail)
                                )
                            }
                        } else {
                            // Fallback: navigate to messages page
                            navController.navigate(Screen.LandlordMessages.route)
                        }
                    },
                    onApplyToJob = { jobTicketId ->
                        // For contractors applying to jobs - create an application
                        val user = currentUser
                        val contractorId = viewModel.getContractorIdForUser(user)
                        if (contractorId != null && user != null) {
                            val contractor = contractors.find { it.id == contractorId }
                            viewModel.applyToJob(
                                jobTicketId, 
                                contractorId,
                                contractor?.name ?: user.name,
                                user.email
                            )
                            navController.popBackStack()
                        }
                    },
                    userRole = currentUser?.role ?: UserRole.TENANT,
                    ticketId = ticketId,
                    applications = ticketApplications,
                    invitations = ticketInvitations,
                    tenantCity = tenantUser?.city,
                    tenantState = tenantUser?.state,
                    contractorUsers = contractorUsersMap,
                    tenantUsers = tenantUsersForMarketplace
                )
            }

            composable(
                route = Screen.AssignContractor.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
                val ticket = allTickets.find { it.id == ticketId }
                val invitationsByTicket: Map<String, List<JobInvitation>> by viewModel.invitationsByTicket.collectAsState()
                val ticketInvitations = invitationsByTicket[ticketId] ?: emptyList()
                
                var tenantUser by remember { mutableStateOf<User?>(null) }
                var contractorUsersMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                
                LaunchedEffect(ticketId) {
                    viewModel.startObservingInvitationsForTicket(ticketId)
                }
                
                // Load tenant user info
                LaunchedEffect(ticket?.submittedBy) {
                    ticket?.submittedBy?.let { tenantEmail ->
                        viewModel.getUserByEmail(tenantEmail)?.let {
                            tenantUser = it
                        }
                    }
                }
                
                // Load contractor user info
                LaunchedEffect(contractors) {
                    val usersMap = mutableMapOf<String, User>()
                    contractors.forEach { contractor ->
                        contractor.email?.let { email ->
                            viewModel.getUserByEmail(email)?.let { user ->
                                usersMap[contractor.id] = user
                            }
                        }
                    }
                    contractorUsersMap = usersMap
                }
                
                if (ticket != null) {
                    AssignContractorScreen(
                        ticket = ticket,
                        contractors = contractors,
                        invitations = ticketInvitations,
                        tenantUser = tenantUser,
                        contractorUsers = contractorUsersMap,
                        onBack = { navController.popBackStack() },
                        onContractorInfo = { contractorId ->
                            navController.navigate(Screen.ContractorProfile.createRoute(contractorId))
                        },
                        onApply = { contractorId ->
                            viewModel.assignContractor(ticketId, contractorId)
                        }
                    )
                } else {
                    // Ticket not found
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ticket not found")
                    }
                }
            }

            composable(
                route = Screen.LandlordTicketDetail.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scope = rememberCoroutineScope()
                val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
                val ticket = allTickets.find { it.id == ticketId }
                var tenantUser by remember { mutableStateOf<User?>(null) }
                
                // Load tenant user info
                LaunchedEffect(ticket?.submittedBy) {
                    ticket?.submittedBy?.let { tenantEmail ->
                        viewModel.getUserByEmail(tenantEmail)?.let {
                            tenantUser = it
                        }
                    }
                }
                
                // Observe invitations for this ticket
                LaunchedEffect(ticketId) {
                    if (ticketId.isNotEmpty()) {
                        viewModel.startObservingInvitationsForTicket(ticketId)
                    }
                }
                
                if (ticket != null) {
                    // Check if ticket has invitations
                    val invitationsByTicket: Map<String, List<JobInvitation>> by viewModel.invitationsByTicket.collectAsState()
                    val ticketInvitations = invitationsByTicket[ticketId] ?: emptyList()
                    val hasInvitations = ticketInvitations.isNotEmpty()
                    
                    LandlordTicketDetailScreen(
                        ticket = ticket,
                        tenantUser = tenantUser,
                        onExit = { 
                            // Navigate back to dashboard instead of just popping
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onMessageTenant = {
                            // Navigate to messages screen first, then open conversation with this tenant
                            tenantUser?.email?.let { tenantEmail ->
                                // First navigate to messages tab to ensure it's selected in bottom nav
                                navController.navigate(Screen.LandlordMessages.route) {
                                    launchSingleTop = true
                                }
                                // Then immediately navigate to the conversation
                                scope.launch {
                                    kotlinx.coroutines.delay(150)
                                    navController.navigate(Screen.TenantLandlordConversation.createRoute(tenantEmail))
                                }
                            }
                        },
                        onAssignContractor = {
                            navController.navigate(Screen.AssignContractor.createRoute(ticketId))
                        },
                        hasInvitations = hasInvitations
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ticket not found")
                    }
                }
            }

            composable(
                route = Screen.ContractorProfile.route,
                arguments = listOf(navArgument("contractorId") { type = NavType.StringType })
            ) { backStackEntry ->
                val contractorId = backStackEntry.arguments?.getString("contractorId") ?: ""
                val contractor = contractors.find { it.id == contractorId }
                if (contractor != null) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Contractor Profile") },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contractor.name.split(" ").map { it.first() }.joinToString(""),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = contractor.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = contractor.company,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Divider()
                            
                            // Rating
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Rating: ${String.format("%.1f", contractor.rating)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Completed Jobs
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Completed Jobs: ${contractor.completedJobs}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Divider()
                            
                            // Specializations
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Specializations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    contractor.specialization.forEach { spec ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = spec,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Divider()
                            
                            // Service Areas
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Service Areas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (contractor.serviceAreas.isEmpty()) {
                                    Text(
                                        text = "No service areas specified",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                } else {
                                    contractor.serviceAreas.forEach { (state, cities) ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = state,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    cities.forEach { city ->
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            shape = MaterialTheme.shapes.small
                                                        ) {
                                                            Text(
                                                                text = city,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            composable(Screen.ContractorDashboard.route) {
                val invitations by viewModel.jobInvitations.collectAsState()
                val contractorId = viewModel.getContractorIdForUser(currentUser)
                val contractor = contractors.find { it.id == contractorId }
                
                // Start observing invitations when contractor is logged in
                LaunchedEffect(contractorId) {
                    contractorId?.let {
                        viewModel.startObservingInvitations(it)
                    }
                }
                
                // Load tenant users for invitation tickets
                var tenantUsersMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                LaunchedEffect(invitations, allTickets) {
                    val usersMap = mutableMapOf<String, User>()
                    invitations.forEach { invitation ->
                        val ticket = allTickets.find { it.id == invitation.ticketId }
                        ticket?.submittedBy?.let { tenantEmail ->
                            val normalizedEmail = tenantEmail.lowercase()
                            if (!usersMap.containsKey(normalizedEmail)) {
                                // Get user by email (suspend function)
                                val user = viewModel.getUserByEmail(tenantEmail)
                                user?.let {
                                    usersMap[normalizedEmail] = it
                                    android.util.Log.d("MainActivity", "Loaded tenant user for $normalizedEmail: address=${it.address}, city=${it.city}, state=${it.state}")
                                } ?: run {
                                    android.util.Log.d("MainActivity", "Could not load tenant user for $normalizedEmail")
                                }
                            }
                        }
                    }
                    android.util.Log.d("MainActivity", "Loaded ${usersMap.size} tenant users for invitations")
                    tenantUsersMap = usersMap
                }
                
                // Get the latest contractor from the contractors list (reactive to updates)
                val latestContractor = remember(contractors, contractorId) {
                    contractors.find { it.id == contractorId }
                }
                
                ContractorDashboardScreen(
                    jobs = jobs,
                    tickets = allTickets, // Contractors need to see all tickets for invitations
                    invitations = invitations,
                    contractor = latestContractor ?: contractor, // Use latest contractor or fallback
                    tenantUsers = tenantUsersMap, // Pass tenant users for location info
                    onJobClick = { jobId ->
                        navController.navigate(Screen.JobDetail.createRoute(jobId))
                    },
                    onAcceptInvitation = { invitationId ->
                        viewModel.acceptInvitation(invitationId)
                    },
                    onDeclineInvitation = { invitationId ->
                        viewModel.declineInvitation(invitationId)
                    },
                    onServiceClick = {
                        navController.navigate(Screen.ContractorService.route)
                    }
                )
            }
            
            composable(Screen.ContractorMessages.route) {
                val allContractorMessages by viewModel.allContractorLandlordMessages.collectAsState()
                val allContractorTenantMessages by viewModel.allContractorTenantMessages.collectAsState()
                
                // Get landlord users for messages
                val landlordUsersMap = remember(allContractorMessages, connections) {
                    allContractorMessages
                        .map { it.landlordEmail }
                        .distinct()
                        .associateWith { landlordEmail ->
                            User(
                                email = landlordEmail,
                                role = UserRole.LANDLORD,
                                name = landlordEmail.split("@").first()
                            )
                        }
                }
                
                // Get tenant users for messages
                var tenantUsersMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                LaunchedEffect(allContractorTenantMessages) {
                    val usersMap = mutableMapOf<String, User>()
                    allContractorTenantMessages.forEach { message: com.example.mvp.data.DirectMessage ->
                        if (!usersMap.containsKey(message.tenantEmail)) {
                            viewModel.getUserByEmail(message.tenantEmail)?.let { user ->
                                usersMap[message.tenantEmail] = user
                            }
                        }
                    }
                    tenantUsersMap = usersMap
                }
                
                // Start observing messages when contractor is logged in
                // Use email as key - will restart only if email changes
                LaunchedEffect(currentUser?.email) {
                    currentUser?.email?.let { email ->
                        val normalizedEmail = email.trim().lowercase()
                        android.util.Log.d("MainActivity", "ContractorMessages screen: Starting/ensuring observation for: $normalizedEmail")
                        viewModel.startObservingAllContractorLandlordMessages(normalizedEmail)
                        viewModel.startObservingAllContractorTenantMessages(normalizedEmail)
                    } ?: run {
                        android.util.Log.w("MainActivity", "No current user email, cannot observe messages")
                    }
                }
                
                // Don't cleanup on dispose - keep observing while on messages tab
                // The ViewModel will handle preventing duplicate observations
                
                ContractorMessagesScreen(
                    messages = allContractorMessages,
                    tenantMessages = allContractorTenantMessages,
                    landlordUsers = landlordUsersMap,
                    tenantUsers = tenantUsersMap,
                    currentUserEmail = currentUser?.email?.lowercase() ?: "",
                    onLandlordClick = { landlordEmail, ticketId ->
                        // Always use "general" to show ALL messages with this landlord, regardless of ticket
                        navController.navigate(Screen.ContractorLandlordConversation.createRoute("general", landlordEmail))
                    },
                    onTenantClick = { tenantEmail ->
                        navController.navigate(Screen.ContractorTenantConversation.createRoute(tenantEmail))
                    },
                    globalLastViewedTimestamps = globalLastViewedTimestamps
                )
            }
            
            composable(Screen.ContractorService.route) {
                val user = currentUser
                val currentContractor = remember(user, contractors) {
                    user?.let { u ->
                        contractors.find { 
                            it.email == u.email || 
                            it.id == viewModel.getContractorIdForUser(u) 
                        }
                    }
                }
                ContractorServiceScreen(
                    currentSpecialization = currentContractor?.specialization ?: emptyList(),
                    currentServiceAreas = currentContractor?.serviceAreas ?: emptyMap(),
                    onSave = { specialization, serviceAreas ->
                        currentContractor?.let { contractor ->
                            viewModel.updateContractorService(contractor.id, specialization, serviceAreas)
                        } ?: run {
                            // If contractor doesn't exist yet, create one
                            user?.let { u ->
                                val contractorId = viewModel.getContractorIdForUser(u) ?: "contractor-${u.email.replace("@", "-").replace(".", "-")}"
                                viewModel.updateContractorService(contractorId, specialization, serviceAreas)
                            }
                        }
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Route for messages page without contractor (just show list)
            composable(Screen.LandlordMessages.route) {
                val allLandlordMessages by viewModel.allLandlordContractorMessages.collectAsState()
                val allTenantMessages by viewModel.allDirectMessages.collectAsState()
                
                // Debug logging
                LaunchedEffect(allTenantMessages.size) {
                    android.util.Log.d("MainActivity", "LandlordMessages: Received ${allTenantMessages.size} tenant messages")
                }
                
                // Get contractor users for messages
                val contractorUsersMap = remember(allLandlordMessages, contractors) {
                    allLandlordMessages
                        .map { it.contractorEmail }
                        .distinct()
                        .associateWith { contractorEmail ->
                            contractors.find { it.email?.lowercase() == contractorEmail.lowercase() }?.let { contractor ->
                                User(
                                    email = contractorEmail,
                                    role = com.example.mvp.data.UserRole.CONTRACTOR,
                                    name = contractor.name
                                )
                            } ?: User(
                                email = contractorEmail,
                                role = com.example.mvp.data.UserRole.CONTRACTOR,
                                name = contractorEmail.split("@").first()
                            )
                        }
                }
                
                // Get tenant users for messages
                var tenantUsersMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                LaunchedEffect(allTenantMessages, connections) {
                    val usersMap = mutableMapOf<String, User>()
                    allTenantMessages.forEach { message ->
                        if (!usersMap.containsKey(message.tenantEmail)) {
                            viewModel.getUserByEmail(message.tenantEmail)?.let {
                                usersMap[message.tenantEmail] = it
                            }
                        }
                    }
                    tenantUsersMap = usersMap
                }
                
                // Filter tenant messages to only show those for current landlord
                val landlordTenantMessages = remember(allTenantMessages, currentUser?.email) {
                    currentUser?.email?.let { landlordEmail ->
                        val normalizedLandlordEmail = landlordEmail.lowercase()
                        // Include messages where landlord is recipient OR sender
                        val filtered = allTenantMessages.filter { 
                            it.landlordEmail.lowercase() == normalizedLandlordEmail || 
                            it.senderEmail.lowercase() == normalizedLandlordEmail
                        }
                        android.util.Log.d("MainActivity", "Filtered ${filtered.size} tenant messages from ${allTenantMessages.size} total for landlord $normalizedLandlordEmail")
                        filtered
                    } ?: emptyList()
                }
                
                // Start observing messages when landlord is logged in
                LaunchedEffect(currentUser?.email) {
                    currentUser?.email?.let { email ->
                        viewModel.startObservingAllLandlordContractorMessages(email.lowercase())
                        // Also observe direct messages (tenant-landlord)
                        viewModel.startObservingAllDirectMessages(email.lowercase())
                    }
                }
                
                // Don't cleanup observations - keep them running while on messages tab
                // This prevents messages from flashing/disappearing when navigating
                
                LandlordMessagesScreen(
                    contractorMessages = allLandlordMessages,
                    tenantMessages = landlordTenantMessages,
                    contractorUsers = contractorUsersMap,
                    tenantUsers = tenantUsersMap,
                    currentUserEmail = currentUser?.email?.lowercase() ?: "",
                    onContractorClick = { contractorEmail, ticketId ->
                        if (ticketId != null && ticketId.isNotEmpty() && ticketId != "general") {
                            navController.navigate(Screen.ContractorLandlordConversation.createRoute(ticketId, contractorEmail))
                        } else {
                            navController.navigate(Screen.ContractorLandlordConversation.createRoute("general", contractorEmail))
                        }
                    },
                    onTenantClick = { tenantEmail ->
                        navController.navigate(Screen.TenantLandlordConversation.createRoute(tenantEmail))
                    }
                )
            }
            
            // Route for messages page with contractor (auto-open conversation)
            composable(
                route = Screen.LandlordMessagesWithContractor.route,
                arguments = listOf(navArgument("contractorEmail") { type = NavType.StringType })
            ) { backStackEntry ->
                val contractorEmailParam = backStackEntry.arguments?.getString("contractorEmail")?.takeIf { it.isNotEmpty() }
                val allLandlordMessages by viewModel.allLandlordContractorMessages.collectAsState()
                val allTenantMessages by viewModel.allDirectMessages.collectAsState()
                
                // Get contractor users for messages
                val contractorUsersMap = remember(allLandlordMessages, contractors) {
                    allLandlordMessages
                        .map { it.contractorEmail }
                        .distinct()
                        .associateWith { contractorEmail ->
                            contractors.find { it.email?.lowercase() == contractorEmail.lowercase() }?.let { contractor ->
                                User(
                                    email = contractorEmail,
                                    role = com.example.mvp.data.UserRole.CONTRACTOR,
                                    name = contractor.name
                                )
                            } ?: User(
                                email = contractorEmail,
                                role = com.example.mvp.data.UserRole.CONTRACTOR,
                                name = contractorEmail.split("@").first()
                            )
                        }
                }
                
                // Start observing messages when landlord is logged in (only once)
                LaunchedEffect(Unit) {
                    currentUser?.email?.let { email ->
                        viewModel.startObservingAllLandlordContractorMessages(email.lowercase())
                        viewModel.startObservingAllDirectMessages(email.lowercase())
                    }
                }
                
                // Don't cleanup observations - keep them running while on messages tab
                // This prevents messages from flashing/disappearing when navigating
                
                // Auto-navigate to conversation if contractorEmail is provided (only once per screen instance)
                var hasNavigated by remember { mutableStateOf(false) }
                LaunchedEffect(contractorEmailParam) {
                    // Only navigate if we have a contractorEmail, haven't navigated yet, and we're on this screen
                    if (contractorEmailParam != null && contractorEmailParam.isNotEmpty() && !hasNavigated) {
                        // Wait a bit for messages to load
                        kotlinx.coroutines.delay(300)
                        // Double-check we're still on this screen before navigating
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute?.contains("landlord_messages") == true && currentRoute.contains(contractorEmailParam)) {
                            navController.navigate(
                                Screen.ContractorLandlordConversation.createRoute("general", contractorEmailParam)
                            )
                            hasNavigated = true
                        }
                    }
                }
                
                // Get tenant users for messages
                var tenantUsersMapForMessages by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                LaunchedEffect(allTenantMessages, connections) {
                    val usersMap = mutableMapOf<String, User>()
                    allTenantMessages.forEach { message ->
                        if (!usersMap.containsKey(message.tenantEmail)) {
                            viewModel.getUserByEmail(message.tenantEmail)?.let {
                                usersMap[message.tenantEmail] = it
                            }
                        }
                    }
                    tenantUsersMapForMessages = usersMap
                }
                
                // Filter tenant messages to only show those for current landlord
                val landlordTenantMessagesForRoute = remember(allTenantMessages, currentUser?.email) {
                    currentUser?.email?.let { landlordEmail ->
                        allTenantMessages.filter { 
                            it.landlordEmail.lowercase() == landlordEmail.lowercase() 
                        }
                    } ?: emptyList()
                }
                
                LandlordMessagesScreen(
                    contractorMessages = allLandlordMessages,
                    tenantMessages = landlordTenantMessagesForRoute,
                    contractorUsers = contractorUsersMap,
                    tenantUsers = tenantUsersMapForMessages,
                    currentUserEmail = currentUser?.email?.lowercase() ?: "",
                    onContractorClick = { contractorEmail, ticketId ->
                        // Mark conversation as read immediately when clicking
                        allLandlordMessages
                            .filter { it.contractorEmail.lowercase() == contractorEmail.lowercase() }
                            .maxByOrNull { it.timestamp }
                            ?.let { latestMessage ->
                                if (latestMessage.timestamp.isNotEmpty()) {
                                    globalLastViewedTimestamps["contractor_${contractorEmail.lowercase()}"] = latestMessage.timestamp
                                    saveTimestamps()
                                }
                            }
                        
                        if (ticketId != null && ticketId.isNotEmpty() && ticketId != "general") {
                            // Pass contractorEmail as the other party (landlordEmail parameter name is misleading, it's actually the other party)
                            navController.navigate(Screen.ContractorLandlordConversation.createRoute(ticketId, contractorEmail))
                        } else {
                            // For general messaging, use "general" as ticketId and pass contractorEmail
                            navController.navigate(Screen.ContractorLandlordConversation.createRoute("general", contractorEmail))
                        }
                    },
                    onTenantClick = { tenantEmail ->
                        // Mark conversation as read immediately when clicking
                        allTenantMessages
                            .filter { it.tenantEmail.lowercase() == tenantEmail.lowercase() }
                            .maxByOrNull { it.timestamp }
                            ?.let { latestMessage ->
                                if (latestMessage.timestamp.isNotEmpty()) {
                                    globalLastViewedTimestamps["tenant_${tenantEmail.lowercase()}"] = latestMessage.timestamp
                                    saveTimestamps()
                                }
                            }
                        
                        navController.navigate(Screen.TenantLandlordConversation.createRoute(tenantEmail))
                    },
                    globalLastViewedTimestamps = globalLastViewedTimestamps
                )
            }

            composable(
                route = Screen.JobDetail.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                val allTicketsForJobDetail by viewModel.allTickets.collectAsState()
                val job = jobs.find { it.id == jobId }
                val ticket = job?.let { allTicketsForJobDetail.find { t -> t.id == it.ticketId } }

                if (job != null && ticket != null) {
                    // Load tenant user info
                    var tenantUser by remember { mutableStateOf<User?>(null) }
                    LaunchedEffect(ticket.submittedBy) {
                        if (ticket.submittedByRole == UserRole.TENANT) {
                            viewModel.getUserByEmail(ticket.submittedBy)?.let {
                                tenantUser = it
                            }
                        }
                    }
                    
                    // Get landlord email for this ticket
                    val connectionsForJobDetail by viewModel.connections.collectAsState()
                    val landlordEmail = remember(ticket.id, connectionsForJobDetail) {
                        viewModel.getLandlordEmailForTicket(ticket.id)
                    }
                    
                    val scope = rememberCoroutineScope()
                    
                    AssignedJobDetailScreen(
                        job = job,
                        ticket = ticket,
                        tenantUser = tenantUser,
                        onBack = { navController.popBackStack() },
                        onSchedule = { jobId ->
                            navController.navigate(Screen.ContractorScheduleJob.createRoute(jobId))
                        },
                        onComplete = { id ->
                            navController.navigate(Screen.JobCompletion.createRoute(id))
                        },
                        onMessageLandlord = {
                            // Navigate to contractor-landlord conversation
                            // Use "general" to show ALL messages with this landlord, regardless of ticket
                            android.util.Log.d("MainActivity", "Message Landlord clicked: landlordEmail=$landlordEmail, ticketId=${ticket.id}")
                            if (landlordEmail != null && landlordEmail.isNotEmpty()) {
                                // First navigate to messages tab
                                navController.navigate(Screen.ContractorMessages.route) {
                                    launchSingleTop = true
                                }
                                // Then navigate to conversation (use "general" to show all messages with this landlord)
                                scope.launch {
                                    kotlinx.coroutines.delay(150)
                                    navController.navigate(Screen.ContractorLandlordConversation.createRoute("general", landlordEmail))
                                }
                            } else {
                                android.util.Log.e("MainActivity", "Cannot navigate: landlordEmail is null or empty")
                            }
                        },
                        onMessageTenant = {
                            // Navigate to contractor-tenant conversation
                            val tenantEmail = ticket.submittedBy
                            if (tenantEmail.isNotEmpty() && ticket.submittedByRole == UserRole.TENANT) {
                                // First navigate to messages tab
                                navController.navigate(Screen.ContractorMessages.route) {
                                    launchSingleTop = true
                                }
                                // Then navigate to contractor-tenant conversation (not tenant-landlord)
                                scope.launch {
                                    kotlinx.coroutines.delay(150)
                                    // Navigate to contractor-tenant conversation
                                    navController.navigate(Screen.ContractorTenantConversation.createRoute(tenantEmail))
                                }
                            }
                        },
                        userRole = currentUser?.role ?: UserRole.CONTRACTOR
                    )
                } else {
                    // Job or ticket not found - show error and go back
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Job Not Found") },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
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
                                Text(
                                    text = "Job not found",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "The job you're looking for doesn't exist or has been removed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Button(onClick = { navController.popBackStack() }) {
                                    Text("Go Back")
                                }
                            }
                        }
                    }
                }
            }
            
            composable(
                route = Screen.ContractorScheduleJob.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                val job = jobs.find { it.id == jobId }
                val scope = rememberCoroutineScope()
                
                if (job != null) {
                    ContractorScheduleJobScreen(
                        job = job,
                        allJobs = jobs,
                        onDismiss = { navController.popBackStack() },
                        onConfirm = { date, time ->
                            android.util.Log.d("MainActivity", "Confirm schedule called for job $jobId with date=$date, time=$time")
                            viewModel.scheduleJob(jobId, date, time)
                            // Wait a bit for the save to complete before navigating
                            scope.launch {
                                kotlinx.coroutines.delay(500) // Give time for save to complete
                                navController.popBackStack()
                            }
                        }
                    )
                } else {
                    // Job not found - show error and go back
                    LaunchedEffect(Unit) {
                        android.util.Log.e("MainActivity", "Job not found: $jobId")
                        navController.popBackStack()
                    }
                }
            }
            
            composable(
                route = Screen.JobCompletion.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                val job = jobs.find { it.id == jobId }
                val scope = rememberCoroutineScope()
                if (job != null) {
                    JobCompletionScreen(
                        job = job,
                        onBack = { navController.popBackStack() },
                        onSubmit = { photos, notes ->
                            scope.launch {
                                // Complete the job
                                viewModel.completeJob(jobId, photos, notes)
                                // Wait a bit for the save to complete
                                kotlinx.coroutines.delay(500)
                                // Navigate back to dashboard (jobs page)
                                navController.navigate(Screen.Dashboard.route) {
                                    // Pop all screens up to and including the job completion screen
                                    popUpTo(Screen.Dashboard.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                } else {
                    // Job not found, go back
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            composable(
                route = Screen.JobDetailTicket.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
                val ticket = allTickets.find { it.id == ticketId }
                var isApplying by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                
                JobDetailScreen(
                    ticket = ticket,
                    onApply = {
                        isApplying = true
                        val user = currentUser
                        val contractorId = viewModel.getContractorIdForUser(user)
                        if (contractorId != null && ticket != null && user != null) {
                            val contractor = contractors.find { it.id == contractorId }
                            viewModel.applyToJob(
                                ticket.id,
                                contractorId,
                                contractor?.name ?: user.name,
                                user.email
                            )
                            // Wait a bit for the update to propagate
                            scope.launch {
                                kotlinx.coroutines.delay(500)
                                isApplying = false
                                navController.popBackStack()
                            }
                        } else {
                            isApplying = false
                        }
                    },
                    onBack = { navController.popBackStack() },
                    isApplying = isApplying
                )
            }

            composable(
                route = Screen.Schedule.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val routeTicketId = backStackEntry.arguments?.getString("ticketId")?.takeIf { it != "null" }
                // Show different schedule screen based on user role
                if (currentUser?.role == UserRole.CONTRACTOR) {
                    ContractorScheduleScreen(
                        jobs = jobs,
                        tickets = allTickets,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    ScheduleScreen(
                        tickets = tickets,
                        defaultTicketId = routeTicketId,
                        onBack = { navController.popBackStack() },
                        onConfirm = { date, time, ticketId ->
                            // Handle schedule confirmation - update ticket with scheduled date/time
                            ticketId?.let { id ->
                                viewModel.scheduleTicket(id, date, time)
                            }
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(
                route = Screen.Rating.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                val job = jobs.find { it.id == jobId }
                val contractor = job?.let { 
                    contractors.find { c -> c.id == it.contractorId }
                }

                val ticket = job?.let { tickets.find { t -> t.id == it.ticketId } }
                
                RatingScreen(
                    contractor = contractor,
                    completedJobs = jobs.filter { it.status == "completed" },
                    recentRatings = emptyList(), // TODO: Load from data
                    onBack = { 
                        // Navigate back to ticket detail if available, otherwise to dashboard
                        ticket?.let {
                            navController.navigate(Screen.TicketDetail.createRoute(it.id)) {
                                popUpTo(Screen.TicketDetail.createRoute(it.id)) { inclusive = false }
                            }
                        } ?: run {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                            }
                        }
                    },
                    onSubmit = { rating, comment ->
                        jobId.let { viewModel.addRating(it, rating) }
                    }
                )
            }

            composable(Screen.History.route) {
                val contractorId = if (currentUser?.role == UserRole.CONTRACTOR) {
                    viewModel.getContractorIdForUser(currentUser)
                } else {
                    null
                }
                // For landlords, filter allTickets to show tickets from their tenants (all connections, not just active)
                // For tenants, filter allTickets to show only their own tickets
                // This ensures history shows all tickets ever received/submitted
                val ticketsForHistory = when (currentUser?.role) {
                    UserRole.LANDLORD -> {
                        val landlordEmail = currentUser?.email?.lowercase()
                        val tenantEmails = connections
                            .filter { 
                                it.landlordEmail.lowercase() == landlordEmail &&
                                it.status != com.example.mvp.data.ConnectionStatus.REJECTED
                            }
                            .map { it.tenantEmail.lowercase() }
                            .toSet()
                        allTickets.filter { 
                            it.submittedByRole == UserRole.TENANT &&
                            it.submittedBy.lowercase() in tenantEmails
                        }
                    }
                    UserRole.TENANT -> {
                        // For tenants, show all tickets they've submitted (regardless of connection status)
                        val tenantEmail = currentUser?.email?.lowercase()
                        allTickets.filter { 
                            it.submittedBy.lowercase() == tenantEmail &&
                            it.submittedByRole == UserRole.TENANT
                        }
                    }
                    else -> tickets // For contractors, use the filtered tickets
                }
                HistoryScreen(
                    tickets = ticketsForHistory,
                    jobs = jobs,
                    contractors = contractors,
                    onBack = { navController.popBackStack() },
                    currentUserRole = currentUser?.role,
                    currentContractorId = contractorId
                )
            }

            composable(Screen.Chat.route) {
                // For contractors, show contractor-landlord chat screen
                if (currentUser?.role == UserRole.CONTRACTOR) {
                    val contractorId = viewModel.getContractorIdForUser(currentUser)
                    ContractorLandlordChatScreen(
                        tickets = allTickets,
                        contractorId = contractorId,
                        onTicketClick = { ticketId ->
                            navController.navigate(Screen.ContractorLandlordConversation.createRoute(ticketId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    ChatScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            
            composable(
                route = Screen.ContractorLandlordConversation.route,
                arguments = listOf(
                    navArgument("ticketId") { type = NavType.StringType },
                    navArgument("landlordEmail") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { backStackEntry ->
                val ticketIdParam = backStackEntry.arguments?.getString("ticketId") ?: ""
                // Normalize ticketId: empty string or "general" means general chat
                // But preserve original ticketId if provided (for sending messages with ticketId)
                val ticketId = if (ticketIdParam.isEmpty() || ticketIdParam == "general") "general" else ticketIdParam
                val originalTicketId = if (ticketIdParam.isNotEmpty() && ticketIdParam != "general") ticketIdParam else null
                // Note: "landlordEmail" parameter name is misleading - it's actually the other party's email
                // For contractors, it's the landlord's email; for landlords, it's the contractor's email
                val otherPartyEmailParam = backStackEntry.arguments?.getString("landlordEmail")?.takeIf { it.isNotEmpty() }
                val isGeneralChat = ticketId == "general"
                val ticket = if (!isGeneralChat) allTickets.find { it.id == ticketId } else null
                val messages by viewModel.contractorLandlordMessages.collectAsState()
                val allContractorMessages by viewModel.allContractorLandlordMessages.collectAsState()
                val allLandlordMessages by viewModel.allLandlordContractorMessages.collectAsState()
                
                // Determine the other party's email
                // Since we're always using "general" now, prioritize the route parameter
                // For general chats: use the parameter or find from messages
                val otherPartyEmail = remember(
                    ticketId, 
                    otherPartyEmailParam,
                    currentUser?.role,
                    allContractorMessages,
                    allLandlordMessages
                ) {
                    // Always use parameter if provided (for both general and ticket-specific navigation)
                    if (otherPartyEmailParam != null && otherPartyEmailParam.isNotEmpty()) {
                        otherPartyEmailParam
                    } else if (!isGeneralChat) {
                        // For ticket-specific navigation (though we use "general" now), get from ticket
                        if (currentUser?.role == UserRole.CONTRACTOR) {
                            viewModel.getLandlordEmailForTicket(ticketId)
                        } else {
                            // For landlords, get contractor email from ticket
                            ticket?.assignedTo?.let { contractorId ->
                                contractors.find { it.id == contractorId }?.email
                            }
                        }
                    } else {
                        // For general chat, find from all messages
                        val allMessages = if (currentUser?.role == UserRole.CONTRACTOR) {
                            allContractorMessages
                        } else {
                            allLandlordMessages
                        }
                        
                        if (allMessages.isNotEmpty()) {
                            // Get the other party's email based on current user's role
                            if (currentUser?.role == UserRole.CONTRACTOR) {
                                allMessages
                                    .groupBy { it.landlordEmail }
                                    .maxByOrNull { (_, messages) -> messages.maxOfOrNull { it.timestamp } ?: "" }
                                    ?.key
                            } else {
                                allMessages
                                    .groupBy { it.contractorEmail }
                                    .maxByOrNull { (_, messages) -> messages.maxOfOrNull { it.timestamp } ?: "" }
                                    ?.key
                            }
                        } else null
                    }
                }
                
                // Get existing messages from allContractorLandlordMessages immediately
                // This ensures we show messages right away, even before the observation starts
                val existingMessages = remember(allContractorMessages, allLandlordMessages, otherPartyEmail, currentUser?.role) {
                    val allMessages = if (currentUser?.role == UserRole.CONTRACTOR) {
                        allContractorMessages
                    } else {
                        allLandlordMessages
                    }
                    
                    if (otherPartyEmail != null) {
                        val normalizedOtherParty = otherPartyEmail.lowercase()
                        allMessages.filter { message ->
                            if (currentUser?.role == UserRole.CONTRACTOR) {
                                message.landlordEmail.lowercase() == normalizedOtherParty
                            } else {
                                message.contractorEmail.lowercase() == normalizedOtherParty
                            }
                        }.sortedBy { it.timestamp }
                    } else {
                        emptyList()
                    }
                }
                
                LaunchedEffect(ticketId, otherPartyEmail) {
                    // Handle both ticket-specific and general chats
                    if (ticketId.isNotEmpty() && otherPartyEmail != null) {
                        viewModel.startObservingContractorLandlordMessages(ticketId, otherPartyEmail)
                    }
                }
                
                DisposableEffect(Unit) {
                    onDispose {
                        // Don't clear messages on dispose - keep them for when user returns
                        // This allows the conversation to persist when navigating back
                    }
                }
                
                // Use messages from StateFlow, but fallback to existing messages if empty
                // This ensures we show messages immediately, even if observation hasn't started yet
                val displayMessages = remember(messages, existingMessages) {
                    if (messages.isNotEmpty()) {
                        messages
                    } else {
                        existingMessages
                    }
                }
                
                // Mark all messages as read when conversation screen opens
                LaunchedEffect(displayMessages.size, otherPartyEmail, currentUser?.email) {
                    val user = currentUser
                    if (messages.isNotEmpty() && user?.email != null) {
                        val readerEmail = user.email.lowercase()
                        val unreadMessageIds = messages
                            .filter { 
                                it.senderEmail.lowercase() != readerEmail &&
                                !it.readBy.contains(readerEmail)
                            }
                            .map { it.id }
                        
                        if (unreadMessageIds.isNotEmpty()) {
                            viewModel.markContractorLandlordMessagesAsRead(unreadMessageIds, readerEmail)
                        }
                    }
                }
                
                ContractorLandlordConversationScreen(
                    ticket = ticket,
                    messages = displayMessages,
                    currentUserEmail = currentUser?.email ?: "",
                    currentUserName = currentUser?.name ?: "",
                    onSendMessage = { messageText ->
                        otherPartyEmail?.let {
                            // When sending from a ticket page, preserve the original ticketId for reference
                            // But the conversation will show all messages regardless of ticketId
                            // If we don't have an original ticket, use "general"
                            val actualTicketId = originalTicketId ?: "general"
                            viewModel.sendContractorLandlordMessage(actualTicketId, it, messageText)
                        }
                    },
                    onBack = { 
                        // Return to the previous screen (where the button was clicked from)
                        navController.popBackStack()
                    }
                )
            }
            
            composable(
                route = Screen.ContractorLandlordChat.route,
                arguments = listOf(
                    navArgument("ticketId") { type = NavType.StringType },
                    navArgument("contractorId") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val ticketIdParam = backStackEntry.arguments?.getString("ticketId") ?: ""
                val contractorIdParam = backStackEntry.arguments?.getString("contractorId")?.takeIf { it.isNotEmpty() }
                val isGeneralChat = ticketIdParam == "general" || ticketIdParam.isEmpty()
                val ticket = if (!isGeneralChat) allTickets.find { it.id == ticketIdParam } else null
                val ticketId = if (isGeneralChat) "general" else ticketIdParam
                val messages by viewModel.contractorLandlordMessages.collectAsState()
                
                LaunchedEffect(ticketId) {
                    if (ticketId.isNotEmpty() && ticketId != "general") {
                        viewModel.startObservingContractorLandlordMessages(ticketId)
                    }
                }
                
                DisposableEffect(Unit) {
                    onDispose {
                        viewModel.clearContractorLandlordMessages()
                    }
                }
                
                val user = currentUser
                if (user != null) {
                    // Determine contractor ID: from route param, ticket assignment, or empty
                    val contractorId = contractorIdParam 
                        ?: ticket?.assignedTo 
                        ?: ticket?.assignedContractor
                        ?: ""
                    
                    // Get contractor email if contractorId is provided
                    val contractor = contractors.find { it.id == contractorId }
                    val contractorEmail = contractor?.email
                    
                    if (user.role == UserRole.LANDLORD && (contractorEmail != null || contractorId.isNotEmpty())) {
                        ContractorLandlordConversationScreen(
                            ticket = ticket,
                            messages = messages,
                            currentUserEmail = user.email,
                            currentUserName = user.name,
                            onSendMessage = { messageText ->
                                // Pass contractor email or ID - ViewModel will handle finding email
                                if (isGeneralChat) {
                                    // For general chat, use contractor email/ID directly
                                    viewModel.sendContractorLandlordMessage("general", contractorEmail ?: contractorId, messageText)
                                } else {
                                    viewModel.sendContractorLandlordMessage(ticketId, contractorEmail ?: contractorId, messageText)
                                }
                            },
                            onBack = { 
                                // Navigate back to marketplace
                                navController.navigate(Screen.Marketplace.createRoute(null))
                            }
                        )
                    } else if (user.role == UserRole.CONTRACTOR) {
                        // For contractors viewing chat
                        ContractorLandlordConversationScreen(
                            ticket = ticket,
                            messages = messages,
                            currentUserEmail = user.email,
                            currentUserName = user.name,
                            onSendMessage = { messageText ->
                                // For contractors, find landlord email from ticket
                                if (isGeneralChat) {
                                    // For general chat, we need landlord email from contractor's perspective
                                    // This might need to be handled differently
                                    viewModel.sendContractorLandlordMessage("general", "", messageText)
                                } else {
                                    val landlordEmail = viewModel.getLandlordEmailForTicket(ticketId) ?: ""
                                    viewModel.sendContractorLandlordMessage(ticketId, landlordEmail, messageText)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            composable(Screen.AIDiagnosis.route) {
                val ticketsWithAI = tickets.filter { 
                    it.aiDiagnosis != null && it.status == TicketStatus.SUBMITTED 
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "AI Diagnosis",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                    items(ticketsWithAI) { ticket ->
                        Card(
                            onClick = {
                                navController.navigate(Screen.TicketDetail.createRoute(ticket.id))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = ticket.title)
                                Text(text = ticket.aiDiagnosis ?: "")
                                Button(
                                    onClick = {
                                        navController.navigate(Screen.Marketplace.createRoute(null))
                                    }
                                ) {
                                    Text("Assign Contractor")
                                }
                            }
                        }
                    }
                }
            }
            
            composable(Screen.TenantMessages.route) {
                // Refresh connections when entering this screen
                LaunchedEffect(Unit) {
                    viewModel.refreshConnections()
                }
                
                val allTenantMessages by viewModel.allTenantMessages.collectAsState()
                
                // Start observing all messages for tenant
                LaunchedEffect(currentUser?.email) {
                    currentUser?.email?.let { email ->
                        val normalizedEmail = email.trim().lowercase()
                        android.util.Log.d("MainActivity", "TenantMessages: Starting observation for: $normalizedEmail")
                        viewModel.startObservingAllTenantMessages(normalizedEmail)
                    }
                }
                
                // Get landlord from connections
                val normalizedTenantEmail = currentUser?.email?.trim()?.lowercase()
                val connectedLandlord = connections.find { 
                    it.status == ConnectionStatus.CONNECTED && 
                    it.tenantEmail.trim().lowercase() == normalizedTenantEmail
                }
                
                // Get landlord user
                var landlordUser: User? = null
                LaunchedEffect(connectedLandlord?.landlordEmail) {
                    connectedLandlord?.landlordEmail?.let { landlordEmail ->
                        viewModel.getUserByEmail(landlordEmail)?.let {
                            landlordUser = it
                        }
                    }
                }
                
                // Get contractor users from messages (contractors who sent messages to this tenant)
                var contractorUsersMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
                LaunchedEffect(allTenantMessages, contractors) {
                    val usersMap = mutableMapOf<String, User>()
                    allTenantMessages.forEach { message ->
                        // Check if sender is a contractor (not the tenant, not the landlord)
                        val senderEmail = message.senderEmail.lowercase()
                        val landlordEmail = connectedLandlord?.landlordEmail?.lowercase() ?: ""
                        val tenantEmail = normalizedTenantEmail ?: ""
                        
                        if (senderEmail != tenantEmail && senderEmail != landlordEmail) {
                            // This might be a contractor
                            if (!usersMap.containsKey(senderEmail)) {
                                // Try to find contractor by email
                                contractors.find { it.email?.lowercase() == senderEmail }?.let { contractor ->
                                    usersMap[senderEmail] = User(
                                        email = contractor.email ?: senderEmail,
                                        role = UserRole.CONTRACTOR,
                                        name = contractor.name
                                    )
                                } ?: run {
                                    // If not found in contractors list, create a basic user
                                    usersMap[senderEmail] = User(
                                        email = senderEmail,
                                        role = UserRole.CONTRACTOR,
                                        name = senderEmail.split("@").first().replaceFirstChar { it.uppercase() }
                                    )
                                }
                            }
                        }
                    }
                    contractorUsersMap = usersMap
                }
                
                TenantMessagesScreen(
                    messages = allTenantMessages,
                    landlordUser = landlordUser,
                    contractorUsers = contractorUsersMap,
                    currentUserEmail = currentUser?.email ?: "",
                    onLandlordClick = {
                        // Navigate to conversation with landlord
                        navController.navigate(Screen.TenantLandlordConversation.createRoute(currentUser?.email))
                    },
                    onContractorClick = { contractorEmail ->
                        // Navigate to tenant-contractor conversation
                        navController.navigate(Screen.TenantContractorConversation.createRoute(contractorEmail))
                    }
                )
            }
            
            composable(
                route = Screen.TenantContractorConversation.route,
                arguments = listOf(navArgument("contractorEmail") { type = NavType.StringType })
            ) { backStackEntry ->
                val contractorEmailParam = backStackEntry.arguments?.getString("contractorEmail") ?: ""
                val scope = rememberCoroutineScope()
                
                // Get contractor info
                val contractor = contractors.find { it.email?.lowercase() == contractorEmailParam.lowercase() }
                val contractorName = contractor?.name ?: contractorEmailParam.split("@").first().replaceFirstChar { it.uppercase() }
                
                // Get tenant email
                val tenantEmail = currentUser?.email ?: ""
                
                // Start observing messages
                LaunchedEffect(tenantEmail, contractorEmailParam) {
                    if (tenantEmail.isNotEmpty() && contractorEmailParam.isNotEmpty()) {
                        viewModel.startObservingTenantContractorMessages(tenantEmail, contractorEmailParam)
                    }
                }
                
                // Cleanup when leaving
                DisposableEffect(Unit) {
                    onDispose {
                        viewModel.stopObservingTenantContractorMessages()
                    }
                }
                
                val messages by viewModel.tenantContractorMessages.collectAsState()
                
                // Mark messages as read
                LaunchedEffect(messages.size, currentUser?.email) {
                    val user = currentUser
                    if (messages.isNotEmpty() && user?.email != null) {
                        val readerEmail = user.email.lowercase()
                        val unreadMessageIds = messages
                            .filter {
                                it.senderEmail.lowercase() != readerEmail &&
                                !it.readBy.contains(readerEmail)
                            }
                            .map { it.id }
                        
                        if (unreadMessageIds.isNotEmpty()) {
                            viewModel.markDirectMessagesAsRead(unreadMessageIds, readerEmail)
                        }
                    }
                }
                
                TenantContractorConversationScreen(
                    contractorEmail = contractorEmailParam,
                    contractorName = contractorName,
                    messages = messages,
                    currentUserEmail = currentUser?.email ?: "",
                    currentUserName = currentUser?.name ?: "",
                    onSendMessage = { messageText ->
                        viewModel.sendMessageToContractor(contractorEmailParam, messageText)
                    },
                    onBack = {
                        // Check if we came from ticket detail
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        val cameFromTicket = previousRoute?.startsWith("ticket_detail/") == true
                        
                        if (cameFromTicket) {
                            // Return to ticket detail
                            navController.popBackStack()
                        } else {
                            // Return to messages tab
                            navController.navigate(Screen.TenantMessages.route) {
                                popUpTo(Screen.TenantMessages.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.ContractorTenantConversation.route,
                arguments = listOf(navArgument("tenantEmail") { type = NavType.StringType })
            ) { backStackEntry ->
                val tenantEmailParam = backStackEntry.arguments?.getString("tenantEmail") ?: ""
                val scope = rememberCoroutineScope()
                
                // Get contractor email
                val contractorEmail = currentUser?.email ?: ""
                
                // Get tenant info
                var tenantUser by remember { mutableStateOf<User?>(null) }
                LaunchedEffect(tenantEmailParam) {
                    if (tenantEmailParam.isNotEmpty()) {
                        viewModel.getUserByEmail(tenantEmailParam)?.let {
                            tenantUser = it
                        }
                    }
                }
                val tenantName = tenantUser?.name ?: tenantEmailParam.split("@").first().replaceFirstChar { it.uppercase() }
                
                // Start observing messages between contractor and tenant (excludes landlord messages)
                LaunchedEffect(contractorEmail, tenantEmailParam) {
                    if (contractorEmail.isNotEmpty() && tenantEmailParam.isNotEmpty()) {
                        viewModel.startObservingTenantContractorMessages(tenantEmailParam, contractorEmail)
                    }
                }
                
                // Cleanup when leaving
                DisposableEffect(Unit) {
                    onDispose {
                        viewModel.stopObservingTenantContractorMessages()
                    }
                }
                
                val messages by viewModel.tenantContractorMessages.collectAsState()
                
                // Get existing messages from allContractorTenantMessages immediately
                val allContractorTenantMessages by viewModel.allContractorTenantMessages.collectAsState()
                val existingTenantMessages = remember(allContractorTenantMessages, contractorEmail, tenantEmailParam) {
                    val normalizedContractorEmail = contractorEmail.lowercase()
                    val normalizedTenantEmail = tenantEmailParam.lowercase()
                    allContractorTenantMessages.filter { message ->
                        val sender = message.senderEmail.lowercase()
                        val receiver = message.receiverEmail.lowercase()
                        val landlordEmail = message.landlordEmail.lowercase()
                        
                        // Only include messages between contractor and tenant (exclude landlord)
                        val isContractorToTenant = sender == normalizedContractorEmail && receiver == normalizedTenantEmail
                        val isTenantToContractor = sender == normalizedTenantEmail && receiver == normalizedContractorEmail
                        
                        (isContractorToTenant || isTenantToContractor) && 
                        landlordEmail != sender && landlordEmail != receiver
                    }.sortedBy { it.timestamp }
                }
                
                // Filter out landlord messages - only show messages between contractor and tenant
                // Use messages from StateFlow, but fallback to existing messages if empty
                val filteredMessages = remember(messages, existingTenantMessages, contractorEmail, tenantEmailParam) {
                    val normalizedContractorEmail = contractorEmail.lowercase()
                    val normalizedTenantEmail = tenantEmailParam.lowercase()
                    val allMessages = if (messages.isNotEmpty()) messages else existingTenantMessages
                    allMessages.filter { message ->
                        val sender = message.senderEmail.lowercase()
                        val receiver = message.receiverEmail.lowercase()
                        val landlordEmail = message.landlordEmail.lowercase()
                        
                        // Only include messages between contractor and tenant (exclude landlord)
                        val isContractorToTenant = sender == normalizedContractorEmail && receiver == normalizedTenantEmail
                        val isTenantToContractor = sender == normalizedTenantEmail && receiver == normalizedContractorEmail
                        
                        (isContractorToTenant || isTenantToContractor) && 
                        landlordEmail != sender && landlordEmail != receiver
                    }
                }
                
                // Mark messages as read
                LaunchedEffect(filteredMessages.size, currentUser?.email) {
                    val user = currentUser
                    if (filteredMessages.isNotEmpty() && user?.email != null) {
                        val readerEmail = user.email.lowercase()
                        val unreadMessageIds = filteredMessages
                            .filter {
                                it.senderEmail.lowercase() != readerEmail &&
                                !it.readBy.contains(readerEmail)
                            }
                            .map { it.id }
                        
                        if (unreadMessageIds.isNotEmpty()) {
                            viewModel.markDirectMessagesAsRead(unreadMessageIds, readerEmail)
                        }
                    }
                }
                
                TenantContractorConversationScreen(
                    contractorEmail = tenantEmailParam, // Pass tenant email (screen shows "Chat with [name]")
                    contractorName = tenantName, // Pass tenant name
                    messages = filteredMessages,
                    currentUserEmail = contractorEmail,
                    currentUserName = currentUser?.name ?: "",
                    onSendMessage = { messageText ->
                        // Contractor sends message to tenant using sendDirectMessage
                        viewModel.sendDirectMessage(tenantEmailParam, messageText)
                    },
                    onBack = {
                        // Return to the previous screen (where the button was clicked from)
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.TenantLandlord.route) {
                // Refresh connections when entering this screen
                LaunchedEffect(Unit) {
                    viewModel.refreshConnections()
                }
                
                // Normalize email for comparison (connections are stored with lowercase emails)
                val normalizedTenantEmail = currentUser?.email?.trim()?.lowercase()
                val pendingConnections = connections.filter { 
                    it.status == ConnectionStatus.PENDING && 
                    it.tenantEmail.trim().lowercase() == normalizedTenantEmail
                }
                val connectedLandlord = connections.find { 
                    it.status == ConnectionStatus.CONNECTED && 
                    it.tenantEmail.trim().lowercase() == normalizedTenantEmail
                }
                
                LaunchedEffect(connectedLandlord?.landlordEmail) {
                    connectedLandlord?.landlordEmail?.let { landlordEmail ->
                        currentUser?.email?.let { tenantEmail ->
                            viewModel.startObservingDirectMessages(tenantEmail)
                        }
                    }
                }
                
                TenantLandlordScreen(
                    connection = connectedLandlord,
                    pendingConnections = pendingConnections,
                    messages = directMessages,
                    currentUserEmail = currentUser?.email ?: "",
                    currentUserName = currentUser?.name ?: "",
                    onConfirmConnection = { connectionId, accept ->
                        viewModel.confirmConnection(connectionId, accept)
                    },
                    onSendMessage = { messageText ->
                        // For tenants, use their own email
                        val emailToUse = if (currentUser?.role == com.example.mvp.data.UserRole.TENANT) {
                            currentUser?.email ?: ""
                        } else {
                            ""
                        }
                        viewModel.sendDirectMessage(emailToUse, messageText)
                    },
                    onOpenChat = {
                        navController.navigate(Screen.TenantLandlordConversation.route)
                    },
                    onBack = { navController.popBackStack() },
                    globalLastViewedTimestamps = globalLastViewedTimestamps
                )
            }
            
            composable(
                route = Screen.TenantLandlordConversation.route,
                arguments = listOf(navArgument("tenantEmail") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val tenantEmailParam = backStackEntry.arguments?.getString("tenantEmail")?.takeIf { it != "general" }
                
                // Refresh connections when entering this screen
                LaunchedEffect(Unit) {
                    viewModel.refreshConnections()
                }
                
                // Determine tenant email: 
                // For TENANTS: ALWAYS use currentUser.email to ensure messages are isolated per tenant account
                // For LANDLORDS: use route param (tenantEmailParam) to view specific tenant's messages
                val user = currentUser // Extract to local variable for smart cast
                val tenantEmail = if (user?.role == com.example.mvp.data.UserRole.TENANT) {
                    // For tenants, always use their own email - ignore route parameters
                    user.email ?: ""
                } else {
                    // For landlords, use route parameter to view specific tenant's messages
                    tenantEmailParam ?: ""
                }
                
                android.util.Log.d("MainActivity", "TenantLandlordConversation: role=${user?.role}, tenantEmail=$tenantEmail, tenantEmailParam=$tenantEmailParam, currentUserEmail=${user?.email}")
                
                // Normalize email for comparison (connections are stored with lowercase emails)
                val normalizedTenantEmail = tenantEmail.trim().lowercase()
                val connectedLandlord = connections.find { 
                    it.status == ConnectionStatus.CONNECTED && 
                    it.tenantEmail.trim().lowercase() == normalizedTenantEmail
                }
                
                // IMPORTANT: Use tenantEmailParam directly in LaunchedEffect key to ensure it restarts when route changes
                LaunchedEffect(tenantEmailParam, user?.email, user?.role) {
                    // For tenants, always use user.email to ensure isolation
                    // For landlords and contractors, use tenantEmailParam from route to view specific tenant
                    val emailToObserve = when (user?.role) {
                        com.example.mvp.data.UserRole.TENANT -> user?.email ?: ""
                        com.example.mvp.data.UserRole.CONTRACTOR -> tenantEmailParam ?: ""
                        else -> tenantEmailParam ?: ""
                    }
                    if (emailToObserve.isNotEmpty()) {
                        android.util.Log.d("MainActivity", "Observing messages for tenant: $emailToObserve (role=${user?.role}, tenantEmailParam=$tenantEmailParam)")
                        // Stop previous observation and clear messages before starting new one for this tenant
                        viewModel.clearDirectMessages()
                        viewModel.startObservingDirectMessages(emailToObserve)
                    } else {
                        android.util.Log.d("MainActivity", "Not observing - emailToObserve is empty")
                        viewModel.clearDirectMessages()
                    }
                }
                
                // Cleanup when leaving this screen
                DisposableEffect(tenantEmailParam) {
                    onDispose {
                        android.util.Log.d("MainActivity", "Disposing TenantLandlordConversation for tenant: $tenantEmailParam")
                        viewModel.clearDirectMessages()
                    }
                }
                
                val directMessages by viewModel.directMessages.collectAsState()
                
                // Mark all messages as read when conversation screen opens
                LaunchedEffect(directMessages.size, tenantEmail, currentUser?.email) {
                    val user = currentUser
                    if (directMessages.isNotEmpty() && user?.email != null) {
                        val readerEmail = user.email.lowercase()
                        val unreadMessageIds = directMessages
                            .filter { 
                                it.senderEmail.lowercase() != readerEmail &&
                                !it.readBy.contains(readerEmail)
                            }
                            .map { it.id }
                        
                        if (unreadMessageIds.isNotEmpty()) {
                            viewModel.markDirectMessagesAsRead(unreadMessageIds, readerEmail)
                        }
                    }
                }
                
                TenantLandlordConversationScreen(
                    landlordEmail = connectedLandlord?.landlordEmail ?: user?.email?.takeIf {
                        user?.role == com.example.mvp.data.UserRole.LANDLORD
                    } ?: "",
                    tenantEmail = tenantEmail, // Pass tenant email
                    messages = directMessages,
                    currentUserEmail = user?.email ?: "",
                    currentUserName = user?.name ?: "",
                    currentUserRole = user?.role, // Pass user role
                    onSendMessage = { messageText ->
                        // For tenants, always use user.email to ensure messages are isolated
                        val emailToUse = when (user?.role) {
                            com.example.mvp.data.UserRole.TENANT -> user?.email ?: ""
                            com.example.mvp.data.UserRole.CONTRACTOR -> tenantEmail
                            else -> tenantEmail
                        }
                        android.util.Log.d("MainActivity", "Sending message: tenantEmail=$emailToUse (role=${user?.role})")
                        viewModel.sendDirectMessage(emailToUse, messageText)
                    },
                    onBack = {
                        // Navigate back based on user role and where we came from
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        val cameFromTicket = previousRoute?.startsWith("ticket_detail/") == true
                        
                        when (currentUser?.role) {
                            com.example.mvp.data.UserRole.LANDLORD -> {
                                navController.navigate(Screen.LandlordMessages.route) {
                                    popUpTo(Screen.LandlordMessages.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            com.example.mvp.data.UserRole.CONTRACTOR -> {
                                navController.navigate(Screen.ContractorMessages.route) {
                                    popUpTo(Screen.ContractorMessages.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            com.example.mvp.data.UserRole.TENANT -> {
                                if (cameFromTicket) {
                                    // Return to ticket detail
                                    navController.popBackStack()
                                } else {
                                    // Return to messages tab
                                    navController.navigate(Screen.TenantMessages.route) {
                                        popUpTo(Screen.TenantMessages.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            }
                            else -> {
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.LandlordTenants.route,
                arguments = listOf(navArgument("tenantEmail") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val tenantEmailFromRoute = backStackEntry.arguments?.getString("tenantEmail")?.takeIf { it != "null" }
                val cameFromTicketDetails = remember { mutableStateOf(false) }
                
                // Refresh connections when entering this screen
                LaunchedEffect(Unit) {
                    viewModel.refreshConnections()
                }
                
                // Check if we came from ticket details by checking the previous route
                LaunchedEffect(Unit) {
                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                    cameFromTicketDetails.value = previousRoute?.startsWith("landlord_ticket_detail/") == true
                }
                
                // Auto-select tenant if provided in route (but only if it's not "null")
                LaunchedEffect(tenantEmailFromRoute) {
                    if (tenantEmailFromRoute != null && tenantEmailFromRoute.isNotEmpty() && tenantEmailFromRoute != "null") {
                        selectedTenantEmail = tenantEmailFromRoute
                    } else {
                        // Clear selection if navigating to base tenants screen
                        selectedTenantEmail = null
                    }
                }
                
                LaunchedEffect(selectedTenantEmail) {
                    val tenantEmail = selectedTenantEmail
                    if (tenantEmail != null && tenantEmail.isNotBlank() && tenantEmail != "null") {
                        android.util.Log.d("MainActivity", "Starting to observe messages for tenant: $tenantEmail")
                        viewModel.startObservingDirectMessages(tenantEmail)
                    } else {
                        android.util.Log.d("MainActivity", "Clearing messages - tenantEmail is null or blank")
                        viewModel.clearDirectMessagesData()
                    }
                }
                
                // Load tenant user info for all connections
                var tenantUsersMap by remember { mutableStateOf<Map<String, com.example.mvp.data.User>>(emptyMap()) }
                LaunchedEffect(connections) {
                    val usersMap = mutableMapOf<String, com.example.mvp.data.User>()
                    connections.forEach { connection ->
                        if (!usersMap.containsKey(connection.tenantEmail)) {
                            viewModel.getUserByEmail(connection.tenantEmail)?.let {
                                usersMap[connection.tenantEmail] = it
                            }
                        }
                    }
                    tenantUsersMap = usersMap
                }
                
                LandlordTenantsScreen(
                    connections = connections,
                    tenantUsers = tenantUsersMap,
                    onAddTenant = { tenantEmail ->
                        viewModel.requestTenantConnection(tenantEmail)
                        // Refresh connections after requesting
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(500) // Small delay to allow Firebase to save
                            viewModel.refreshConnections()
                        }
                    },
                    onSelectTenant = { tenantEmail ->
                        navController.navigate(Screen.TenantDetail.createRoute(tenantEmail))
                    },
                    onCancelConnection = { connectionId ->
                        viewModel.cancelConnectionRequest(connectionId)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = Screen.TenantDetail.route,
                arguments = listOf(navArgument("tenantEmail") { type = NavType.StringType })
            ) { backStackEntry ->
                val tenantEmail = backStackEntry.arguments?.getString("tenantEmail") ?: ""
                val scope = rememberCoroutineScope()
                val allTickets by viewModel.allTickets.collectAsState()
                
                // Get tenant user
                var tenantUser by remember { mutableStateOf<com.example.mvp.data.User?>(null) }
                LaunchedEffect(tenantEmail) {
                    viewModel.getUserByEmail(tenantEmail)?.let {
                        tenantUser = it
                    }
                }
                
                // Get tickets for this tenant
                val tenantTickets = remember(allTickets, tenantEmail) {
                    allTickets.filter { 
                        it.submittedBy.lowercase() == tenantEmail.lowercase() &&
                        it.submittedByRole == com.example.mvp.data.UserRole.TENANT
                    }
                }
                
                if (tenantUser != null) {
                    TenantDetailScreen(
                        tenant = tenantUser!!,
                        tickets = tenantTickets,
                        onBack = { navController.popBackStack() },
                        onTicketClick = { ticketId ->
                            navController.navigate(Screen.LandlordTicketDetail.createRoute(ticketId))
                        },
                        onMessageTenant = {
                            // Navigate to messages screen first, then open conversation with this tenant
                            // First navigate to messages tab to ensure it's selected in bottom nav
                            navController.navigate(Screen.LandlordMessages.route) {
                                launchSingleTop = true
                            }
                            // Then immediately navigate to the conversation
                            scope.launch {
                                kotlinx.coroutines.delay(150)
                                navController.navigate(Screen.TenantLandlordConversation.createRoute(tenantEmail))
                            }
                        }
                    )
                }
            }
            
            composable(Screen.TenantReview.route) {
                // Use allTickets to ensure we get the latest ticket updates (including ratings)
                val allTicketsForReview by viewModel.allTickets.collectAsState()
                val allJobsForReview = jobs // Use the jobs from the outer scope
                
                TenantReviewScreen(
                    tickets = allTicketsForReview,
                    jobs = allJobsForReview,
                    onTicketClick = { ticketId ->
                        navController.navigate(Screen.ReviewDetail.createRoute(ticketId))
                    },
                    currentUserEmail = currentUser?.email ?: ""
                )
            }
            
            composable(
                route = Screen.ReviewDetail.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
                // Use allTickets and allJobs to get the latest data
                val allTicketsForDetail by viewModel.allTickets.collectAsState()
                val allJobsForDetail by viewModel.jobs.collectAsState()
                val ticket = allTicketsForDetail.find { it.id == ticketId }
                val job = ticket?.let { t -> allJobsForDetail.find { j: com.example.mvp.data.Job -> j.ticketId == t.id } }
                
                if (ticket != null) {
                    val scope = rememberCoroutineScope()
                    ReviewDetailScreen(
                        ticket = ticket,
                        job = job,
                        onBack = { navController.popBackStack() },
                        onRate = { rating ->
                            scope.launch {
                                android.util.Log.d("MainActivity", "Submitting rating $rating for ticket $ticketId")
                                
                                try {
                                    // Update ticket rating first
                                    viewModel.updateTicketRating(ticketId, rating)
                                    android.util.Log.d("MainActivity", "Ticket rating updated")
                                    
                                    // Update job rating if job exists (which also updates contractor rating)
                                    job?.id?.let { jobId ->
                                        android.util.Log.d("MainActivity", "Updating job $jobId with rating $rating")
                                        viewModel.rateJob(jobId, rating)
                                        android.util.Log.d("MainActivity", "Job rating updated")
                                    } ?: run {
                                        android.util.Log.w("MainActivity", "No job found for ticket $ticketId, only updating ticket rating")
                                        // Even without a job, we should still update contractor rating from tickets
                                        // The contractor rating calculation will use tickets directly
                                    }
                                    
                                    // Wait a bit for the save to complete
                                    kotlinx.coroutines.delay(500)
                                    
                                    android.util.Log.d("MainActivity", "Rating submission complete, navigating back")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Error submitting rating: ${e.message}", e)
                                }
                                
                                // Navigate back to review screen (ticket will disappear from list)
                                navController.popBackStack()
                            }
                        }
                    )
                } else {
                    // Ticket not found - show error or navigate back
                    LaunchedEffect(Unit) {
                        android.util.Log.e("MainActivity", "Ticket not found: $ticketId")
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
