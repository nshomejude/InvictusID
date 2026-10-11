package com.opesware.childtrace.child.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.opesware.childtrace.child.ChildApplication
import com.opesware.childtrace.shared.model.CaptureSource
import com.opesware.childtrace.shared.model.CapturedMessage

/**
 * Primary capture feed. Receives notification text system-wide — the most
 * reliable source for DMs from WhatsApp, Instagram, Telegram, TikTok, etc.
 *
 * Limitation (stated honestly in the spec): this sees notification content
 * (sender + preview). It misses messages read in-app with no notification, or
 * when previews are disabled — the AccessibilityService feed fills that gap.
 */
class NotificationCaptureService : NotificationListenerService() {

    private val sink: CaptureSink get() = (application as ChildApplication).captureSink

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == applicationContext.packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: return
        if (text.isBlank()) return

        sink.submit(
            CapturedMessage(
                capturedAtEpochMillis = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
                sourceApp = sbn.packageName,
                source = CaptureSource.NOTIFICATION,
                text = text,
                sender = title,
                // A conversation with no contact name is a reasonable best-effort
                // signal for an unknown/first-contact party; refined later.
                senderIsUnknownContact = title.isNullOrBlank(),
            )
        )
    }
}
