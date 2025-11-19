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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val contractors by viewModel.contractors.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val authErrorState by viewModel.authError.collectAsState()

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
                            "create_ticket" -> navController.navigate(Screen.CreateTicket.route)
                            "marketplace" -> navController.navigate(Screen.Marketplace.createRoute(null))
                            "ai_diagnosis" -> navController.navigate(Screen.AIDiagnosis.route)
                            "contractor_dashboard" -> navController.navigate(Screen.ContractorDashboard.route)
                            "schedule" -> navController.navigate(Screen.Schedule.createRoute(null))
                            "history" -> navController.navigate(Screen.History.route)
                            "chat" -> navController.navigate(Screen.Chat.route)
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
                            "create_ticket" -> navController.navigate(Screen.CreateTicket.route)
                            "marketplace" -> navController.navigate(Screen.Marketplace.createRoute(null))
                            "contractor_dashboard" -> navController.navigate(Screen.ContractorDashboard.route)
                            "schedule" -> navController.navigate(Screen.Schedule.createRoute(null))
                            "history" -> navController.navigate(Screen.History.route)
                            "chat" -> navController.navigate(Screen.Chat.route)
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
                    onCreateAccount = { name, email, password, role ->
                        viewModel.createAccount(name, email, password, role)
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
                    UserRole.LANDLORD -> LandlordDashboardScreen(
                        tickets = tickets,
                        onTicketClick = { ticketId ->
                            navController.navigate(Screen.TicketDetail.createRoute(ticketId))
                        },
                        onAIDiagnosis = {
                            navController.navigate(Screen.AIDiagnosis.route)
                        },
                        onMarketplace = {
                            navController.navigate(Screen.Marketplace.route)
                        }
                    )
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
                            aiDiagnosis = "AI Suggestion: $category - Auto-detected",
                            createdAt = com.example.mvp.utils.DateUtils.getCurrentDateTimeString(),
                            createdDate = dateStr,
                            priority = priority,
                            ticketNumber = "${System.currentTimeMillis() % 100000}"
                        )
                        viewModel.addTicket(newTicket)
                    }
                )
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

                if (ticket != null) {
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
                        userRole = currentUser?.role ?: UserRole.TENANT,
                        currentUserEmail = currentUser?.email,
                        currentUserName = currentUser?.name,
                        onAddMessage = { message ->
                            viewModel.addMessageToTicket(ticketId, message)
                        }
                    )
                }
            }

            composable(
                route = Screen.Marketplace.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getString("ticketId")?.takeIf { it != "null" }
                MarketplaceScreen(
                    contractors = contractors,
                    tickets = tickets,
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
                        // For contractors applying to jobs - assign them to the ticket
                        val contractorId = viewModel.getContractorIdForUser(currentUser)
                        if (contractorId != null) {
                            viewModel.assignContractor(jobTicketId, contractorId)
                            navController.popBackStack()
                        }
                    },
                    userRole = currentUser?.role ?: UserRole.TENANT,
                    ticketId = ticketId
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
                    tickets = tickets,
                    onJobClick = { jobId ->
                        navController.navigate(Screen.JobDetail.createRoute(jobId))
                    },
                    onApplyToJob = { ticketId ->
                        // Handle job application - could navigate to ticket detail or apply directly
                        navController.navigate(Screen.TicketDetail.createRoute(ticketId))
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
                    JobDetailScreen(
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
                route = Screen.Schedule.route,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val routeTicketId = backStackEntry.arguments?.getString("ticketId")?.takeIf { it != "null" }
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

            composable(
                route = Screen.Rating.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                val job = jobs.find { it.id == jobId }
                val contractor = job?.let { 
                    contractors.find { c -> c.id == it.contractorId }
                }

                RatingScreen(
                    contractor = contractor,
                    completedJobs = jobs.filter { it.status == "completed" },
                    recentRatings = emptyList(), // TODO: Load from data
                    onBack = { 
                        navController.navigate(Screen.History.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    },
                    onSubmit = { rating, comment ->
                        jobId.let { viewModel.addRating(it, rating) }
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    tickets = tickets,
                    jobs = jobs,
                    contractors = contractors,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Chat.route) {
                ChatScreen(
                    onBack = { navController.popBackStack() }
                )
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
        }
    }
}
