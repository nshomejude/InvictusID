package com.opesware.childtrace.parent

import android.app.Application
import com.opesware.childtrace.parent.data.MockParentRepository
import com.opesware.childtrace.parent.data.ParentRepository

/** Holds app-wide singletons. Swap [MockParentRepository] for the backend-backed one. */
class ParentApplication : Application() {
    val repository: ParentRepository by lazy { MockParentRepository() }
}
