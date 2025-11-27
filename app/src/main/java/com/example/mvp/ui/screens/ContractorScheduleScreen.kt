package com.example.mvp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvp.data.Job
import com.example.mvp.data.Ticket
import com.example.mvp.data.TicketStatus
import com.example.mvp.utils.DateUtils
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorScheduleScreen(
    jobs: List<Job>,
    tickets: List<Ticket>,
    onBack: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(Date()) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    
    // Get jobs with scheduled dates (from job.scheduledDate or ticket.scheduledDate)
    // Exclude completed jobs - they should not appear on the calendar
    val scheduledJobs = jobs.filter { job ->
        // Only include jobs that are not completed
        job.status != "completed" && 
        ((job.scheduledDate != null && job.scheduledTime != null) || 
        (tickets.find { it.id == job.ticketId }?.scheduledDate != null && 
         tickets.find { it.id == job.ticketId }?.status == TicketStatus.SCHEDULED))
    }
    
    // Group jobs by date
    val jobsByDate = scheduledJobs.groupBy { job ->
        // Prefer job.scheduledDate, fallback to ticket.scheduledDate
        val dateStr = job.scheduledDate ?: tickets.find { it.id == job.ticketId }?.scheduledDate
        dateStr?.let {
            try {
                // scheduledDate format is "yyyy-MM-dd" or "yyyy-MM-dd HH:mm", extract just the date part
                val dateOnly = it.split(" ").firstOrNull() ?: it
                DateUtils.parseDate(dateOnly)
            } catch (e: Exception) {
                null
            }
        }
    }.filterKeys { it != null }.mapKeys { it.key!! }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "My Schedule",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "View your scheduled jobs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Calendar Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Calendar Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                val cal = Calendar.getInstance()
                                cal.time = currentMonth
                                cal.add(Calendar.MONTH, -1)
                                currentMonth = cal.time
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                            }
                            Text(
                                text = remember(currentMonth) {
                                    val cal = Calendar.getInstance()
                                    cal.time = currentMonth
                                    DateUtils.formatMonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { 
                                val cal = Calendar.getInstance()
                                cal.time = currentMonth
                                cal.add(Calendar.MONTH, 1)
                                currentMonth = cal.time
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Days of Week Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid
                        val cal = Calendar.getInstance()
                        cal.time = currentMonth
                        val year = cal.get(Calendar.YEAR)
                        val month = cal.get(Calendar.MONTH) + 1
                        val daysInMonth = DateUtils.getDaysInMonth(year, month)
                        val startOffset = DateUtils.getDayOfWeek(year, month, 1) // Sunday = 0
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.height(280.dp)
                        ) {
                            // Previous month days (faded)
                            items(startOffset) {
                                Spacer(modifier = Modifier.size(40.dp))
                            }
                            
                            // Current month days
                            items(daysInMonth) { day ->
                                val date = DateUtils.createDate(year, month, day + 1)
                                val hasJobs = jobsByDate.containsKey(date)
                                val isSelected = selectedDate?.let { 
                                    val cal1 = Calendar.getInstance()
                                    val cal2 = Calendar.getInstance()
                                    cal1.time = it
                                    cal2.time = date
                                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                                    cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                                    cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
                                } ?: false
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                        .clickable { selectedDate = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            hasJobs -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${day + 1}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected || hasJobs) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    hasJobs -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Jobs
            selectedDate?.let { date ->
                val dayJobs = jobsByDate[date] ?: emptyList()
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Jobs on ${DateUtils.formatDate(date, "MMM dd, yyyy")}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (dayJobs.isEmpty()) {
                                Text(
                                    text = "No jobs scheduled for this day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            } else {
                                dayJobs.forEach { job ->
                                    val ticket = tickets.find { it.id == job.ticketId }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = ticket?.title ?: job.issueType,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            // Show scheduled date and time
                                            val scheduledDateStr = job.scheduledDate ?: ticket?.scheduledDate
                                            val scheduledTimeStr = job.scheduledTime
                                            if (scheduledDateStr != null) {
                                                val timeDisplay = if (scheduledTimeStr != null) {
                                                    val time12Hour = com.example.mvp.utils.DateUtils.formatTime12Hour(scheduledTimeStr)
                                                    "$scheduledDateStr at $time12Hour"
                                                } else if (scheduledDateStr.contains(" ")) {
                                                    scheduledDateStr // Already includes time
                                                } else {
                                                    scheduledDateStr
                                                }
                                                Text(
                                                    text = "Scheduled: $timeDisplay",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Type: ${job.issueType}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

