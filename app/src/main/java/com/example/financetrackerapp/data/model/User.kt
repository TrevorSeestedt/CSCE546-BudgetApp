package com.example.financetrackerapp.data.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val email: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis()
) 