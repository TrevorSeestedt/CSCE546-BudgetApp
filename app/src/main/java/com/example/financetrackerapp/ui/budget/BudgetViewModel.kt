package com.example.financetrackerapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetrackerapp.data.model.Budget
import com.example.financetrackerapp.data.model.BudgetPeriod
import com.example.financetrackerapp.data.repository.AuthRepository
import com.example.financetrackerapp.data.repository.BudgetRepository
import com.example.financetrackerapp.di.AppModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetViewModel : ViewModel() {
    private val budgetRepository: BudgetRepository = AppModule.provideBudgetRepository()
    private val authRepository: AuthRepository = AppModule.provideAuthRepository()
    
    private val _uiState = MutableStateFlow<BudgetUiState>(BudgetUiState.Initial)
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    
    fun createBudget(name: String, amount: Double, period: BudgetPeriod, startDate: Long, endDate: Long? = null, repeating: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = BudgetUiState.Loading
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = BudgetUiState.Error("User not logged in")
                    return@launch
                }
                
                val result = budgetRepository.createBudget(
                    userId = currentUser.id,
                    name = name,
                    amount = amount,
                    period = period,
                    startDate = startDate,
                    endDate = endDate,
                    repeating = repeating
                )
                
                result.fold(
                    onSuccess = { budget ->
                        _uiState.value = BudgetUiState.Success
                        // don't immediately load budgets - wait for navigation to complete
                        // budgets will be loaded when returning to the home screen
                    },
                    onFailure = { error ->
                        _uiState.value = BudgetUiState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun loadUserBudgets() {
        viewModelScope.launch {
            _uiState.value = BudgetUiState.Loading
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = BudgetUiState.Error("User not logged in")
                    return@launch
                }
                
                budgetRepository.getBudgetForUser(currentUser.id).collectLatest { budgets ->
                    _uiState.update { currentState ->
                        if (currentState is BudgetUiState.BudgetList) {
                            currentState.copy(budgets = budgets)
                        } else {
                            BudgetUiState.BudgetList(budgets)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = BudgetUiState.Initial
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
            _uiState.value = BudgetUiState.Loading
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
                        _uiState.value = BudgetUiState.Success
                    },
                    onFailure = { error ->
                        _uiState.value = BudgetUiState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class BudgetUiState {
    data object Initial : BudgetUiState()
    data object Loading : BudgetUiState()
    data object Success : BudgetUiState()
    data class BudgetList(val budgets: List<Budget>) : BudgetUiState()
    data class Error(val message: String) : BudgetUiState()
} 