package com.example.fittrack.domain.usecase.auth

import com.example.fittrack.domain.repository.UserRepository
import javax.inject.Inject

class CheckAuthUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Boolean> =
        runCatching { userRepository.isLoggedIn() }
}