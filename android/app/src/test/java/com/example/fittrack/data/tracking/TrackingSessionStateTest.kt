package com.example.fittrack.data.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TrackingSessionStateTest {

    @Test
    fun `fresh auto session stays resumable within timeout`() {
        val now = LocalDateTime.of(2026, 4, 5, 9, 30, 0)
        val session = TrackingSessionState(
            isTracking = true,
            startTime = now.minusMinutes(3),
            source = TrackingSessionSource.AUTO,
            lastStepAt = now.minusSeconds(45),
            recordedSteps = 180
        )

        assertTrue(session.isFreshAutoSession(timeoutMs = 60_000L, now = now))
    }

    @Test
    fun `auto session becomes stale at timeout boundary`() {
        val now = LocalDateTime.of(2026, 4, 5, 9, 30, 0)
        val session = TrackingSessionState(
            isTracking = true,
            startTime = now.minusMinutes(5),
            source = TrackingSessionSource.AUTO,
            lastStepAt = now.minusSeconds(60),
            recordedSteps = 240
        )

        assertFalse(session.isFreshAutoSession(timeoutMs = 60_000L, now = now))
    }

    @Test
    fun `manual sessions are never treated as resumable auto sessions`() {
        val now = LocalDateTime.of(2026, 4, 5, 9, 30, 0)
        val session = TrackingSessionState(
            isTracking = true,
            startTime = now.minusMinutes(2),
            source = TrackingSessionSource.MANUAL,
            lastStepAt = now.minusSeconds(10),
            recordedSteps = 25
        )

        assertFalse(session.isFreshAutoSession(timeoutMs = 60_000L, now = now))
    }
}
