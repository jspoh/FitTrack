package com.example.fittrack.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.fittrack.core.constants.ApiConstants
import com.example.fittrack.core.utils.SessionEvent
import com.example.fittrack.core.utils.SessionEventBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            dataStore.data.first()[stringPreferencesKey(ApiConstants.PREF_TOKEN_KEY)]
        }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrEmpty()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            SessionEventBus.emit(SessionEvent.Unauthorized)
        }

        return response
    }
}
