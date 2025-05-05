package com.example.financetrackerapp.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetrackerapp.data.model.Budget
import com.example.financetrackerapp.data.model.Expense
import com.example.financetrackerapp.data.repository.AuthRepository
import com.example.financetrackerapp.data.repository.BudgetRepository
import com.example.financetrackerapp.di.AppModule
import com.example.financetrackerapp.util.CalendarUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel : ViewModel() {
    private val budgetRepository: BudgetRepository = AppModule.provideBudgetRepository()
    private val authRepository: AuthRepository = AppModule.provideAuthRepository()
    
    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()
    
    private val _displayMonth = MutableStateFlow(System.currentTimeMillis())
    val displayMonth: StateFlow<Long> = _displayMonth.asStateFlow()
    
    private val _calendarDates = MutableStateFlow<List<Long>>(emptyList())
    val calendarDates: StateFlow<List<Long>> = _calendarDates.asStateFlow()
    
    private val _selectedDayExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val selectedDayExpenses: StateFlow<List<Expense>> = _selectedDayExpenses.asStateFlow()
    
    // Track navigation events
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()
    
    // Track delete confirmation dialogs
    private val _showDeleteDialog = MutableStateFlow<DeleteDialogState>(DeleteDialogState.Hidden)
    val showDeleteDialog: StateFlow<DeleteDialogState> = _showDeleteDialog.asStateFlow()
    
    init {
        updateCalendarDates()
        loadCalendarData()
    }
    
    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
        loadExpensesForSelectedDay()
    }
    
    fun moveToNextMonth() {
        _displayMonth.value = CalendarUtil.getNextMonth(_displayMonth.value)
        updateCalendarDates()
        loadCalendarData()
    }
    
    fun moveToPreviousMonth() {
        _displayMonth.value = CalendarUtil.getPreviousMonth(_displayMonth.value)
        updateCalendarDates()
        loadCalendarData()
    }
    
    private fun updateCalendarDates() {
        _calendarDates.value = CalendarUtil.getDatesForMonthView(_displayMonth.value)
    }
    
    private fun loadCalendarData() {
        viewModelScope.launch {
            _uiState.value = CalendarUiState.Loading
            
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = CalendarUiState.Error("User not logged in")
                    return@launch
                }
                
                val firstDayOfMonth = CalendarUtil.getFirstDayOfMonth(_displayMonth.value)
                val lastDayOfMonth = CalendarUtil.getLastDayOfMonth(_displayMonth.value)
                
                // Get all budgets for the user
                budgetRepository.getBudgetForUser(currentUser.id).collectLatest { budgets ->
                    // If there are no budgets, update UI state with empty data
                    if (budgets.isEmpty()) {
                        _uiState.value = CalendarUiState.Success(
                            budgets = emptyList(),
                            allExpenses = emptyList(),
                            resetDates = emptyList()
                        )
                        return@collectLatest
                    }
                    
                    // Process all reset dates first
                    val allResetDates = mutableListOf<Pair<Budget, Long>>()
                    budgets.forEach { budget ->
                        val resetDates = budget.getResetDatesInRange(firstDayOfMonth, lastDayOfMonth)
                        resetDates.forEach { resetDate ->
                            allResetDates.add(Pair(budget, resetDate))
                        }
                    }
                    
                    // Simple approach for now - just load current state without waiting for all expenses
                    // This ensures the calendar at least displays with budget reset dates
                    _uiState.value = CalendarUiState.Success(
                        budgets = budgets,
                        allExpenses = emptyList(), // Will be updated as expenses load
                        resetDates = allResetDates
                    )
                    
                    // Load expenses in background 
                    loadExpensesForMonth(budgets, firstDayOfMonth, lastDayOfMonth)
                }
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun loadExpensesForMonth(budgets: List<Budget>, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            try {
                val allExpenses = mutableListOf<Expense>()
                
                // Process one budget at a time sequentially to avoid concurrency issues
                for (budget in budgets) {
                    budgetRepository.getExpensesForBudget(budget.id).collectLatest { expenses ->
                        // Filter expenses within date range
                        val filteredExpenses = expenses.filter { it.date in startDate..endDate }
                        allExpenses.addAll(filteredExpenses)
                        
                        // Update UI state with the expenses we have so far
                        if (_uiState.value is CalendarUiState.Success) {
                            val currentState = _uiState.value as CalendarUiState.Success
                            _uiState.value = currentState.copy(
                                allExpenses = allExpenses.toList()
                            )
                        }
                        
                        // We only need to collect once per budget
                        return@collectLatest
                    }
                }
                
                // After processing all budgets, ensure we update selected day expenses
                loadExpensesForSelectedDay()
            } catch (e: Exception) {
                // Don't change UI state on failure - just keep what we have
            }
        }
    }
    
    private fun loadExpensesForSelectedDay() {
        viewModelScope.launch {
            try {
                if (uiState.value is CalendarUiState.Success) {
                    val currentState = uiState.value as CalendarUiState.Success
                    val selectedDayExpenses = currentState.allExpenses.filter { expense ->
                        CalendarUtil.isSameDay(expense.date, _selectedDate.value)
                    }
                    _selectedDayExpenses.value = selectedDayExpenses
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                val result = budgetRepository.deleteBudget(budget.id)
                result.fold(
                    onSuccess = {
                        // Success handled via Flow updates
                    },
                    onFailure = {
                        _uiState.value = CalendarUiState.Error("Failed to delete budget")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Failed to delete budget")
            }
        }
    }
    
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                val result = budgetRepository.deleteExpense(expense.id)
                result.fold(
                    onSuccess = {
                        // Success will be reflected in the Flow updates
                    },
                    onFailure = {
                        _uiState.value = CalendarUiState.Error("Failed to delete expense")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Failed to delete expense")
            }
        }
    }
    
    fun navigateToEditBudget(budget: Budget) {
        _navigationEvent.value = NavigationEvent.NavigateToEditBudget(budget.id)
    }
    
    fun navigateToEditExpense(expense: Expense) {
        _navigationEvent.value = NavigationEvent.NavigateToEditExpense(expense)
    }
    
    fun showDeleteBudgetDialog(budget: Budget) {
        _showDeleteDialog.value = DeleteDialogState.ConfirmBudgetDelete(budget)
    }
    
    fun showDeleteExpenseDialog(expense: Expense) {
        _showDeleteDialog.value = DeleteDialogState.ConfirmExpenseDelete(expense)
    }
    
    fun hideDeleteDialog() {
        _showDeleteDialog.value = DeleteDialogState.Hidden
    }
    
    fun consumeNavigationEvent() {
        _navigationEvent.value = null
    }
}

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class Success(
        val budgets: List<Budget>,
        val allExpenses: List<Expense>,
        val resetDates: List<Pair<Budget, Long>>
    ) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

sealed class NavigationEvent {
    data class NavigateToEditBudget(val budgetId: String) : NavigationEvent()
    data class NavigateToEditExpense(val expense: Expense) : NavigationEvent()
}

sealed class DeleteDialogState {
    data object Hidden : DeleteDialogState()
    data class ConfirmBudgetDelete(val budget: Budget) : DeleteDialogState()
    data class ConfirmExpenseDelete(val expense: Expense) : DeleteDialogState()
} 