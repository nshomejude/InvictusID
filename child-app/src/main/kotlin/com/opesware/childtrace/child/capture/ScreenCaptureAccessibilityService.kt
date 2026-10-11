package com.opesware.childtrace.child.capture

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.opesware.childtrace.child.ChildApplication
import com.opesware.childtrace.shared.model.CaptureSource
import com.opesware.childtrace.shared.model.CapturedMessage

/**
 * Gap-filler feed. Reads on-screen text as displayed, covering what the
 * notification feed misses (long messages, in-app reading with previews off).
 *
 * More powerful and more fragile: it needs per-app parsing that is maintained
 * as WhatsApp/Instagram/Telegram/TikTok change their layouts. This scaffold
 * collects visible text nodes from the monitored packages; the per-app parsers
 * that turn raw nodes into clean (sender, message) pairs are a follow-up.
 */
class ScreenCaptureAccessibilityService : AccessibilityService() {

    private val sink: CaptureSink get() = (application as ChildApplication).captureSink

    /** Packages whose on-screen content we parse. Tuned per release. */
    private val monitoredPackages = setOf(
        "com.whatsapp",
        "com.instagram.android",
        "org.telegram.messenger",
        "com.snapchat.android",
        "com.zhiliaoapp.musically", // TikTok
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in monitoredPackages) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) return

        val root = rootInActiveWindow ?: return
        val text = collectVisibleText(root).trim()
        if (text.isEmpty()) return

        sink.submit(
            CapturedMessage(
                capturedAtEpochMillis = System.currentTimeMillis(),
                sourceApp = pkg,
                source = CaptureSource.ACCESSIBILITY,
                text = text,
            )
        )
    }

    override fun onInterrupt() { /* no-op */ }

    private fun collectVisibleText(node: AccessibilityNodeInfo?, depth: Int = 0): String {
        node ?: return ""
        if (depth > 40) return "" // guard against pathological trees
        val sb = StringBuilder()
        node.text?.let { if (it.isNotBlank()) sb.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            sb.append(collectVisibleText(node.getChild(i), depth + 1))
        }
        return sb.toString()
    }
}
