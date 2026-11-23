package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.MediaItemEntity
import com.example.player.ConnectRouteKind
import com.example.player.LumaConnectManager
import com.example.player.LumaConnectUiState
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalLumaGlass
import com.example.ui.theme.formatDuration

@Composable
fun ConnectScreen(
    connectState: LumaConnectUiState,
    settings: AppSettings,
    lastSavedTrack: MediaItemEntity?,
    bottomInsetPadding: Dp,
    onBack: () -> Unit,
    onRefreshDevices: () -> Unit,
    onToggleAutoPauseDisconnect: (Boolean) -> Unit,
    onToggleAutoResumeBluetooth: (Boolean) -> Unit,
    onToggleHeadsetDoubleTapSkip: (Boolean) -> Unit,
    onRestoreLastSession: () -> Unit,
    onLaunchError: (String) -> Unit
) {
    BackHandler { onBack() }

    val glass = LocalLumaGlass.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("luma_connect_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = bottomInsetPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("connect_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Luma Connect", style = MaterialTheme.typography.headlineMedium, color = glass.textPrimary)
                    Text(
                        "Bluetooth, Android Auto, auscultadores e restauração",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
                IconButton(onClick = onRefreshDevices, modifier = Modifier.testTag("connect_refresh_button")) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar saídas")
                }
            }
        }

        // Active Output Route Hero Card
        item {
            LiquidGlassSurface(
                shape = RoundedCornerShape(26.dp),
                elevated = true,
                accentGlow = glass.accentPrimary,
                modifier = Modifier.testTag("connect_active_route_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(glass.accentPrimary, glass.accentSecondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(routeIcon(connectState.activeRoute), contentDescription = null, tint = glass.onAccent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Saída de Áudio Ativa", style = MaterialTheme.typography.labelMedium, color = glass.accentPrimary)
                            Text(
                                text = connectState.activeRouteLabel,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            connectState.lastRouteEvent?.let { evt ->
                                Text(evt, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(LumaConnectManager.createOutputSwitcherIntent())
                                }.onFailure {
                                    onLaunchError("Não foi possível abrir o seletor de saída neste dispositivo.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accentPrimary, contentColor = glass.onAccent),
                            modifier = Modifier.weight(1f).testTag("connect_output_switcher_button")
                        ) {
                            Icon(Icons.Rounded.Speaker, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trocar Saída")
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(LumaConnectManager.createBluetoothSettingsIntent())
                                }.onFailure {
                                    onLaunchError("Não foi possível abrir as definições de Bluetooth.")
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("connect_bluetooth_settings_button")
                        ) {
                            Icon(Icons.Rounded.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bluetooth")
                        }
                    }
                }
            }
        }

        // Detected Hardware Routes
        item {
            Text("Dispositivos de Saída Detetados (${connectState.devices.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (connectState.devices.isEmpty()) {
            item {
                LiquidGlassSurface(shape = RoundedCornerShape(18.dp)) {
                    Text(
                        text = "Nenhum dispositivo externo detetado — saída padrão pelo altifalante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(connectState.devices, key = { "dev_${it.id}_${it.kind.name}" }) { dev ->
                LiquidGlassSurface(shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (dev.kind == connectState.activeRoute) glass.accentPrimary.copy(alpha = 0.2f)
                                    else glass.glassSurface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = routeIcon(dev.kind),
                                contentDescription = null,
                                tint = if (dev.kind == connectState.activeRoute) glass.accentPrimary else glass.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dev.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(dev.typeLabel, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                        }
                        if (dev.kind == connectState.activeRoute) {
                            Text(
                                "Em uso",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = glass.accentPrimary
                            )
                        }
                    }
                }
            }
        }

        // Headphone & Bluetooth Smart Automation
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Controlos de Auscultadores & Automação", style = MaterialTheme.typography.titleMedium)

                    ConnectToggleRow(
                        title = "Pausar ao desligar dispositivo",
                        subtitle = "Pausa automaticamente ao remover cabo ou desligar Bluetooth",
                        checked = settings.autoPauseOnDisconnect,
                        onCheckedChange = onToggleAutoPauseDisconnect
                    )
                    ConnectToggleRow(
                        title = "Retomar ao ligar Bluetooth",
                        subtitle = "Continua a reprodução atual quando um auricular Bluetooth se liga",
                        checked = settings.autoResumeOnBluetooth,
                        onCheckedChange = onToggleAutoResumeBluetooth
                    )
                    ConnectToggleRow(
                        title = "Botão de auricular: toque duplo avança",
                        subtitle = "Controlo rápido com botões de auscultadores e volante",
                        checked = settings.headsetButtonDoubleTapSkip,
                        onCheckedChange = onToggleHeadsetDoubleTapSkip
                    )
                }
            }
        }

        // Android Auto & External MediaSession Compatibility
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = glass.accentPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Android Auto & Controlos Externos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = glass.accentPrimary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "O serviço MediaLibraryService do Luma expõe navegação completa ao Android Auto, relógios Wear OS e ecrã de bloqueio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Text(
                        "Catálogo ativo: Músicas • Auto DJ • Coleções • Favoritos • Artistas • Álbuns • Playlists",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.accentSecondary
                    )
                }
            }
        }

        // Session Restoration & Background Reliability
        item {
            LiquidGlassSurface(shape = RoundedCornerShape(22.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = glass.accentPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restauração de Reprodução", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = if (lastSavedTrack != null) {
                            "Última sessão guardada: ${lastSavedTrack.title} (${formatDuration(settings.lastPositionMs)})"
                        } else {
                            "Inicie uma música para guardar automaticamente a posição e a fila."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    OutlinedButton(
                        onClick = onRestoreLastSession,
                        enabled = lastSavedTrack != null,
                        modifier = Modifier.fillMaxWidth().testTag("connect_restore_session_button")
                    ) {
                        Text("Restaurar Última Fila & Posição")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = glass.accentSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fiabilidade em Segundo Plano", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (connectState.isBatteryOptimizationIgnored) {
                                    "Sem restrições agressivas de bateria"
                                } else {
                                    "Otimização de bateria ativa (pode suspender serviço)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(LumaConnectManager.createBatterySettingsIntent(context))
                                }.onFailure {
                                    onLaunchError("Não foi possível abrir definições de bateria.")
                                }
                            }
                        ) {
                            Text("Gerir")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val glass = LocalLumaGlass.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = glass.onAccent,
                checkedTrackColor = glass.accentPrimary
            )
        )
    }
}

private fun routeIcon(kind: ConnectRouteKind): ImageVector = when (kind) {
    ConnectRouteKind.BLUETOOTH -> Icons.Rounded.Bluetooth
    ConnectRouteKind.WIRED -> Icons.Rounded.Headphones
    ConnectRouteKind.USB -> Icons.Rounded.Usb
    ConnectRouteKind.CAST_HDMI -> Icons.Rounded.CastConnected
    ConnectRouteKind.SPEAKER -> Icons.Rounded.Speaker
}
