package com.opesware.childtrace.child.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.opesware.childtrace.child.ChildApplication
import com.opesware.childtrace.shared.model.MediaKind
import com.opesware.childtrace.shared.model.MediaTrigger
import com.opesware.childtrace.shared.model.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Takes a single screenshot through the platform [MediaProjection] API and hands
 * it to [MediaCapturer] for policy check, sealing, and upload.
 *
 * Visible by design (CLAUDE.md invariant 1), on two counts:
 *  1. MediaProjection requires the user to grant capture through a **system
 *     consent dialog**, and while a projection is held the OS shows its recording
 *     indicator. This service never suppresses it and holds no API to.
 *  2. This runs as a foreground service with a persistent, plainly-worded
 *     notification saying screen capture is part of the monitoring.
 *
 * It captures **one frame per request** and tears the projection down — there is
 * no continuous recording loop. Both an explicit parent request
 * ([MediaTrigger.PARENT_REQUESTED_SCREEN]) and a high/critical alert
 * ([MediaTrigger.FLAGGED_ALERT], carrying the alert id and severity) route here,
 * so flagged evidence is a screenshot of the current screen, never a sweep of
 * stored photos.
 */
class MediaProjectionCaptureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var projection: MediaProjection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        if (intent == null) { stopSelf(); return START_NOT_STICKY }
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = intentResultData(intent)
        val trigger = MediaTrigger.valueOf(intent.getStringExtra(EXTRA_TRIGGER) ?: MediaTrigger.PARENT_REQUESTED_SCREEN.name)
        val alertId = intent.getStringExtra(EXTRA_ALERT_ID)
        val severity = intent.getStringExtra(EXTRA_SEVERITY)?.let { runCatching { Severity.valueOf(it) }.getOrNull() }

        if (resultCode == 0 || resultData == null) { stopSelf(); return START_NOT_STICKY }

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(resultCode, resultData)
        captureOneFrame(trigger, alertId, severity)
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun intentResultData(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        else intent.getParcelableExtra(EXTRA_RESULT_DATA)

    private fun captureOneFrame(trigger: MediaTrigger, alertId: String?, severity: Severity?) {
        val projection = projection ?: run { stopSelf(); return }
        val metrics = DisplayMetrics().also {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(it)
        }
        val reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        val display: VirtualDisplay = projection.createVirtualDisplay(
            "childtrace-screenshot",
            metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null,
        )

        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            val png = try { encodePng(image, metrics.widthPixels, metrics.heightPixels) } finally { image.close() }
            // One frame only: tear everything down so there is no ongoing capture.
            display.release(); reader.close(); projection.stop()

            val app = application as ChildApplication
            scope.launch {
                runCatching {
                    app.mediaCapturer()?.capture(
                        bytes = png,
                        kind = MediaKind.SCREENSHOT,
                        trigger = trigger,
                        mimeType = "image/png",
                        alertId = alertId,
                        linkedAlertSeverity = severity,
                    )
                }
                stopSelf()
            }
        }, null)
    }

    private fun encodePng(image: android.media.Image, width: Int, height: Int): ByteArray {
        val plane = image.planes[0]
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(plane.buffer)
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        return ByteArrayOutputStream().use { out ->
            cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Screen capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID) else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setContentTitle("ChildTrace is capturing the screen")
            .setContentText("A screenshot is being taken and shared with your parent. This is part of the monitoring you were told about.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        projection?.stop()
        projection = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_TRIGGER = "trigger"
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_SEVERITY = "severity"
        private const val NOTIFICATION_ID = 0x5C01
        private const val CHANNEL_ID = "childtrace.screencapture"

        /** Build the intent to start a one-shot capture, given a granted MediaProjection result. */
        fun intent(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            trigger: MediaTrigger,
            alertId: String? = null,
            severity: Severity? = null,
        ): Intent = Intent(context, MediaProjectionCaptureService::class.java).apply {
            putExtra(EXTRA_RESULT_CODE, resultCode)
            putExtra(EXTRA_RESULT_DATA, resultData)
            putExtra(EXTRA_TRIGGER, trigger.name)
            alertId?.let { putExtra(EXTRA_ALERT_ID, it) }
            severity?.let { putExtra(EXTRA_SEVERITY, it.name) }
        }
    }
}
