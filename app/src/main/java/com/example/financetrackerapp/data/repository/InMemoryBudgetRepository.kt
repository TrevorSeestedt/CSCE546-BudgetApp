package com.example.financetrackerapp.data.repository

import com.example.financetrackerapp.data.model.Budget
import com.example.financetrackerapp.data.model.BudgetPeriod
import com.example.financetrackerapp.data.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class InMemoryBudgetRepository : BudgetRepository {
    private val budgets = mutableListOf<Budget>()
    private val budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())
    
    private val expenses = mutableListOf<Expense>()
    private val expensesFlow = MutableStateFlow<List<Expense>>(emptyList())

    override suspend fun createBudget(
        userId: String,
        name: String,
        amount: Double,
        period: BudgetPeriod,
        startDate: Long,
        endDate: Long?,
        repeating: Boolean
    ): Result<Budget> {
        val newBudget = Budget(
            userId = userId,
            name = name,
            amount = amount,
            period = period,
            startDate = startDate,
            endDate = endDate,
            repeating = repeating
        )
        budgets.add(newBudget)
        budgetsFlow.value = budgets.toList()
        return Result.success(newBudget)
    }

    override suspend fun getBudgetForUser(userId: String): Flow<List<Budget>> {
        return budgetsFlow.map { budgetList ->
            budgetList.filter { it.userId == userId }
        }
    }

    override suspend fun getBudgetById(budgetId: String): Budget? {
        return budgets.find { it.id == budgetId }
    }

    override suspend fun updateBudget(budget: Budget): Result<Budget> {
        val index = budgets.indexOfFirst { it.id == budget.id }
        if (index == -1) {
            return Result.failure(Exception("Budget not found"))
        }
        
        budgets[index] = budget
        budgetsFlow.value = budgets.toList()
        return Result.success(budget)
    }

    override suspend fun deleteBudget(budgetId: String): Result<Boolean> {
        val removed = budgets.removeIf { it.id == budgetId }
        if (removed) {
            budgetsFlow.value = budgets.toList()
            // delete all expenses associated with this budget
            expenses.removeIf { it.budgetId == budgetId }
            expensesFlow.value = expenses.toList()
            return Result.success(true)
        }
        return Result.failure(Exception("Budget not found"))
    }
    
    // Expense operations
    override suspend fun addExpense(
        budgetId: String,
        amount: Double,
        date: Long,
        description: String
    ): Result<Expense> {
        val budget = getBudgetById(budgetId) ?: return Result.failure(Exception("Budget not found"))
        
        val newExpense = Expense(
            budgetId = budgetId,
            amount = amount,
            date = date,
            description = description
        )
        
        expenses.add(newExpense)
        expensesFlow.value = expenses.toList()
        return Result.success(newExpense)
    }
    
    override suspend fun getExpensesForBudget(budgetId: String): Flow<List<Expense>> {
        return expensesFlow.map { expenseList ->
            expenseList.filter { it.budgetId == budgetId }
        }
    }
    
    override suspend fun getBudgetWithExpenses(budgetId: String): Flow<Pair<Budget, List<Expense>>> {
        val budget = getBudgetById(budgetId) ?: throw Exception("Budget not found")
        
        return getExpensesForBudget(budgetId).map { expenses ->
            Pair(budget, expenses)
        }
    }
    
    override suspend fun getTotalExpenseForBudget(budgetId: String): Flow<Double> {
        return getExpensesForBudget(budgetId).map { expenses ->
            expenses.sumOf { it.amount }
        }
    }
    
    override suspend fun updateExpense(expense: Expense): Result<Expense> {
        val index = expenses.indexOfFirst { it.id == expense.id }
        if (index == -1) {
            return Result.failure(Exception("Expense not found"))
        }
        
        expenses[index] = expense
        expensesFlow.value = expenses.toList()
        return Result.success(expense)
    }
    
    override suspend fun deleteExpense(expenseId: String): Result<Boolean> {
        val removed = expenses.removeIf { it.id == expenseId }
        if (removed) {
            expensesFlow.value = expenses.toList()
            return Result.success(true)
        }
        return Result.failure(Exception("Expense not found"))
    }
} 