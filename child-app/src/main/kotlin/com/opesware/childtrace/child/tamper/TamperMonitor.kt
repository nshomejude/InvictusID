package com.opesware.childtrace.child.tamper

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.opesware.childtrace.shared.model.TamperEvent
import com.opesware.childtrace.shared.model.TamperKind

/**
 * Checks whether ChildTrace's capture capabilities are still enabled and reports
 * any that have been turned off. This is tamper-*resistance*, never concealment:
 * the app stays visible and uninstallable; it simply tells the parent, openly,
 * when its monitoring has been disabled — which is itself useful safety signal.
 */
class TamperMonitor(private val context: Context, private val childId: String) {

    /** Returns a tamper event for each capability currently disabled. */
    fun scan(nowEpochMillis: Long = System.currentTimeMillis()): List<TamperEvent> = buildList {
        if (!notificationAccessEnabled()) {
            add(TamperEvent(childId, TamperKind.NOTIFICATION_ACCESS_REVOKED, nowEpochMillis))
        }
        if (!accessibilityEnabled()) {
            add(TamperEvent(childId, TamperKind.ACCESSIBILITY_DISABLED, nowEpochMillis))
        }
    }

    private fun notificationAccessEnabled(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    private fun accessibilityEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        if (!am.isEnabled) return false
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.contains(context.packageName)
    }
}
