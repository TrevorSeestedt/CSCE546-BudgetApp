package com.example.financetrackerapp.data.repository

import com.example.financetrackerapp.data.model.Budget
import com.example.financetrackerapp.data.model.BudgetPeriod
import com.example.financetrackerapp.data.model.Expense
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    // Budget operations
    suspend fun createBudget(userId: String, name: String, amount: Double, period: BudgetPeriod, startDate: Long, endDate: Long? = null, repeating: Boolean = true): Result<Budget>
    suspend fun getBudgetForUser(userId: String): Flow<List<Budget>>
    suspend fun getBudgetById(budgetId: String): Budget?
    suspend fun updateBudget(budget: Budget): Result<Budget>
    suspend fun deleteBudget(budgetId: String): Result<Boolean>
    
    // Expense operations
    suspend fun addExpense(budgetId: String, amount: Double, date: Long, description: String): Result<Expense>
    suspend fun getExpensesForBudget(budgetId: String): Flow<List<Expense>>
    suspend fun getBudgetWithExpenses(budgetId: String): Flow<Pair<Budget, List<Expense>>>
    suspend fun getTotalExpenseForBudget(budgetId: String): Flow<Double>
    suspend fun updateExpense(expense: Expense): Result<Expense>
    suspend fun deleteExpense(expenseId: String): Result<Boolean>
} 