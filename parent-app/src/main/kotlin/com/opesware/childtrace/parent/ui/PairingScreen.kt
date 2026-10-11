package com.opesware.childtrace.parent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opesware.childtrace.parent.data.BackendParentClient
import com.opesware.childtrace.parent.data.ParentKeystore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Parent-side pairing: the parent types the code the agent gives them. Claiming
 * registers this device's key with the backend (so the child can seal to it)
 * and resolves the child's policy band. On success [onPaired] carries the child
 * id back to the host so it can open the console.
 */
@Composable
fun PairingScreen(
    client: BackendParentClient,
    keystore: ParentKeystore,
    onPaired: (childId: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text("Pair with your child's phone", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Text(
            "Enter the code from the ChildTrace agent. This links your child's device to your account and sets up end-to-end encryption, so only you can read what's synced.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().take(8); error = null },
            label = { Text("Pairing code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        Button(
            onClick = {
                busy = true; error = null
                scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            client.claimPairing(code, keystore.currentKeyId(), keystore.currentPublicKeySpki())
                        }
                    }
                    busy = false
                    result.onSuccess { onPaired(it.childId) }
                        .onFailure { error = "That code didn't work. Check it with the agent and try again." }
                }
            },
            enabled = code.length == 8 && !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) CircularProgressIndicator(Modifier.padding(4.dp)) else Text("Pair")
        }
    }
}
