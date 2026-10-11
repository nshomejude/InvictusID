package com.opesware.childtrace.child.engine

import com.opesware.childtrace.child.capture.CaptureSink
import com.opesware.childtrace.child.data.ActivityStore
import com.opesware.childtrace.shared.engine.RiskEngine
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.ActivityType
import com.opesware.childtrace.shared.model.CaptureSource
import com.opesware.childtrace.shared.model.CapturedMessage
import com.opesware.childtrace.shared.model.Policy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The on-device sink. Every captured message is scored here on the phone before
 * anything is persisted or synced:
 *
 *  - The [RiskEngine] (from :shared) ranks danger; a flagged result becomes a
 *    high-priority record the parent sees first.
 *  - Under the comprehensive-monitoring model, ordinary activity is also stored
 *    (encrypted) for the parent of a minor to review — but the danger signal is
 *    always computed locally, so the parent is never left to scroll for risk.
 *
 * Classification runs on-device; only the resulting records sync, encrypted.
 */
class OnDeviceAnalyzer(
    private val childId: String,
    private val policyProvider: () -> Policy,
    private val store: ActivityStore,
    private val engine: RiskEngine = RiskEngine(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : CaptureSink {

    override fun submit(message: CapturedMessage) {
        scope.launch {
            val policy = policyProvider()
            val alert = engine.analyze(message, childId, policy)
            store.record(
                ActivityRecord(
                    id = UUID.randomUUID().toString(),
                    childId = childId,
                    type = activityType(message),
                    createdAtEpochMillis = message.capturedAtEpochMillis,
                    sourceApp = message.sourceApp,
                    content = message.text,
                    counterparty = message.sender,
                    riskCategory = alert?.category,
                    severity = alert?.severity,
                )
            )
        }
    }

    private fun activityType(message: CapturedMessage): ActivityType = when (message.source) {
        CaptureSource.NOTIFICATION -> ActivityType.MESSAGE
        CaptureSource.ACCESSIBILITY -> ActivityType.SCREEN_TEXT
    }
}
