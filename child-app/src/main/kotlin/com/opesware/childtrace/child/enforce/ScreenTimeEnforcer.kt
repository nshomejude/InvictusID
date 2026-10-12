package com.opesware.childtrace.child.enforce

import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import java.util.Calendar

/**
 * Enforces the parent-set daily screen-time limit, visibly. It reads today's
 * foreground usage from [UsageStatsManager] and, when the limit is exceeded and
 * device-admin is active, locks the screen via [DevicePolicyManager.lockNow].
 *
 * Locking is overt (the child sees the lock) and the child can still see and
 * uninstall ChildTrace — this is enforcement of a disclosed rule, not covert
 * control. Usage access and device-admin are both granted through visible
 * system prompts.
 */
class ScreenTimeEnforcer(private val context: Context) {

    private val admin = ComponentName(context, ChildDeviceAdminReceiver::class.java)

    /** Total foreground time used so far today, in minutes. */
    fun usedMinutesToday(): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now) ?: return 0
        val totalMs = stats.sumOf { it.totalTimeInForeground }
        return totalMs / 60_000
    }

    /**
     * Enforces [dailyLimitMinutes]: locks the screen if today's usage is over
     * the limit and admin is active. Returns true if it locked.
     */
    fun enforce(dailyLimitMinutes: Int?): Boolean {
        dailyLimitMinutes ?: return false
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        if (!dpm.isAdminActive(admin)) return false
        if (usedMinutesToday() < dailyLimitMinutes) return false
        dpm.lockNow()
        return true
    }
}
