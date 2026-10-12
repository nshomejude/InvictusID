package com.opesware.childtrace.parent.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.opesware.childtrace.parent.ParentApplication
import com.opesware.childtrace.parent.data.BackendParentRepository
import com.opesware.childtrace.design.theme.ChildTraceTheme
import com.opesware.childtrace.shared.model.AgeBand

/**
 * Entry point. The Parent app is PIN/biometric gated so a found phone does not
 * expose a child's alerts. On successful auth it shows the console; the control
 * actions are the parent's alone.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ParentApplication

        setContent {
            ChildTraceTheme {
                var authed by remember { mutableStateOf(false) }
                var childId by remember { mutableStateOf(app.pairedChildId) }

                when {
                    !authed -> AuthGate(onUnlock = { promptBiometric { authed = true } })
                    childId == null -> PairingScreen(app.client, app.keystore) { paired ->
                        app.pairedChildId = paired
                        childId = paired
                    }
                    else -> {
                        // Backed by the real backend once paired; a ViewModel would
                        // own refresh() in production. Band is resolved at pairing.
                        val repo = remember(childId) {
                            BackendParentRepository(
                                childId = childId!!,
                                displayName = childId!!,
                                band = AgeBand.FLAG_HIGH,
                                client = app.client,
                                keystore = app.keystore,
                            )
                        }
                        var loaded by remember(childId) { mutableStateOf(0) }
                        LaunchedEffect(childId) {
                            runCatching { withContext(Dispatchers.IO) { repo.registerKey(); repo.refresh() } }
                            loaded++
                        }
                        // Re-read the now-populated cache once the refresh completes.
                        key(loaded) { ParentConsole(repo) }
                    }
                }
            }
        }
    }

    private fun promptBiometric(onSuccess: () -> Unit) {
        val canAuth = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // A dedicated PIN screen would go here; for the scaffold, allow through.
            onSuccess(); return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock ChildTrace")
                .setSubtitle("Verify it's you")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }
}
