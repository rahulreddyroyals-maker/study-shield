package com.studyshield.utils

object SessionState {
    data class Session(
        val sessionId: String,
        val childId: String,
        val allowedPackages: Set<String>,
        val endTimeMs: Long
    )

    @Volatile var current: Session? = null
        private set

    fun update(sessionId: String, childId: String, allowedPackages: Set<String>, endTimeMs: Long) {
        current = Session(sessionId, childId, allowedPackages, endTimeMs)
    }

    fun clear() { current = null }
    val isActive: Boolean get() = current != null
}
