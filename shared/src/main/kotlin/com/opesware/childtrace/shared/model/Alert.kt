package com.opesware.childtrace.shared.model

import kotlinx.serialization.Serializable

/**
 * A flagged item — the only thing that ever crosses from the child's device to
 * the backend and on to the parent.
 *
 * An Alert carries the *minimum context* needed for a parent to understand and
 * act, never a transcript of the conversation. The [contextSnippet] is treated
 * as the most sensitive field in the system and is encrypted at the application
 * layer at rest (see the spec's Security section).
 */
@Serializable
data class Alert(
    val id: String,
    val childId: String,
    val category: RiskCategory,
    val severity: Severity,
    val createdAtEpochMillis: Long,
    /** Source app package the flagged text came from. */
    val sourceApp: String,
    /** Minimal surrounding context — never the full conversation. */
    val contextSnippet: String,
    /** Why the engine flagged it (human-readable, drawn from the firing rules). */
    val reason: String,
    val status: AlertStatus = AlertStatus.NEW,
)

@Serializable
enum class AlertStatus { NEW, REVIEWED, DISMISSED }
