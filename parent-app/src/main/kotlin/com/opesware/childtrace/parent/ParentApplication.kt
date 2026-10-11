package com.opesware.childtrace.parent

import android.app.Application
import android.content.Context
import com.opesware.childtrace.parent.data.BackendParentClient
import com.opesware.childtrace.parent.data.ParentKeystore

/** Holds app-wide singletons and the small amount of paired-state the parent app keeps. */
class ParentApplication : Application() {

    val backendBaseUrl: String = "https://api.childtrace.example"
    val client: BackendParentClient by lazy { BackendParentClient(backendBaseUrl) }
    val keystore: ParentKeystore by lazy { ParentKeystore(this) }

    private val prefs by lazy { getSharedPreferences("childtrace.parent", Context.MODE_PRIVATE) }

    /** The paired child id, or null if the parent hasn't paired yet. */
    var pairedChildId: String?
        get() = prefs.getString("child_id", null)
        set(value) { prefs.edit().putString("child_id", value).apply() }
}
