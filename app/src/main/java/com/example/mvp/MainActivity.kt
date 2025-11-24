package com.example.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                            "history" -> navController.navigate(Screen.History.route)
                            "chat" -> navController.navigate(Screen.Chat.route)
                            "tenant_landlord" -> navController.navigate(Screen.TenantLandlord.route)
                            "landlord_tenants" -> navController.navigate(Screen.LandlordTenants.route)
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
                    }
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
                            "contractor_dashboard" -> navController.navigate(Screen.ContractorDashboard.route)
                            "schedule" -> navController.navigate(Screen.Schedule.createRoute(null))
                            "history" -> navController.navigate(Screen.History.route)
                            "chat" -> navController.navigate(Screen.Chat.route)
                            "tenant_landlord" -> navController.navigate(Screen.TenantLandlord.route)
                            "landlord_tenants" -> navController.navigate(Screen.LandlordTenants.route)
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
                when (currentUser?.role) {
                    UserRole.TENANT -> TenantDashboardScreen(
                        tickets = tickets,
                        onCreateTicket = {
                            navController.navigate(Screen.CreateTicket.route)
                        },
                        onTicketClick = { ticketId ->
                            navController.navigate(Screen.TicketDetail.createRoute(ticketId))
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
                        
                        LandlordDashboardScreen(
                            tickets = tickets,
                            onTicketClick = { ticketId ->
                                navController.navigate(Screen.TicketDetail.createRoute(ticketId))
                            },
                            onAIDiagnosis = {
                                navController.navigate(Screen.AIDiagnosis.route)
                            },
                            onMarketplace = {
                                navController.navigate(Screen.Marketplace.route)
                            },
                            onChatWithContractor = { ticketId ->
                                navController.navigate("contractor_landlord_chat/$ticketId")
                            },
                            tenantUsers = tenantUsersMap
                        )
                    }
                    UserRole.CONTRACTOR -> ContractorDashboardScreen(
                        jobs = jobs,
                        onJobClick = { jobId ->
                            navController.navigate(Screen.JobDetail.createRoute(jobId))
                        }
                    )
                    null -> {}
                }
            }

            composable(Screen.CreateTicket.route) {
                // Only tenants can create tickets
                if (currentUser?.role == UserRole.TENANT) {
                    CreateTicketScreen(
                        onBack = { navController.popBackStack() },
                        onSubmit = { title, description, category, priority ->
                            val dateStr = com.example.mvp.utils.DateUtils.getCurrentDateString()
                            val newTicket = Ticket(
                                id = "ticket-${System.currentTimeMillis()}",
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
                        }
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
                    }
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
                    tenantCity = tenantUser?.city,
                    tenantState = tenantUser?.state,
                    contractorUsers = contractorUsersMap
                )
            }

            composable(
                route = Screen.ContractorProfile.route,
                arguments = listOf(navArgument("contractorId") { type = NavType.StringType })
            ) { backStackEntry ->
                val contractorId = backStackEntry.arguments?.getString("contractorId") ?: ""
                val contractor = contractors.find { it.id == contractorId }
                if (contractor != null) {
                    // Simple contractor profile view
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = contractor.name,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(text = contractor.company)
                        Text(text = "Rating: ${contractor.rating}")
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Back")
                        }
                    }
                }
            }

            composable(Screen.ContractorDashboard.route) {
                ContractorDashboardScreen(
                    jobs = jobs,
                    tickets = allTickets, // Contractors need to see all tickets to find available jobs
                    onJobClick = { jobId ->
                        navController.navigate(Screen.JobDetail.createRoute(jobId))
                    },
                    onApplyToJob = { ticketId ->
                        // Navigate to job detail screen for available jobs
                        navController.navigate(Screen.JobDetailTicket.createRoute(ticketId))
                    }
                )
            }

            composable(
                route = Screen.JobDetail.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                val job = jobs.find { it.id == jobId }
                val ticket = job?.let { tickets.find { t -> t.id == it.ticketId } }

                if (job != null && ticket != null) {
                    AssignedJobDetailScreen(
                        job = job,
                        ticket = ticket,
                        onBack = { navController.popBackStack() },
                        onComplete = { id ->
                            viewModel.completeJob(id)
                            navController.navigate(Screen.Rating.createRoute(id))
                        },
                        userRole = currentUser?.role ?: UserRole.CONTRACTOR
                    )
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
                HistoryScreen(
                    tickets = tickets,
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
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
                val ticket = allTickets.find { it.id == ticketId }
                val messages by viewModel.contractorLandlordMessages.collectAsState()
                
                LaunchedEffect(ticketId) {
                    if (ticketId.isNotEmpty()) {
                        viewModel.startObservingContractorLandlordMessages(ticketId)
                    }
                }
                
                DisposableEffect(Unit) {
                    onDispose {
                        viewModel.clearContractorLandlordMessages()
                    }
                }
                
                if (ticket != null) {
                    val landlordEmail = viewModel.getLandlordEmailForTicket(ticketId)
                    ContractorLandlordConversationScreen(
                        ticket = ticket,
                        messages = messages,
                        currentUserEmail = currentUser?.email ?: "",
                        currentUserName = currentUser?.name ?: "",
                        onSendMessage = { messageText ->
                            landlordEmail?.let {
                                viewModel.sendContractorLandlordMessage(ticketId, it, messageText)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            
            composable(
                route = Screen.ContractorLandlordChat.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
                val ticket = allTickets.find { it.id == ticketId }
                val messages by viewModel.contractorLandlordMessages.collectAsState()
                
                LaunchedEffect(ticketId) {
                    if (ticketId.isNotEmpty()) {
                        viewModel.startObservingContractorLandlordMessages(ticketId)
                    }
                }
                
                DisposableEffect(Unit) {
                    onDispose {
                        viewModel.clearContractorLandlordMessages()
                    }
                }
                
                val user = currentUser
                if (ticket != null && user?.role == UserRole.LANDLORD) {
                    // For landlord: use contractor ID as placeholder for email
                    val contractorId = ticket.assignedTo ?: ticket.assignedContractor ?: ""
                    
                    ContractorLandlordConversationScreen(
                        ticket = ticket,
                        messages = messages,
                        currentUserEmail = user.email,
                        currentUserName = user.name,
                        onSendMessage = { messageText ->
                            // Pass contractor ID as the "other party email" parameter
                            // The ViewModel will handle setting landlordEmail = currentUser.email
                            viewModel.sendContractorLandlordMessage(ticketId, contractorId, messageText)
                        },
                        onBack = { navController.popBackStack() }
                    )
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
            
            composable(Screen.TenantLandlord.route) {
                // Refresh connections when entering this screen
                LaunchedEffect(Unit) {
                    viewModel.refreshConnections()
                }
                
                val pendingConnections = connections.filter { 
                    it.status == ConnectionStatus.PENDING && it.tenantEmail == currentUser?.email 
                }
                val connectedLandlord = connections.find { 
                    it.status == ConnectionStatus.CONNECTED && it.tenantEmail == currentUser?.email 
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
                        viewModel.sendDirectMessage("", messageText)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.LandlordTenants.route) {
                // Refresh connections when entering this screen
                LaunchedEffect(Unit) {
                    viewModel.refreshConnections()
                }
                
                LaunchedEffect(selectedTenantEmail) {
                    selectedTenantEmail?.let { tenantEmail ->
                        viewModel.startObservingDirectMessages(tenantEmail)
                    } ?: run {
                        viewModel.clearDirectMessages()
                    }
                }
                
                LandlordTenantsScreen(
                    connections = connections,
                    selectedTenantEmail = selectedTenantEmail,
                    messages = directMessages,
                    currentUserEmail = currentUser?.email ?: "",
                    currentUserName = currentUser?.name ?: "",
                    onAddTenant = { tenantEmail ->
                        viewModel.requestTenantConnection(tenantEmail)
                        // Refresh connections after requesting
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(500) // Small delay to allow Firebase to save
                            viewModel.refreshConnections()
                        }
                    },
                    onSelectTenant = { tenantEmail ->
                        selectedTenantEmail = tenantEmail
                    },
                    onSendMessage = { tenantEmail, messageText ->
                        viewModel.sendDirectMessage(tenantEmail, messageText)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
