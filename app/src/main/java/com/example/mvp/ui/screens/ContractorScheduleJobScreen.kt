package com.example.mvp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mvp.data.Job
import com.example.mvp.utils.DateUtils
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorScheduleJobScreen(
    job: Job,
    allJobs: List<Job>, // All jobs to check for conflicts
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit // date, time
) {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var currentMonth by remember { mutableStateOf(Date()) }
    var showError by remember { mutableStateOf<String?>(null) }
    
    // Get scheduled dates for other jobs (to prevent conflicts)
    val scheduledDates = remember(allJobs, job.id) {
        allJobs
            .filter { it.id != job.id && it.scheduledDate != null && it.status != "completed" }
            .mapNotNull { otherJob ->
                otherJob.scheduledDate?.let { dateStr ->
                    try {
                        // Handle both "yyyy-MM-dd" and "yyyy-MM-dd HH:mm" formats
                        val dateOnly = dateStr.split(" ").firstOrNull() ?: dateStr
                        DateUtils.parseDate(dateOnly)
                    } catch (e: Exception) {
                        android.util.Log.e("ContractorScheduleJobScreen", "Error parsing scheduled date: $dateStr", e)
                        null
                    }
                }
            }
            .filterNotNull()
            .toSet()
    }
    
    // Time slots from 9am to 9pm in 12-hour format
    val availableTimeSlots = listOf(
        "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
        "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM",
        "6:00 PM", "7:00 PM", "8:00 PM", "9:00 PM"
    )
    
    // Convert 12-hour format to 24-hour format for storage
    fun convertTo24Hour(time12Hour: String): String {
        return try {
            val parts = time12Hour.split(" ")
            if (parts.size != 2) return time12Hour
            
            val timePart = parts[0]
            val amPm = parts[1].uppercase()
            val (hour, minute) = timePart.split(":").let { 
                it[0].toInt() to (if (it.size > 1) it[1].toInt() else 0)
            }
            
            val hour24 = when {
                amPm == "AM" && hour == 12 -> 0
                amPm == "AM" -> hour
                amPm == "PM" && hour == 12 -> 12
                amPm == "PM" -> hour + 12
                else -> hour
            }
            
            String.format("%02d:%02d", hour24, minute)
        } catch (e: Exception) {
            time12Hour // Return original if conversion fails
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schedule Work",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select a date and time for this job",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Error message
                    showError?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    // Select Date Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Date",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("📅", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose an available date (cannot conflict with other scheduled work)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
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
                            
                            // Calendar Grid - Use a fixed height Box with LazyVerticalGrid
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            ) {
                                val cal = Calendar.getInstance()
                                cal.time = currentMonth
                                val year = cal.get(Calendar.YEAR)
                                val month = cal.get(Calendar.MONTH) + 1
                                val daysInMonth = DateUtils.getDaysInMonth(year, month)
                                val startOffset = DateUtils.getDayOfWeek(year, month, 1)
                                
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(7),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Previous month days (faded)
                                    items(startOffset) {
                                        Spacer(modifier = Modifier.size(40.dp))
                                    }
                                    
                                    // Current month days
                                    items(daysInMonth) { day ->
                                        val date = DateUtils.createDate(year, month, day + 1)
                                        val cal1 = Calendar.getInstance()
                                        cal1.time = date
                                        cal1.set(Calendar.HOUR_OF_DAY, 0)
                                        cal1.set(Calendar.MINUTE, 0)
                                        cal1.set(Calendar.SECOND, 0)
                                        cal1.set(Calendar.MILLISECOND, 0)
                                        val dateNormalized = cal1.time
                                        
                                        val isSelected = selectedDate?.let { 
                                            val cal2 = Calendar.getInstance()
                                            val cal3 = Calendar.getInstance()
                                            cal2.time = it
                                            cal3.time = dateNormalized
                                            cal2.get(Calendar.YEAR) == cal3.get(Calendar.YEAR) &&
                                            cal2.get(Calendar.MONTH) == cal3.get(Calendar.MONTH) &&
                                            cal2.get(Calendar.DAY_OF_MONTH) == cal3.get(Calendar.DAY_OF_MONTH)
                                        } ?: false
                                        
                                        val isPast = dateNormalized.before(today)
                                        val isConflict = scheduledDates.any { scheduledDate ->
                                            val cal4 = Calendar.getInstance()
                                            val cal5 = Calendar.getInstance()
                                            cal4.time = scheduledDate
                                            cal5.time = dateNormalized
                                            cal4.get(Calendar.YEAR) == cal5.get(Calendar.YEAR) &&
                                            cal4.get(Calendar.MONTH) == cal5.get(Calendar.MONTH) &&
                                            cal4.get(Calendar.DAY_OF_MONTH) == cal5.get(Calendar.DAY_OF_MONTH)
                                        }
                                        val isDisabled = isPast || isConflict
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(4.dp)
                                                .clickable(enabled = !isDisabled) { 
                                                    selectedDate = dateNormalized
                                                    showError = null
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isConflict -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                    isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                                            isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
                    
                    // Select Time Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Time",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("🕐", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Available time slots",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Time slots grid - Use fixed height Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(availableTimeSlots) { timeSlot ->
                                        OutlinedButton(
                                            onClick = { 
                                                selectedTime = timeSlot
                                                showError = null
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = selectedDate != null,
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (selectedTime == timeSlot)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            ),
                                            border = if (selectedTime == timeSlot)
                                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                            else
                                                null
                                        ) {
                                            Surface(
                                                color = if (selectedTime == timeSlot)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surface,
                                                shape = MaterialTheme.shapes.small,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = timeSlot,
                                                    modifier = Modifier.padding(12.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (selectedTime == timeSlot) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedTime == timeSlot)
                                                        MaterialTheme.colorScheme.onPrimary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Confirm Button
                Button(
                    onClick = {
                        if (selectedDate == null || selectedTime == null) {
                            showError = "Please select both a date and time"
                        } else {
                            val dateStr = DateUtils.formatDate(selectedDate!!, "yyyy-MM-dd")
                            // Convert 12-hour format to 24-hour format for storage
                            val time24Hour = convertTo24Hour(selectedTime!!)
                            onConfirm(dateStr, time24Hour)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedDate != null && selectedTime != null
                ) {
                    Text("Confirm Schedule", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
