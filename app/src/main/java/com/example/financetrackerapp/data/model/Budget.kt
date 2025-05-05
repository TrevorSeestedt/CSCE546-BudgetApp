package com.example.financetrackerapp.data.model

import java.util.Calendar
import java.util.UUID

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String = "My Budget",
    val amount: Double,
    val period: BudgetPeriod,
    val startDate: Long,
    val endDate: Long? = null,
    val repeating: Boolean = true,
    val categories: List<BudgetCategory> = emptyList()
) {
    /**
     * calculates the next reset date based on the start date and period
     */
    fun getNextResetDate(fromDate: Long = System.currentTimeMillis()): Long {
        // non-repeating budgets do not reset
        if (!repeating) {
            return Long.MAX_VALUE // No resets for non-repeating budgets
        }
        
        // passed the end date
        if (endDate != null && fromDate >= endDate) {
            return Long.MAX_VALUE // No more resets after end date
        }
        
        // if there's an end date and within the range
        if (endDate != null && fromDate < endDate) {
            val nextCalculatedReset = calculateNextReset(fromDate)
            // if next calculated reset is after the end date, return the end date as the final reset
            return if (nextCalculatedReset > endDate) endDate else nextCalculatedReset
        }
        
        // repeating budget with no end date
        return calculateNextReset(fromDate)
    }
    
    private fun calculateNextReset(fromDate: Long): Long {
        // non-repeating budgets do not calculate resets... as it was returning the max date beforehand
        if (!repeating) {
            return Long.MAX_VALUE
        }
        
        val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
        val fromCal = Calendar.getInstance().apply { timeInMillis = fromDate }
        val resultCal = Calendar.getInstance().apply { timeInMillis = fromDate }
        
        // if budget was just created then the first reset should will be in the future
        val isFirstPeriod = fromDate <= startDate
        
        when (period) {
            BudgetPeriod.WEEKLY -> {
                resultCal.set(Calendar.DAY_OF_WEEK, startCal.get(Calendar.DAY_OF_WEEK))
                
                if (isFirstPeriod) {
                    resultCal.timeInMillis = startDate
                    resultCal.add(Calendar.WEEK_OF_YEAR, 1)
                } else {
                    if (!fromCal.before(resultCal)) {
                        resultCal.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
            }
            BudgetPeriod.MONTHLY -> {
                // set to same day of month as start date
                val startDayOfMonth = startCal.get(Calendar.DAY_OF_MONTH)
                
                if (isFirstPeriod) {
                    resultCal.timeInMillis = startDate
                    resultCal.add(Calendar.MONTH, 1)
                    
                    // case where the day might not exist in the next month
                    val maxDayInNextMonth = resultCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    if (startDayOfMonth > maxDayInNextMonth) {
                        resultCal.set(Calendar.DAY_OF_MONTH, maxDayInNextMonth)
                    }
                } else {
                    // case where start day might not exist in the current month (i.e 31st)
                    val maxDayInCurrentMonth = resultCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val targetDay = minOf(startDayOfMonth, maxDayInCurrentMonth)
                    
                    resultCal.set(Calendar.DAY_OF_MONTH, targetDay)
                    
                    // if the current date is after or equal to this month's reset day, move to next month
                    if (!fromCal.before(resultCal)) {
                        resultCal.add(Calendar.MONTH, 1)
                        
                        // recalculate for the next month (in case it has fewer days)
                        val maxDayInNextMonth = resultCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val nextMonthTargetDay = minOf(startDayOfMonth, maxDayInNextMonth)
                        resultCal.set(Calendar.DAY_OF_MONTH, nextMonthTargetDay)
                    }
                }
            }
        }
        
        // set time to midnight (start of day)
        resultCal.set(Calendar.HOUR_OF_DAY, 0)
        resultCal.set(Calendar.MINUTE, 0)
        resultCal.set(Calendar.SECOND, 0)
        resultCal.set(Calendar.MILLISECOND, 0)
        
        return resultCal.timeInMillis
    }
    
    /**
     * Check if the budget has reset based on the current date
     */
    fun hasReset(currentDate: Long = System.currentTimeMillis()): Boolean {
        if (!repeating) {
            return false
        }
        
        val nextReset = getNextResetDate(currentDate)
        return currentDate >= nextReset
    }
    
    /**
     * Get all reset dates between start and end date
     */
    fun getResetDatesInRange(startRange: Long, endRange: Long): List<Long> {
        val resetDates = mutableListOf<Long>()
        
        if (!repeating) {
            if (startDate >= startRange && startDate <= endRange) {
                resetDates.add(startDate)
            }
            return resetDates
        }
        
        // for repeating budgets calculate all reset dates within the set range
        var currentDate = startDate
        while (currentDate <= endRange) {
            if (currentDate >= startRange) {
                resetDates.add(currentDate)
            }
            
            val nextReset = getNextResetDate(currentDate + 24 * 60 * 60 * 1000) // added one day to avoid infinite loop
            if (nextReset == Long.MAX_VALUE) break 
            currentDate = nextReset
        }
        
        return resetDates
    }
}

enum class BudgetPeriod {
    WEEKLY, MONTHLY
}

data class BudgetCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val color: Int
) 