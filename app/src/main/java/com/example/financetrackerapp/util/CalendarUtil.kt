package com.example.financetrackerapp.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CalendarUtil {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
    
    /**
     * Format date to "MMM dd, yyyy" format
     */
    fun formatDate(date: Long): String {
        return dateFormat.format(Date(date))
    }
    
    /**
     * Format date to month and year only
     */
    fun formatMonthYear(date: Long): String {
        return monthYearFormat.format(Date(date))
    }
    
    /**
     * Format date to day of month only (numeric)
     */
    fun formatDayOfMonth(date: Long): String {
        return dayOfMonthFormat.format(Date(date))
    }
    
    /**
     * Format date to day of week (abbreviated)
     */
    fun formatDayOfWeek(date: Long): String {
        return dayOfWeekFormat.format(Date(date))
    }
    
    /**
     * Generate dates for a month view calendar
     */
    fun getDatesForMonthView(selectedDate: Long): List<Long> {
        val dates = mutableListOf<Long>()
        
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        
        // Set to the first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        
        // Determine the day of week for the first day of month
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Go back to the beginning of the week
        calendar.add(Calendar.DAY_OF_MONTH, -(firstDayOfMonth - 1))
        
        // Generate 42 days (6 weeks)
        repeat(42) {
            dates.add(calendar.timeInMillis)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return dates
    }
    
    /**
     * Check if two dates are on the same day
     */
    fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
    
    /**
     * Check if the given date is in the current month
     */
    fun isInCurrentMonth(date: Long, currentMonth: Long): Boolean {
        val dateCal = Calendar.getInstance().apply { timeInMillis = date }
        val currentCal = Calendar.getInstance().apply { timeInMillis = currentMonth }
        
        return dateCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                dateCal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)
    }
    
    /**
     * Get the first day of the month
     */
    fun getFirstDayOfMonth(date: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get the last day of the month
     */
    fun getLastDayOfMonth(date: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, lastDay)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * Move to the previous month
     */
    fun getPreviousMonth(date: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        calendar.add(Calendar.MONTH, -1)
        return calendar.timeInMillis
    }
    
    /**
     * Move to the next month
     */
    fun getNextMonth(date: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        calendar.add(Calendar.MONTH, 1)
        return calendar.timeInMillis
    }
} 