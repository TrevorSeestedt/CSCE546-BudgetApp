package com.example.financetrackerapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financetrackerapp.data.model.User
import com.example.financetrackerapp.data.repository.AuthRepository
import com.example.financetrackerapp.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val authRepository: AuthRepository = AppModule.provideAuthRepository()
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    val isLoggedIn: StateFlow<Boolean> = authRepository.isUserLoggedIn()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = authRepository.registerUser(username, email, password)
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = AuthUiState.Success(user)
                    },
                    onFailure = { error ->
                        _uiState.value = AuthUiState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = authRepository.loginUser(email, password)
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = AuthUiState.Success(user)
                    },
                    onFailure = { error ->
                        _uiState.value = AuthUiState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                authRepository.logoutUser()
                _uiState.value = AuthUiState.Initial
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = AuthUiState.Initial
    }
}

sealed class AuthUiState {
    data object Initial : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
} 