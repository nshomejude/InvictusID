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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import com.opesware.childtrace.shared.model.ActivityRecord
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opesware.childtrace.parent.data.ParentRepository
import com.opesware.childtrace.design.theme.Motion
import com.opesware.childtrace.design.theme.criticalPulseAlpha
import com.opesware.childtrace.design.theme.severityColors
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.SafetyResources
import com.opesware.childtrace.shared.model.Severity

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
                    icon = { Icon(Icons.Filled.List, null) }, label = { Text("Activity") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Controls") })
            }
        },
    ) { pad ->
        // Tab content crossfades at motionBase (styling-spec §8).
        Crossfade(targetState = tab, animationSpec = tween(Motion.base), label = "tab") { current ->
            Box(Modifier.padding(pad)) {
                when (current) {
                    0 -> AlertFeedScreen(repo)
                    1 -> ActivityByAppScreen(repo)
                    2 -> ActivityStreamScreen(repo)
                    else -> ControlsScreen(repo)
                }
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
        items(alerts, key = { it.id }) { AlertCard(it, Modifier.animateItem()) }
    }
}

@Composable
private fun AlertCard(alert: Alert, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeverityChip(alert.severity)
                    // The one sanctioned glow: a pulsing dot on a new critical alert.
                    if (alert.severity == Severity.CRITICAL && alert.status.name == "NEW") CriticalPulseDot()
                }
                Text(alert.status.name, fontSize = 11.sp)
            }
            Text(alert.category.label, fontWeight = FontWeight.SemiBold)
            Text(alert.contextSnippet, fontSize = 13.sp)
            Text(alert.sourceApp, fontSize = 11.sp)
            // Escalation resources for serious categories (self-harm, exploitation).
            SafetyResources.forCategory(alert.category).firstOrNull()?.let { res ->
                Card {
                    Column(Modifier.padding(10.dp)) {
                        Text(res.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(res.description, fontSize = 11.sp)
                        Text(res.contact, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/** Severity label on its own paired container (anti-collision, styling-spec §2). */
@Composable
private fun SeverityChip(severity: Severity) {
    val (fg, bg) = severityColors(severity.rank)
    Box(
        Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) { Text(severity.name, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun CriticalPulseDot() {
    val (fg, _) = severityColors(Severity.CRITICAL.rank)
    Box(Modifier.size(9.dp).alpha(criticalPulseAlpha()).background(fg, CircleShape))
}

/** A numeric count on its severity-paired container. */
@Composable
private fun CountChip(count: Int, rank: Int) {
    val (fg, bg) = severityColors(rank)
    Box(
        Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) { Text(count.toString(), color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
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
                    // Count on its severity-paired container (or the clear/safe pair when 0).
                    CountChip(app.flaggedCount, app.topSeverity?.rank ?: Severity.LOW.rank)
                }
            }
        }
    }
}

@Composable
private fun ActivityStreamScreen(repo: ParentRepository) {
    val items = repo.activity()
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Full activity on ${repo.childName()}'s device. Flagged items are marked and sorted first.",
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(4.dp))
        }
        items(items, key = { it.id }) { ActivityCard(it, Modifier.animateItem()) }
    }
}

@Composable
private fun ActivityCard(record: ActivityRecord, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.type.name.replace('_', ' '), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                record.severity?.let { SeverityChip(it) }
            }
            Text(record.content)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.sourceApp ?: record.counterparty ?: "", fontSize = 11.sp)
                Text(record.counterparty?.takeIf { record.sourceApp != null } ?: "", fontSize = 11.sp)
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
