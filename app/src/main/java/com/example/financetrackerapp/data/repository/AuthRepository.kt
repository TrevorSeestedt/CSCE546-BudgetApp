package com.example.financetrackerapp.data.repository

import com.example.financetrackerapp.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun registerUser(username: String, email: String, password: String): Result<User>
    suspend fun loginUser(email: String, password: String): Result<User>
    suspend fun logoutUser()
    suspend fun getCurrentUser(): User?
    fun isUserLoggedIn(): Flow<Boolean>
} 