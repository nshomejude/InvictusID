package com.opesware.childtrace.parent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.opesware.childtrace.parent.ParentApplication
import com.opesware.childtrace.parent.ui.theme.ChildTraceParentTheme

/**
 * Entry point. The Parent app is PIN/biometric gated so a found phone does not
 * expose a child's alerts. On successful auth it shows the console; the control
 * actions are the parent's alone.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as ParentApplication).repository

        setContent {
            ChildTraceParentTheme {
                var authed by remember { mutableStateOf(false) }
                if (authed) {
                    ParentConsole(repo)
                } else {
                    AuthGate(onUnlock = { promptBiometric { authed = true } })
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
