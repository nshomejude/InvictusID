package com.opesware.childtrace.child.disclosure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The always-visible home screen — the launcher entry the child can open any
 * time. It is honest disclosure: it states that the device is monitored, who
 * set it up, and what the parent can see. This screen is what keeps the product
 * a visible parental-control tool rather than covert surveillance (invariant 1).
 *
 * The copy is deliberately truthful about the comprehensive scope — it does not
 * claim "we don't read your chats", because under the current model a parent of
 * a minor can review activity. Honesty here is a compliance requirement, not a
 * nicety.
 */
class DisclosureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DisclosureScreen()
                }
            }
        }
    }
}

@Composable
private fun DisclosureScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("ChildTrace is on", style = MaterialTheme.typography.headlineSmall)
        Text(
            "A parent or guardian who looks after you set this up to help keep you safe online. " +
                "It is always here on your phone — it is never hidden.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text("What your grown-up can see", style = MaterialTheme.typography.titleMedium)
        Text(
            "• Messages and activity in your apps\n" +
                "• Websites you visit and apps you use\n" +
                "• Where your phone is\n" +
                "Anything that looks unsafe is shown to them first.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "You can open this screen any time to see that ChildTrace is running and what it does. " +
                "If you have questions, talk to the grown-up who set it up.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
