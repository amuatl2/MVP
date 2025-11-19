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
}

