package com.example.fittrack.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SessionEventBus {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emit(event: SessionEvent) = _events.tryEmit(event)
}

sealed class SessionEvent {
    object Unauthorized : SessionEvent()
}