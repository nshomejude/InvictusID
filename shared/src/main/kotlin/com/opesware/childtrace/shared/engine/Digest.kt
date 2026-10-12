package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity
import kotlinx.serialization.Serializable

/**
 * A calm weekly summary so a parent isn't only contacted on alarms. Pure
 * aggregation over a window of alerts — no new data collected.
 */
@Serializable
data class WeeklyDigest(
    val childId: String,
    val windowStartEpochMillis: Long,
    val windowEndEpochMillis: Long,
    val totalFlags: Int,
    val bySeverity: Map<String, Int>,
    val byCategory: Map<String, Int>,
    val mostActiveApp: String?,
    /** A one-line, non-alarming headline for the digest card. */
    val headline: String,
)

object DigestBuilder {
    fun build(childId: String, alerts: List<Alert>, windowStart: Long, windowEnd: Long): WeeklyDigest {
        val inWindow = alerts.filter { it.createdAtEpochMillis in windowStart..windowEnd }
        val bySeverity = Severity.entries.associate { sev ->
            sev.name to inWindow.count { it.severity == sev }
        }.filterValues { it > 0 }
        val byCategory = RiskCategory.entries.associate { cat ->
            cat.name to inWindow.count { it.category == cat }
        }.filterValues { it > 0 }
        val mostActiveApp = inWindow.groupingBy { it.sourceApp }.eachCount().maxByOrNull { it.value }?.key
        val critical = inWindow.count { it.severity == Severity.CRITICAL }

        val headline = when {
            inWindow.isEmpty() -> "A quiet week — nothing flagged."
            critical > 0 -> "$critical urgent item${if (critical == 1) "" else "s"} this week — review when you can."
            else -> "${inWindow.size} item${if (inWindow.size == 1) "" else "s"} flagged this week, none critical."
        }

        return WeeklyDigest(
            childId = childId,
            windowStartEpochMillis = windowStart,
            windowEndEpochMillis = windowEnd,
            totalFlags = inWindow.size,
            bySeverity = bySeverity,
            byCategory = byCategory,
            mostActiveApp = mostActiveApp,
            headline = headline,
        )
    }
}
