package com.opesware.childtrace.child.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.opesware.childtrace.child.R

/**
 * Keeps capture alive against Android's background limits. Its persistent
 * notification is deliberate: it doubles as honest disclosure that ChildTrace
 * is running — part of the always-visible invariant, never dismissible away
 * into stealth.
 */
class CaptureForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.capture_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = getString(R.string.capture_channel_desc) }
            )
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_running_title))
            .setContentText(getString(R.string.capture_running_text))
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "childtrace_capture"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CaptureForegroundService::class.java))
        }
    }
}
