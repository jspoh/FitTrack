package com.example.fittrack.domain.repository

import com.example.fittrack.domain.model.User

interface UserRepository {
    suspend fun login(username: String, password: String): User
    suspend fun register(username: String, email: String, password: String): User
    suspend fun isLoggedIn(): Boolean
    suspend fun getMe(): User
    suspend fun updateUser(username: String?, email: String?, password: String?, stepTarget: Int?): User
    suspend fun logout()
}
