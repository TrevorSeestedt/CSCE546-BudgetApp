package com.example.financetrackerapp.di

import com.example.financetrackerapp.data.repository.AuthRepository
import com.example.financetrackerapp.data.repository.BudgetRepository
import com.example.financetrackerapp.data.repository.InMemoryAuthRepository
import com.example.financetrackerapp.data.repository.InMemoryBudgetRepository

object AppModule {
    // Singleton instances
    private val authRepository: AuthRepository by lazy { InMemoryAuthRepository() }
    private val budgetRepository: BudgetRepository by lazy { InMemoryBudgetRepository() }
    
    fun provideAuthRepository(): AuthRepository = authRepository
    
    fun provideBudgetRepository(): BudgetRepository = budgetRepository
} 