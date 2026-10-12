package com.opesware.childtrace.child

import android.app.Application
import androidx.room.Room
import com.opesware.childtrace.child.capture.CaptureSink
import com.opesware.childtrace.child.data.ChildDatabase
import com.opesware.childtrace.child.data.RoomActivityStore
import com.opesware.childtrace.child.engine.OnDeviceAnalyzer
import com.opesware.childtrace.child.engine.OnDeviceClassifier
import com.opesware.childtrace.child.media.MediaCapturer
import com.opesware.childtrace.child.pairing.HasBackendBaseUrl
import com.opesware.childtrace.child.pairing.pairedChild
import com.opesware.childtrace.child.security.KeystoreSecurity
import com.opesware.childtrace.child.sync.BackendClient
import com.opesware.childtrace.child.sync.SyncManager
import com.opesware.childtrace.child.sync.SyncWorker
import com.opesware.childtrace.child.tamper.TamperMonitor
import com.opesware.childtrace.shared.engine.ClassifierRule
import com.opesware.childtrace.shared.engine.MediaPolicy
import com.opesware.childtrace.shared.engine.RiskEngine
import com.opesware.childtrace.shared.engine.rules.DefaultRules
import com.opesware.childtrace.shared.model.AgeBand
import com.opesware.childtrace.shared.model.Policy
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Wires the capture feeds to the on-device analyzer and the encrypted store.
 * The capture services pull [captureSink] from here, so they stay free of any
 * analysis or persistence logic.
 *
 * The provisioned child's age/band comes from secure provisioning state; a
 * default is used here until pairing writes the real policy.
 */
class ChildApplication : Application(), HasBackendBaseUrl {

    override val backendBaseUrl: String = "https://api.childtrace.example"

    lateinit var captureSink: CaptureSink
        private set

    lateinit var syncManager: SyncManager
        private set

    private var mediaCapturer: MediaCapturer? = null

    /**
     * The media gate for screen capture, or null until the device is paired.
     * Explicit screen capture stays disabled unless the deployment enables it
     * (counsel sign-off); flagged-media capture is always policy-gated.
     */
    fun mediaCapturer(): MediaCapturer? = mediaCapturer

    override fun onCreate() {
        super.onCreate()

        // The paired child id is written by PairingActivity; until the device is
        // paired, capture produces no bound records and sync is not scheduled.
        val childId = pairedChild(this)
        // TODO(pairing): load the provisioned Policy (band) alongside the child id.
        val policy = Policy.defaultFor(AgeBand.FLAG_HIGH)

        // Encrypt the local activity DB at rest: SQLCipher opened with a
        // passphrase sealed by the Android Keystore (invariant 3).
        System.loadLibrary("sqlcipher")
        val security = KeystoreSecurity(this)
        val factory = SupportOpenHelperFactory(security.databasePassphrase())
        val db = Room.databaseBuilder(this, ChildDatabase::class.java, "childtrace.db")
            .openHelperFactory(factory)
            .build()

        val store = RoomActivityStore(db.activityDao())
        // Engine = explainable rules + the on-device ML seam (no-op until a model
        // is bundled, so it never adds a false positive today).
        val engine = RiskEngine(DefaultRules.all() + ClassifierRule(OnDeviceClassifier()))
        captureSink = OnDeviceAnalyzer(
            childId = childId ?: "unpaired",
            policyProvider = { policy },
            store = store,
            engine = engine,
        )

        // Only drain to the backend once paired (there is a parent key to seal
        // to). WorkManager handles cadence, reboot survival, and backoff.
        if (childId != null) {
            val client = BackendClient(backendBaseUrl, childId)
            val tamperMonitor = TamperMonitor(this, childId)
            syncManager = SyncManager(store, client, tamperMonitor)
            // Explicit screen capture is off unless the build enables it (counsel
            // sign-off, BuildConfig.ALLOW_SCREEN_CAPTURE). Flagged media is gated
            // by severity regardless.
            mediaCapturer = MediaCapturer(
                childId = childId,
                client = client,
                config = MediaPolicy.Config(allowScreenCapture = BuildConfig.ALLOW_SCREEN_CAPTURE),
            )
            SyncWorker.schedule(this)
        } else {
            syncManager = SyncManager(store, BackendClient(backendBaseUrl, "unpaired"))
        }
    }
}
