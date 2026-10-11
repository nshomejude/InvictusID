package com.opesware.childtrace.child.pairing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opesware.childtrace.child.disclosure.DisclosureActivity
import com.opesware.childtrace.child.sync.BackendClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Child-side pairing, shown openly at setup (invariant 1 — nothing hidden). The
 * agent or parent enters the code the parent already claimed. The device can
 * only pair after the parent's claim, so capture never starts without a key to
 * seal to and a parent on the other end.
 */
class PairingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    PairingScreen(
                        baseUrl = (applicationContext as? HasBackendBaseUrl)?.backendBaseUrl
                            ?: "https://api.childtrace.example",
                        onPaired = { childId ->
                            savePairedChild(this, childId)
                            startActivity(Intent(this, DisclosureActivity::class.java))
                            finish()
                        },
                    )
                }
            }
        }
    }
}

/** App may expose its configured backend URL. */
interface HasBackendBaseUrl { val backendBaseUrl: String }

fun savePairedChild(context: Context, childId: String) {
    context.getSharedPreferences("childtrace.child", Context.MODE_PRIVATE)
        .edit().putString("child_id", childId).apply()
}

fun pairedChild(context: Context): String? =
    context.getSharedPreferences("childtrace.child", Context.MODE_PRIVATE).getString("child_id", null)

@Composable
private fun PairingScreen(baseUrl: String, onPaired: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Set up ChildTrace", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Text(
            "Enter the pairing code from your parent or the ChildTrace agent. " +
                "ChildTrace will stay visible on this phone and show what it does.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().take(8); error = null },
            label = { Text("Pairing code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        Button(
            onClick = {
                busy = true; error = null
                scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) { BackendClient.pairDevice(baseUrl, code, "0.1.0") }
                    }
                    busy = false
                    result.onSuccess { onPaired(it.childId) }
                        .onFailure { error = "That code didn't work, or setup isn't finished on the parent's phone yet." }
                }
            },
            enabled = code.length == 8 && !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) CircularProgressIndicator(Modifier.padding(4.dp)) else Text("Pair this phone")
        }
    }
}
