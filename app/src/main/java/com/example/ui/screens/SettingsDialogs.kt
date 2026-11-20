package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.data.model.FolderSummary
import com.example.data.model.HomeSectionType
import com.example.ui.theme.LocalLumaGlass

@Composable
fun HomeLayoutDialog(
    homeSectionOrder: String,
    hiddenHomeSections: Set<String>,
    onSave: (String, Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    var ordered by remember(homeSectionOrder) { mutableStateOf(HomeSectionType.parseOrder(homeSectionOrder)) }
    var hidden by remember(hiddenHomeSections) { mutableStateOf(hiddenHomeSections) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalizar Ecrã Inicial") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(ordered, key = { _, item -> item.key }) { index, section ->
                    val isVisible = section.key !in hidden
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(glass.textPrimary.copy(alpha = 0.05f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                hidden = if (isVisible) hidden + section.key else hidden - section.key
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = if (isVisible) "Ocultar ${section.label}" else "Mostrar ${section.label}",
                                tint = if (isVisible) glass.accentPrimary else glass.textSecondary
                            )
                        }
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isVisible) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isVisible) glass.textPrimary else glass.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val mutable = ordered.toMutableList()
                                    val tmp = mutable[index - 1]
                                    mutable[index - 1] = mutable[index]
                                    mutable[index] = tmp
                                    ordered = mutable
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir secção")
                        }
                        IconButton(
                            onClick = {
                                if (index < ordered.lastIndex) {
                                    val mutable = ordered.toMutableList()
                                    val tmp = mutable[index + 1]
                                    mutable[index + 1] = mutable[index]
                                    mutable[index] = tmp
                                    ordered = mutable
                                }
                            },
                            enabled = index < ordered.lastIndex,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Descer secção")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(ordered.joinToString(",") { it.key }, hidden)
                    onDismiss()
                },
                modifier = Modifier.testTag("home_layout_save_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        ordered = HomeSectionType.entries
                        hidden = emptySet()
                    }
                ) {
                    Text("Repor")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
fun ExcludedFoldersDialog(
    folders: List<FolderSummary>,
    excludedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLumaGlass.current
    val allEntries = remember(folders, excludedFolders) {
        val known = folders.map { folder ->
            val key = folder.path.ifBlank { folder.name }
            Triple(key, folder.name, "${folder.itemCount} ficheiro(s)")
        }
        val knownKeys = known.map { it.first }.toSet()
        val extraExcluded = excludedFolders.filter { it !in knownKeys }.map { key ->
            Triple(key, key.substringAfterLast('/'), "Excluída manualmente")
        }
        known + extraExcluded
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pastas Excluídas") },
        text = {
            if (allEntries.isEmpty()) {
                Text(
                    text = "Ainda não existem pastas indexadas na biblioteca.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allEntries, key = { it.first }) { (key, title, subtitle) ->
                        val isExcluded = key in excludedFolders || title in excludedFolders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(glass.textPrimary.copy(alpha = 0.05f))
                                .clickable { onToggleFolder(key) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOff,
                                contentDescription = null,
                                tint = if (isExcluded) MaterialTheme.colorScheme.error else glass.textSecondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isExcluded) "Oculta da biblioteca e início" else subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isExcluded) MaterialTheme.colorScheme.error else glass.textSecondary,
                                    maxLines = 1
                                )
                            }
                            Switch(
                                checked = isExcluded,
                                onCheckedChange = { onToggleFolder(key) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Concluir") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomAccentColorDialog(
    currentHex: Long,
    onSelectHex: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val swatches = listOf(
        0L to "Padrão do tema",
        0xFFE11D74L to "Rosa Luma",
        0xFF10B981L to "Esmeralda",
        0xFF38BDF8L to "Azul Safira",
        0xFF8B5CF6L to "Violeta",
        0xFFF59E0BL to "Âmbar",
        0xFFEC4899L to "Magenta",
        0xFF14B8A6L to "Turquesa",
        0xFFEF4444L to "Rubi"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cor de Destaque Personalizada") },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                swatches.forEach { (hex, label) ->
                    val selected = currentHex == hex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectHex(hex)
                                onDismiss()
                            }
                            .padding(6.dp)
                            .testTag("accent_swatch_$hex")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hex == 0L) MaterialTheme.colorScheme.primary
                                    else Color(hex)
                                )
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selecionada",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}
