package com.example.player

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ConnectRouteKind(val label: String, val token: String) {
    SPEAKER("Altifalante do dispositivo", "Speaker"),
    WIRED("Auscultadores c/ Fio", "Wired"),
    BLUETOOTH("Bluetooth Áudio", "Bluetooth"),
    USB("Áudio USB", "USB"),
    CAST_HDMI("HDMI / Cast", "Cast")
}

@Immutable
data class ConnectedAudioDevice(
    val id: Int,
    val name: String,
    val kind: ConnectRouteKind,
    val typeLabel: String
)

@Immutable
data class LumaConnectUiState(
    val activeRoute: ConnectRouteKind = ConnectRouteKind.SPEAKER,
    val activeRouteLabel: String = ConnectRouteKind.SPEAKER.label,
    val devices: List<ConnectedAudioDevice> = emptyList(),
    val hasBluetooth: Boolean = false,
    val hasWiredOrUsb: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = true,
    val lastRouteEvent: String? = null
)

class LumaConnectManager(
    private val context: Context,
    private val playbackController: PlaybackController
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(LumaConnectUiState())
    val state: StateFlow<LumaConnectUiState> = _state.asStateFlow()

    @Volatile
    private var autoPauseOnDisconnect: Boolean = true

    @Volatile
    private var autoResumeOnBluetooth: Boolean = false

    @Volatile
    private var callbackRegistered = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            val btConnected = addedDevices?.any { isBluetoothType(it.type) } == true
            val label = addedDevices?.firstOrNull()?.productName?.toString()?.takeIf { it.isNotBlank() }
            refreshState(
                eventMessage = if (btConnected && label != null) {
                    "Dispositivo Bluetooth ligado: $label"
                } else null
            )
            if (btConnected && autoResumeOnBluetooth) {
                val pb = playbackController.uiState.value
                if (!pb.isPlaying && pb.currentTrack != null) {
                    playbackController.togglePlayPause()
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val externalDisconnected = removedDevices?.any {
                isExternalOutputType(it.type)
            } == true
            refreshState(
                eventMessage = if (externalDisconnected) {
                    "Saída de áudio externa desligada"
                } else null
            )
            if (externalDisconnected && autoPauseOnDisconnect) {
                val stillHasExternal = _state.value.hasBluetooth || _state.value.hasWiredOrUsb
                if (!stillHasExternal && playbackController.uiState.value.isPlaying) {
                    playbackController.pause()
                }
            }
        }
    }

    fun start() {
        refreshState()
        if (!callbackRegistered) {
            audioManager?.let { manager ->
                runCatching {
                    manager.registerAudioDeviceCallback(deviceCallback, mainHandler)
                    callbackRegistered = true
                }
            }
        }
    }

    fun stop() {
        if (callbackRegistered) {
            audioManager?.let { manager ->
                runCatching { manager.unregisterAudioDeviceCallback(deviceCallback) }
            }
            callbackRegistered = false
        }
    }

    fun updateAutomationPolicies(
        autoPauseDisconnect: Boolean,
        autoResumeBluetooth: Boolean
    ) {
        autoPauseOnDisconnect = autoPauseDisconnect
        autoResumeOnBluetooth = autoResumeBluetooth
    }

    fun currentRouteToken(): String = _state.value.activeRoute.token

    fun refreshState(eventMessage: String? = null) {
        val rawDevices = runCatching {
            audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList().orEmpty()
        }.getOrDefault(emptyList())

        val mapped = rawDevices.mapNotNull { info ->
            mapDeviceInfo(info)
        }.distinctBy { "${it.kind}_${it.name}" }

        val activeRoute = when {
            mapped.any { it.kind == ConnectRouteKind.BLUETOOTH } -> ConnectRouteKind.BLUETOOTH
            mapped.any { it.kind == ConnectRouteKind.USB } -> ConnectRouteKind.USB
            mapped.any { it.kind == ConnectRouteKind.WIRED } -> ConnectRouteKind.WIRED
            mapped.any { it.kind == ConnectRouteKind.CAST_HDMI } -> ConnectRouteKind.CAST_HDMI
            else -> ConnectRouteKind.SPEAKER
        }
        val activeName = mapped.firstOrNull { it.kind == activeRoute }?.name ?: activeRoute.label
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true }.getOrDefault(true)
        } else true

        _state.update { prev ->
            prev.copy(
                activeRoute = activeRoute,
                activeRouteLabel = activeName,
                devices = mapped,
                hasBluetooth = mapped.any { it.kind == ConnectRouteKind.BLUETOOTH },
                hasWiredOrUsb = mapped.any { it.kind == ConnectRouteKind.WIRED || it.kind == ConnectRouteKind.USB },
                isBatteryOptimizationIgnored = batteryIgnored,
                lastRouteEvent = eventMessage ?: prev.lastRouteEvent
            )
        }
    }

    private fun mapDeviceInfo(info: AudioDeviceInfo): ConnectedAudioDevice? {
        val (kind, typeLabel) = when (info.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> ConnectRouteKind.BLUETOOTH to "Bluetooth A2DP"
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL -> ConnectRouteKind.WIRED to "Cabo / Jack"
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_USB_HEADSET -> ConnectRouteKind.USB to "USB Áudio"
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> ConnectRouteKind.CAST_HDMI to "HDMI / Cast"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> ConnectRouteKind.SPEAKER to "Altifalante"
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (info.type == AudioDeviceInfo.TYPE_BLE_HEADSET || info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                ) {
                    ConnectRouteKind.BLUETOOTH to "LE Audio"
                } else {
                    return null
                }
            }
        }

        val product = info.productName?.toString()?.trim().orEmpty()
        val displayName = when {
            product.isNotBlank() && !product.equals(Build.MODEL, ignoreCase = true) -> product
            kind == ConnectRouteKind.SPEAKER -> "Altifalante do telemóvel"
            else -> typeLabel
        }
        return ConnectedAudioDevice(
            id = info.id,
            name = displayName,
            kind = kind,
            typeLabel = typeLabel
        )
    }

    private fun isBluetoothType(type: Int): Boolean {
        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
        ) return true
        return false
    }

    private fun isExternalOutputType(type: Int): Boolean {
        return isBluetoothType(type) ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            type == AudioDeviceInfo.TYPE_USB_DEVICE
    }

    companion object {
        fun createOutputSwitcherIntent(): Intent {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent("android.settings.panel.action.VOLUME")
            } else {
                Intent(Settings.ACTION_SOUND_SETTINGS)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun createBluetoothSettingsIntent(): Intent =
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun createBatterySettingsIntent(context: Context): Intent {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }
}
