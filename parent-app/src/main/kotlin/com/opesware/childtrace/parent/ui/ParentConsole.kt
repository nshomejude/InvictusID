package com.opesware.childtrace.parent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opesware.childtrace.parent.data.ParentRepository
import com.opesware.childtrace.parent.ui.theme.severityColor
import com.opesware.childtrace.shared.model.Alert

@Composable
fun AuthGate(onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text("ChildTrace", fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            Text("Locked — verify to continue")
            Button(onClick = onUnlock) { Text("Unlock") }
        }
    }
}

@Composable
fun ParentConsole(repo: ParentRepository) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = { ConsoleTopBar(repo.childName()) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Notifications, null) }, label = { Text("Alerts") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Menu, null) }, label = { Text("Apps") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Controls") })
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> AlertFeedScreen(repo)
                1 -> ActivityByAppScreen(repo)
                else -> ControlsScreen(repo)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleTopBar(childName: String) {
    TopAppBar(title = {
        Column {
            Text("ChildTrace", fontWeight = FontWeight.SemiBold)
            Text(childName, fontSize = 12.sp)
        }
    })
}

@Composable
private fun AlertFeedScreen(repo: ParentRepository) {
    val alerts = repo.alerts().sortedByDescending { it.severity.rank }
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(alerts) { AlertCard(it) }
    }
}

@Composable
private fun AlertCard(alert: Alert) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SeverityChip(alert.severity.name, severityColor(alert.severity.rank))
                Text(alert.status.name, fontSize = 11.sp)
            }
            Text(alert.category.label, fontWeight = FontWeight.SemiBold)
            Text(alert.contextSnippet, fontSize = 13.sp)
            Text(alert.sourceApp, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SeverityChip(label: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) { Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ActivityByAppScreen(repo: ParentRepository) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors()) {
                Text(
                    "Flagged items only. ChildTrace shows where danger appeared — not the conversations themselves.",
                    Modifier.padding(12.dp), fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        items(repo.activityByApp()) { app ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(app.appLabel, fontWeight = FontWeight.SemiBold)
                        Text(app.lastActivity, fontSize = 11.sp)
                    }
                    val c = app.topSeverity?.let { severityColor(it.rank) } ?: Color(0xFF0E7A68)
                    SeverityChip(app.flaggedCount.toString(), c)
                }
            }
        }
    }
}

@Composable
private fun ControlsScreen(repo: ParentRepository) {
    val policy = repo.childPolicy()
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Policy band · set by age", fontSize = 11.sp)
                Text("${policy.band.name} — ages ${policy.band.minAge}–${policy.band.maxAge}", fontWeight = FontWeight.SemiBold)
                Text("Sensitivity and surfaced-severity follow the band. Age is bound to the consent record.", fontSize = 12.sp)
            }
        }
        ControlToggle("Screen-time limit", policy.dailyScreenTimeMinutes?.let { "$it min/day" } ?: "Off", policy.dailyScreenTimeMinutes != null)
        ControlToggle("Location sharing", "Visible to child", policy.locationSharingEnabled)
        ControlToggle("App-install approval", "Ask before installs", policy.requireInstallApproval)
        ControlToggle("Push on critical", "Immediate alert", true)
    }
}

@Composable
private fun ControlToggle(title: String, subtitle: String, checked: Boolean) {
    var state by remember { androidx.compose.runtime.mutableStateOf(checked) }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp)
            }
            Switch(checked = state, onCheckedChange = { state = it })
        }
    }
}
