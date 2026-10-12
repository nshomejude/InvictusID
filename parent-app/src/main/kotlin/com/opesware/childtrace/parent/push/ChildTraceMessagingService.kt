package com.opesware.childtrace.parent.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.opesware.childtrace.parent.ParentApplication
import com.opesware.childtrace.parent.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the real-time critical-alert push. The payload carries no content —
 * only that a critical flag arrived for a child — so this shows a prompt to
 * open the app, where the detail is fetched and decrypted locally.
 *
 * Requires google-services.json + the google-services Gradle plugin to be
 * active in a real build; the class compiles without them and is inert until
 * Firebase is configured.
 */
class ChildTraceMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val app = application as? ParentApplication ?: return
        val childId = app.pairedChildId ?: return
        scope.launch {
            runCatching { app.client.registerPushToken(parentId = childId, childId = childId, token = token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val channelId = "childtrace_critical"
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(channelId) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(channelId, "Critical alerts", NotificationManager.IMPORTANCE_HIGH),
            )
        }
        val note = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("ChildTrace: urgent")
            .setContentText("A critical alert needs your attention. Open to review.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        mgr.notify(2001, note)
    }
}
