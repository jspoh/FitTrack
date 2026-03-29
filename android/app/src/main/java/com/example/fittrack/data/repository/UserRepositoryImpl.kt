package com.example.fittrack.data.repository

import kotlinx.coroutines.flow.first
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.fittrack.core.constants.ApiConstants
import com.example.fittrack.data.remote.api.UserApiService
import com.example.fittrack.data.remote.dto.CreateUserPayload
import com.example.fittrack.data.remote.dto.LoginPayload
import com.example.fittrack.data.remote.dto.UpdateUserPayload
import com.example.fittrack.domain.model.User
import com.example.fittrack.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService,
    private val dataStore: DataStore<Preferences>
) : UserRepository {

    private val tokenKey = stringPreferencesKey(ApiConstants.PREF_TOKEN_KEY)

    override suspend fun login(username: String, password: String): User {
        val response = userApiService.login(LoginPayload(username, password))
        dataStore.edit { it[tokenKey] = response.accessToken }
        return User(response.user.id, response.user.username, response.user.email)
    }

    override suspend fun register(username: String, email: String, password: String): User {
        val response = userApiService.createUser(CreateUserPayload(username, email, password))
        return User(response.id, response.username, response.email)
    }

    override suspend fun isLoggedIn(): Boolean {
        val token = dataStore.data.first()[tokenKey]
        return !token.isNullOrEmpty()
    }

    override suspend fun getMe(): User {
        val response = userApiService.getMe()
        return User(response.id, response.username, response.email)
    }

    override suspend fun updateUser(
        username: String?, email: String?, password: String?, stepTarget: Int?
    ): User {
        val response = userApiService.updateUser(UpdateUserPayload(username, email, password, stepTarget))
        return User(response.id, response.username, response.email)
    }

    override suspend fun logout() {
        dataStore.edit { it.remove(tokenKey) }
    }
}
