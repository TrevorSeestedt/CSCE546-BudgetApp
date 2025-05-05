package com.example.financetrackerapp.data.model

import java.util.UUID

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val budgetId: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val description: String = ""
) 