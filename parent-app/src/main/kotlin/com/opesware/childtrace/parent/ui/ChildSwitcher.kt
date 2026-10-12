package com.opesware.childtrace.parent.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opesware.childtrace.shared.api.ChildSummary

/**
 * Multi-child switcher. Shows the active child and, when the parent account has
 * more than one, a dropdown to switch. Hidden entirely for a single child so it
 * adds no clutter.
 */
@Composable
fun ChildSwitcher(
    children: List<ChildSummary>,
    currentChildId: String,
    onSelect: (String) -> Unit,
) {
    if (children.size < 2) return
    var open by remember { mutableStateOf(false) }
    val current = children.firstOrNull { it.childId == currentChildId }

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextButton(onClick = { open = true }) {
                Text(current?.displayName ?: "Select child")
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch child")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                children.forEach { child ->
                    DropdownMenuItem(
                        text = { Text("${child.displayName} · ${child.band.lowercase()}") },
                        onClick = { open = false; onSelect(child.childId) },
                    )
                }
            }
        }
    }
}
