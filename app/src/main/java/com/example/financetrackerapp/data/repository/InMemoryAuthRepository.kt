package com.example.financetrackerapp.data.repository

import com.example.financetrackerapp.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryAuthRepository : AuthRepository {
    private val users = mutableListOf<User>()
    private var currentUser: User? = null
    private val isLoggedInFlow = MutableStateFlow(false)

    override suspend fun registerUser(username: String, email: String, password: String): Result<User> {
        // check if user exists
        if (users.any { it.email == email }) {
            return Result.failure(Exception("User with this email already exists"))
        }

        val newUser = User(
            username = username,
            email = email,
            password = password
        )
        users.add(newUser)
        currentUser = newUser
        isLoggedInFlow.value = true
        return Result.success(newUser)
    }

    override suspend fun loginUser(email: String, password: String): Result<User> {
        val user = users.find { it.email == email && it.password == password }
            ?: return Result.failure(Exception("Invalid credentials"))
        
        currentUser = user
        isLoggedInFlow.value = true
        return Result.success(user)
    }

    override suspend fun logoutUser() {
        currentUser = null
        isLoggedInFlow.value = false
    }

    override suspend fun getCurrentUser(): User? {
        return currentUser
    }

    override fun isUserLoggedIn(): Flow<Boolean> {
        return isLoggedInFlow.asStateFlow()
    }
} 