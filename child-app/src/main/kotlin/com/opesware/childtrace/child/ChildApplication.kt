package com.opesware.childtrace.child

import android.app.Application
import androidx.room.Room
import com.opesware.childtrace.child.capture.CaptureSink
import com.opesware.childtrace.child.data.ChildDatabase
import com.opesware.childtrace.child.data.RoomActivityStore
import com.opesware.childtrace.child.engine.OnDeviceAnalyzer
import com.opesware.childtrace.shared.model.AgeBand
import com.opesware.childtrace.shared.model.Policy

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

    override fun onCreate() {
        super.onCreate()

        // TODO(pairing): load childId + provisioned Policy from encrypted prefs.
        val childId = "unpaired"
        val policy = Policy.defaultFor(AgeBand.FLAG_HIGH)

        val db = Room.databaseBuilder(this, ChildDatabase::class.java, "childtrace.db")
            // TODO(security): open with an Android Keystore-derived passphrase (SQLCipher).
            .build()

        captureSink = OnDeviceAnalyzer(
            childId = childId,
            policyProvider = { policy },
            store = RoomActivityStore(db.activityDao()),
        )
    }
}
