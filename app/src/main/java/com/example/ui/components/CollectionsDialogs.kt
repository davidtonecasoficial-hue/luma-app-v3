package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomCollectionWithTracks
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.MediaArtwork

private val CollectionAccentOptions = listOf(
    "emerald" to Color(0xFF10B981),
    "cyan" to Color(0xFF0EA5E9),
    "violet" to Color(0xFF8B5CF6),
    "amber" to Color(0xFFF59E0B)
)

fun collectionAccentColor(tag: String): Color {
    return CollectionAccentOptions.firstOrNull { it.first == tag }?.second ?: Color(0xFF10B981)
}

@Composable
fun CreateEditCollectionDialog(
    initialName: String = "",
    initialDescription: String = "",
    initialAccentTag: String = "emerald",
    dialogTitle: String = "Nova Coleção",
    confirmLabel: String = "Criar",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, accentTag: String) -> Unit
) {
    val glass = LocalLumaGlass.current
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var accentTag by remember(initialAccentTag) { mutableStateOf(initialAccentTag) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da coleção") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collection_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição opcional") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Etiqueta visual",
                    style = MaterialTheme.typography.labelLarge,
                    color = glass.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CollectionAccentOptions.forEach { (key, color) ->
                        val selected = accentTag == key
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) glass.textPrimary else color.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { accentTag = key }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) {
                        onConfirm(clean, description.trim(), accentTag)
                    }
                },
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass.accentPrimary,
                    contentColor = glass.onAccent
                ),
                modifier = Modifier.testTag("collection_confirm_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun CollectionPickerDialog(
    collections: List<CustomCollectionWithTracks>,
    onSelectCollection: (Long) -> Unit,
    onCreateNew: (name: String, description: String, accentTag: String) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    var showCreateInline by remember { mutableStateOf(collections.isEmpty()) }

    if (showCreateInline) {
        CreateEditCollectionDialog(
            dialogTitle = "Nova Coleção",
            confirmLabel = "Criar e Guardar",
            onDismiss = {
                if (collections.isEmpty()) onDismiss() else showCreateInline = false
            },
            onConfirm = onCreateNew
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = { Text("Adicionar à Coleção") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(collections, key = { it.collection.id }) { col ->
                    val tagColor = collectionAccentColor(col.collection.accentTag)
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        onClick = { onSelectCollection(col.collection.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tagColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = col.collection.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${col.tracks.size} faixa(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.CollectionsBookmark,
                                contentDescription = null,
                                tint = tagColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showCreateInline = true }) {
                Text("Nova Coleção")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun MissingArtworkScannerDialog(
    missingTracks: List<MediaItemEntity>,
    autoMatchableCount: Int,
    onAutoMatchAll: () -> Unit,
    onPickCustomArtwork: (MediaItemEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.surfaceElevated,
        titleContentColor = glass.textPrimary,
        textContentColor = glass.textSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ImageSearch, contentDescription = null, tint = glass.accentPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scanner de Capas em Falta")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (missingTracks.isEmpty()) {
                        "Todas as músicas da biblioteca já têm capa associada."
                    } else {
                        "${missingTracks.size} música(s) sem capa. Os ficheiros originais não serão alterados."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (autoMatchableCount > 0) {
                    Button(
                        onClick = onAutoMatchAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = glass.accentPrimary,
                            contentColor = glass.onAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_match_artwork_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Associar $autoMatchableCount do álbum/pasta")
                    }
                }
                if (missingTracks.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(missingTracks, key = { it.id }) { track ->
                            LiquidGlassSurface(shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MediaArtwork(
                                        artworkPath = track.artworkPath,
                                        contentDescription = track.title,
                                        cornerRadius = 10.dp,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = glass.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artist} • ${track.album}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(onClick = { onPickCustomArtwork(track) }) {
                                        Text(
                                            text = "Escolher",
                                            color = glass.accentPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
