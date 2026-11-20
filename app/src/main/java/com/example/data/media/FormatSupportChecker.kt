package com.example.data.media

import android.media.MediaCodecList

data class FormatCapability(
    val formatName: String,
    val mimeType: String,
    val isVideo: Boolean,
    val isSupported: Boolean,
    val decoderNames: List<String>,
    val note: String
)

object FormatSupportChecker {

    private val audioTargets = listOf(
        Triple("MP3 (MPEG Audio)", "audio/mpeg", "Suportado nativamente por descodificadores do sistema"),
        Triple("AAC / M4A", "audio/mp4a-latm", "Suportado nativamente em contentores MP4/M4A"),
        Triple("FLAC (Lossless)", "audio/flac", "Áudio sem perdas suportado pelo descodificador FLAC"),
        Triple("WAV (PCM)", "audio/raw", "Áudio PCM não comprimido"),
        Triple("OGG / Vorbis", "audio/vorbis", "Contentor OGG com codec Vorbis"),
        Triple("Opus", "audio/opus", "Codec moderno de baixa latência em OGG/WebM"),
        Triple("AIFF", "audio/aiff", "Áudio PCM sem perdas em contentor AIFF (via extrator/codec)"),
        Triple("ALAC (Apple Lossless)", "audio/alac", "Depende do suporte de descodificador do dispositivo"),
        Triple("WMA (Windows Media)", "audio/x-ms-wma", "Disponível apenas com descodificador adicional no sistema"),
        Triple("AMR-NB / WB", "audio/3gpp", "Áudio comprimido padrão 3GPP"),
        Triple("AC-3 / Dolby", "audio/ac3", "Suportado apenas em dispositivos com descodificador licenciado")
    )

    private val videoTargets = listOf(
        Triple("MP4 / H.264 (AVC)", "video/avc", "Compatibilidade universal em dispositivos Android"),
        Triple("HEVC / H.265", "video/hevc", "Vídeo de alta eficiência (4K / HDR)"),
        Triple("VP9 (WebM / MKV)", "video/x-vnd.on2.vp9", "Contentores WebM e Matroska (MKV)"),
        Triple("VP8 (WebM)", "video/x-vnd.on2.vp8", "Contentores WebM antigos"),
        Triple("AV1", "video/av01", "Codec moderno em Android 10+ (hardware/software)"),
        Triple("MPEG-4 / MOV / MKV", "video/mp4v-es", "Contentores MP4, MOV e MKV legados")
    )

    fun inspectDeviceCodecs(): List<FormatCapability> {
        val codecInfos = runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter { !it.isEncoder }
        }.getOrDefault(emptyList())

        val results = mutableListOf<FormatCapability>()

        audioTargets.forEach { (name, mime, note) ->
            val decoders = codecInfos.filter { info ->
                info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
            }.map { it.name }
            val supported = decoders.isNotEmpty() || mime == "audio/raw"
            results.add(
                FormatCapability(
                    formatName = name,
                    mimeType = mime,
                    isVideo = false,
                    isSupported = supported,
                    decoderNames = decoders.take(3),
                    note = if (supported) note else "Descodificador não encontrado neste dispositivo"
                )
            )
        }

        videoTargets.forEach { (name, mime, note) ->
            val decoders = codecInfos.filter { info ->
                info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
            }.map { it.name }
            results.add(
                FormatCapability(
                    formatName = name,
                    mimeType = mime,
                    isVideo = true,
                    isSupported = decoders.isNotEmpty(),
                    decoderNames = decoders.take(3),
                    note = if (decoders.isNotEmpty()) note else "Descodificador não disponível neste dispositivo"
                )
            )
        }

        return results
    }
}
