package com.example.financetrackerapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetrackerapp.data.model.Budget
import com.example.financetrackerapp.data.model.BudgetPeriod
import com.example.financetrackerapp.data.model.Expense
import com.example.financetrackerapp.data.repository.BudgetRepository
import com.example.financetrackerapp.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date

class BudgetDetailViewModel : ViewModel() {
    private val budgetRepository: BudgetRepository = AppModule.provideBudgetRepository()
    
    private val _uiState = MutableStateFlow<BudgetDetailUiState>(BudgetDetailUiState.Loading)
    val uiState: StateFlow<BudgetDetailUiState> = _uiState.asStateFlow()
    
    // navigate back after deleting a budget
    private val _budgetDeleted = MutableStateFlow(false)
    val budgetDeleted: StateFlow<Boolean> = _budgetDeleted.asStateFlow()
    
    init {
        // reset the delete state on initialization
        resetDeleteState()
    }
    
    fun loadBudgetDetails(budgetId: String) {
        viewModelScope.launch {
            _uiState.value = BudgetDetailUiState.Loading
            try {
                val budget = budgetRepository.getBudgetById(budgetId)
                if (budget == null) {
                    _uiState.value = BudgetDetailUiState.Error("Budget not found")
                    return@launch
                }
                
                val expensesFlow = budgetRepository.getExpensesForBudget(budgetId)
                val totalExpenseFlow = budgetRepository.getTotalExpenseForBudget(budgetId)
                
                combine(expensesFlow, totalExpenseFlow) { expenses, totalSpent ->
                    Triple(budget, expenses, totalSpent)
                }.collectLatest { (budget, expenses, totalSpent) ->
                    val percentUsed = if (budget.amount > 0) (totalSpent / budget.amount) * 100 else 0.0
                    val isOverBudget = totalSpent > budget.amount
                    
                    _uiState.value = BudgetDetailUiState.Success(
                        budget = budget,
                        expenses = expenses.sortedByDescending { it.date },
                        totalSpent = totalSpent,
                        percentUsed = percentUsed,
                        isOverBudget = isOverBudget
                    )
                }
            } catch (e: Exception) {
                _uiState.value = BudgetDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun addExpense(budgetId: String, amount: Double, date: Long = System.currentTimeMillis(), description: String = "") {
        viewModelScope.launch {
            try {
                budgetRepository.addExpense(
                    budgetId = budgetId,
                    amount = amount,
                    date = date,
                    description = description
                )
                // UI will update automatically through flow collection
            } catch (e: Exception) {
                _uiState.value = BudgetDetailUiState.Error(e.message ?: "Failed to add expense")
            }
        }
    }
    
    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                val result = budgetRepository.deleteBudget(budgetId)
                result.fold(
                    onSuccess = {
                        _budgetDeleted.value = true
                    },
                    onFailure = {
                        _uiState.value = BudgetDetailUiState.Error("Failed to delete budget")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BudgetDetailUiState.Error(e.message ?: "Failed to delete budget")
            }
        }
    }
    
    fun updateBudget(
        budget: Budget,
        newName: String,
        newAmount: Double,
        newPeriod: BudgetPeriod,
        newStartDate: Long,
        newEndDate: Long?,
        newRepeating: Boolean
    ) {
        viewModelScope.launch {
            try {
                val updatedBudget = budget.copy(
                    name = newName,
                    amount = newAmount,
                    period = newPeriod,
                    startDate = newStartDate,
                    endDate = newEndDate,
                    repeating = newRepeating
                )
                
                val result = budgetRepository.updateBudget(updatedBudget)
                result.fold(
                    onSuccess = {
                        // UI will update automatically through Flow collection
                    },
                    onFailure = {
                        _uiState.value = BudgetDetailUiState.Error("Failed to update budget")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BudgetDetailUiState.Error(e.message ?: "Failed to update budget")
            }
        }
    }
    
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                val result = budgetRepository.deleteExpense(expense.id)
                result.fold(
                    onSuccess = {
                        // UI will update automatically through Flow collection
                    },
                    onFailure = {
                        _uiState.value = BudgetDetailUiState.Error("Failed to delete expense")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BudgetDetailUiState.Error(e.message ?: "Failed to delete expense")
            }
        }
    }
    
    fun updateExpense(expense: Expense, newAmount: Double, newDescription: String) {
        viewModelScope.launch {
            try {
                // create updated expense
                val updatedExpense = expense.copy(
                    amount = newAmount,
                    description = newDescription
                )
                
                // update in repository
                val result = budgetRepository.updateExpense(updatedExpense)
                result.fold(
                    onSuccess = {
                        // UI will update automatically through Flow collection
                    },
                    onFailure = {
                        _uiState.value = BudgetDetailUiState.Error("Failed to update expense")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BudgetDetailUiState.Error(e.message ?: "Failed to update expense")
            }
        }
    }
    
    fun resetDeleteState() {
        _budgetDeleted.value = false
    }
}

sealed class BudgetDetailUiState {
    data object Loading : BudgetDetailUiState()
    data class Success(
        val budget: Budget,
        val expenses: List<Expense>,
        val totalSpent: Double,
        val percentUsed: Double,
        val isOverBudget: Boolean
    ) : BudgetDetailUiState()
    data class Error(val message: String) : BudgetDetailUiState()
} 