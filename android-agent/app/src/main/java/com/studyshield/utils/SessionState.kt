package com.studyshield.utils

/**
 * Singleton holding the current active session state.
 * Updated in real-time from Firestore via AppMonitorService.
 */
object SessionState {

    data class Session(
        val sessionId: String,
        val childId: String,
        val allowedPackages: Set<String>,
        val endTimeMs: Long
    )

    @Volatile
    var current: Session? = null
        private set

    fun update(
        sessionId: String,
        childId: String,
        allowedPackages: Set<String>,
        endTimeMs: Long
    ) {
        current = Session(sessionId, childId, allowedPackages, endTimeMs)
    }

    fun clear() {
        current = null
    }

    val isActive: Boolean get() = current != null
}
