package com.opesware.childtrace.child

import android.app.Application
import androidx.room.Room
import com.opesware.childtrace.child.capture.CaptureSink
import com.opesware.childtrace.child.data.ChildDatabase
import com.opesware.childtrace.child.data.RoomActivityStore
import com.opesware.childtrace.child.engine.OnDeviceAnalyzer
import com.opesware.childtrace.child.security.KeystoreSecurity
import com.opesware.childtrace.child.sync.BackendClient
import com.opesware.childtrace.child.sync.SyncManager
import com.opesware.childtrace.child.sync.SyncWorker
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
class ChildApplication : Application() {

    lateinit var captureSink: CaptureSink
        private set

    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()

        // TODO(pairing): load childId + provisioned Policy from encrypted prefs.
        val childId = "unpaired"
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
        captureSink = OnDeviceAnalyzer(
            childId = childId,
            policyProvider = { policy },
            store = store,
        )

        // Drain captured activity to the backend via WorkManager (survives
        // process death / reboot, network-gated, exponential backoff). The base
        // URL comes from provisioning config in production.
        val backendBaseUrl = "https://api.childtrace.example"
        syncManager = SyncManager(store, BackendClient(backendBaseUrl, childId))
        SyncWorker.schedule(this)
    }
}
