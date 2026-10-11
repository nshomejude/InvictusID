package com.opesware.childtrace.agent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opesware.childtrace.agent.data.AgentClient
import com.opesware.childtrace.design.theme.ChildTraceTheme
import com.opesware.childtrace.shared.api.PairingSessionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Agent surface. After the offline provisioning gate (ID, guardianship proof,
 * own child) and consent recording, the agent opens a pairing session for the
 * child and reads out the code. The parent claims it; then the child device
 * pairs. This app never sees any monitored content.
 */
class MainActivity : ComponentActivity() {

    private val baseUrl = "https://api.childtrace.example"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val client = AgentClient(baseUrl)
        setContent {
            ChildTraceTheme {
                Surface(Modifier.fillMaxSize()) { OpenSessionScreen(client) }
            }
        }
    }
}

@Composable
private fun OpenSessionScreen(client: AgentClient) {
    var childId by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<PairingSessionResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Open a pairing session", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Text(
            "The child's consent must already be recorded. Enter the child id to " +
                "generate a one-time pairing code, then read it to the parent.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = childId,
            onValueChange = { childId = it.trim(); error = null; session = null },
            label = { Text("Child id") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }

        session?.let { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pairing code", fontSize = 12.sp)
                    Text(
                        s.code,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text("Parent claims this first, then the child device pairs.", fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Button(
            onClick = {
                busy = true; error = null; session = null
                scope.launch {
                    val result = runCatching { withContext(Dispatchers.IO) { client.openPairingSession(childId) } }
                    busy = false
                    result.onSuccess { session = it }
                        .onFailure { error = "Could not open a session. Confirm the child's consent is recorded." }
                }
            },
            enabled = childId.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) CircularProgressIndicator(Modifier.padding(4.dp)) else Text("Generate code")
        }
    }
}
