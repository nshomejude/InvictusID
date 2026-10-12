package com.opesware.childtrace.parent.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opesware.childtrace.parent.data.MediaItemUi
import com.opesware.childtrace.parent.data.ParentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withContext

/**
 * The parent's captured-media gallery. It lists screenshots and images the
 * device captured, and decrypts one **in the app** when the parent opens it —
 * the private key never leaves this phone, and there is no web-admin view of the
 * plaintext (invariant 3). Items sealed to a rotated-out key are shown but can't
 * be opened here, which is the correct security property, not a bug.
 */
@Composable
fun MediaGalleryScreen(repo: ParentRepository) {
    var items by remember { mutableStateOf<List<MediaItemUi>?>(null) }
    LaunchedEffect(Unit) {
        items = runCatching { repo.mediaList() }.getOrDefault(emptyList())
    }

    val list = items
    when {
        list == null -> Column(Modifier.fillMaxSize().padding(24.dp)) { CircularProgressIndicator() }
        list.isEmpty() -> Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("No captured media", style = MaterialTheme.typography.titleMedium)
            Text(
                "Screenshots appear here only when a safety alert captures one, or when you request one. " +
                    "Nothing is swept from the photo gallery.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list, key = { it.id }) { item -> MediaCard(repo, item) }
        }
    }
}

@Composable
private fun MediaCard(repo: ParentRepository, item: MediaItemUi) {
    var bytes by remember(item.id) { mutableStateOf<ByteArray?>(null) }
    var loading by remember(item.id) { mutableStateOf(false) }
    var failed by remember(item.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(Modifier.fillMaxWidth().padding(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.kindLabel, fontWeight = FontWeight.SemiBold)
            Text(item.reason, style = MaterialTheme.typography.bodyMedium)

            val decoded = bytes
            when {
                decoded != null -> {
                    val bmp = remember(item.id) { BitmapFactory.decodeByteArray(decoded, 0, decoded.size) }
                    if (bmp != null) {
                        Image(bmp.asImageBitmap(), contentDescription = item.reason, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Couldn't display this image.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                !item.openableOnThisDevice ->
                    Text(
                        "Sealed to a previous key — can't be opened on this device.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                failed ->
                    Text("Couldn't decrypt this item.", style = MaterialTheme.typography.bodySmall)
                loading -> CircularProgressIndicator()
                else -> TextButton(onClick = {
                    loading = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { repo.openMedia(item.id) }.getOrNull() }
                        loading = false
                        if (result == null) failed = true else bytes = result
                    }
                }) { Text("Decrypt & view") }
            }
        }
    }
}
