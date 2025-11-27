package com.example.mvp.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    fun getCurrentDateString(): String {
        return dateFormat.format(Date())
    }
    
    fun getCurrentDateTimeString(): String {
        return dateTimeFormat.format(Date())
    }
    
    fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = dateFormat.parse(dateString)
            displayDateFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
    
    fun formatMonthYear(year: Int, month: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        return monthYearFormat.format(calendar.time)
    }
    
    fun createDate(year: Int, month: Int, day: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    fun getDayOfWeek(year: Int, month: Int, day: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        return calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
    }
    
    fun addMonths(date: Date, months: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MONTH, months)
        return calendar.time
    }
    
    fun subtractMonths(date: Date, months: Int): Date {
        return addMonths(date, -months)
    }
    
    fun formatDate(date: Date, pattern: String): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
    
    fun parseDate(dateString: String): Date? {
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    // Convert 24-hour format (HH:mm) to 12-hour format (h:mm AM/PM)
    fun formatTime12Hour(time24Hour: String): String {
        return try {
            val parts = time24Hour.split(":")
            if (parts.size < 2) return time24Hour
            
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val amPm = if (hour < 12) "AM" else "PM"
            
            String.format("%d:%02d %s", hour12, minute, amPm)
        } catch (e: Exception) {
            time24Hour // Return original if conversion fails
        }
    }
    
    // Format timestamp as relative time (e.g., "2 m ago", "1 h ago", "3 d ago")
    fun formatRelativeTime(timestamp: String): String {
        return try {
            val date = dateTimeFormat.parse(timestamp)
            if (date != null) {
                val now = Date()
                val diff = now.time - date.time
                val seconds = diff / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val days = hours / 24
                
                when {
                    seconds < 60 -> "Just now"
                    minutes < 60 -> "$minutes m ago"
                    hours < 24 -> "$hours h ago"
                    days < 7 -> "$days d ago"
                    else -> {
                        val displayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                        displayFormat.format(date)
                    }
                }
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}

