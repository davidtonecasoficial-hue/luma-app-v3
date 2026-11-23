package com.example.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import com.example.config.AppConfig
import com.example.data.model.MediaItemEntity
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ImportSummary(
    val importedCount: Int,
    val duplicateCount: Int,
    val unsupportedOrFailedCount: Int,
    val messages: List<String>
)

data class MediaStoreScanSummary(
    val totalScanned: Int,
    val addedCount: Int,
    val updatedCount: Int,
    val missingCount: Int
)

class MediaScanner(private val context: Context) {

    private val artworkDir: File by lazy {
        File(context.cacheDir, "artwork").apply { if (!exists()) mkdirs() }
    }

    suspend fun extractMediaItem(uri: Uri): Result<MediaItemEntity> = withContext(Dispatchers.IO) {
        runCatching {
            tryPersistPermission(uri)

            val contentResolver = context.contentResolver
            val (fileName, sizeBytes) = queryFileNameAndSize(contentResolver, uri)
            val extension = fileName.substringAfterLast('.', "").lowercase()
            val detectedMime = contentResolver.getType(uri).orEmpty()

            val isVideo = detectedMime.startsWith("video/") ||
                (extension in AppConfig.VIDEO_EXTENSIONS && !detectedMime.startsWith("audio/"))
            val isAudio = detectedMime.startsWith("audio/") || extension in AppConfig.AUDIO_EXTENSIONS

            if (!isAudio && !isVideo) {
                throw IllegalArgumentException("Formato não suportado: $fileName")
            }

            val mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)

                val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val rawAlbumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                val rawGenre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                val rawYear = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                val rawTrackNum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0

                val artworkPath = extractArtworkOrThumbnail(retriever, uri, mediaType)
                val folderInfo = resolveFolderInfo(uri)
                val parsedFile = ArtistNormalizer.parseFilenameMetadata(fileName)

                val cleanTitle = ArtistNormalizer.cleanDisplayTag(rawTitle).ifBlank {
                    parsedFile.title.ifBlank { fileName.substringBeforeLast('.', fileName) }
                }
                val cleanArtist = if (mediaType == MediaType.VIDEO) {
                    ArtistNormalizer.cleanDisplayTag(rawArtist).ifBlank { "Vídeo local" }
                } else {
                    ArtistNormalizer.normalizePrimaryArtist(
                        rawArtist = rawArtist ?: rawAlbumArtist,
                        fallbackFileName = fileName
                    )
                }
                val cleanAlbum = ArtistNormalizer.cleanDisplayTag(rawAlbum).ifBlank {
                    if (mediaType == MediaType.VIDEO) "Vídeos"
                    else folderInfo.first.takeIf { !it.equals("Importados", true) } ?: "Álbum desconhecido"
                }
                val releaseYear = rawYear?.trim()?.take(4)?.toIntOrNull()
                    ?.takeIf { it in 1900..2100 } ?: parsedFile.releaseYear
                val trackNumber = rawTrackNum?.substringBefore('/')?.trim()?.toIntOrNull()
                    ?.takeIf { it > 0 } ?: parsedFile.trackNumber
                val resolvedGenre = ArtistNormalizer.cleanDisplayTag(rawGenre).ifBlank {
                    if (mediaType == MediaType.AUDIO) {
                        ArtistNormalizer.inferGenre(cleanTitle, cleanArtist, cleanAlbum, folderInfo.first, durationMs)
                    } else ""
                }

                MediaItemEntity(
                    uri = uri.toString(),
                    mediaType = mediaType,
                    title = cleanTitle,
                    artist = cleanArtist,
                    album = cleanAlbum,
                    albumArtist = ArtistNormalizer.cleanDisplayTag(rawAlbumArtist),
                    genre = resolvedGenre,
                    releaseYear = releaseYear,
                    trackNumber = trackNumber,
                    durationMs = durationMs,
                    fileName = fileName,
                    folderName = folderInfo.first,
                    folderPath = folderInfo.second,
                    mimeType = detectedMime.ifEmpty { guessMimeFromExtension(extension, mediaType) },
                    sizeBytes = sizeBytes,
                    videoWidth = width,
                    videoHeight = height,
                    artworkPath = artworkPath,
                    embeddedLyrics = null,
                    addedAt = System.currentTimeMillis(),
                    isAvailable = true
                )
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    suspend fun scanMediaStore(
        scanAudio: Boolean,
        scanVideo: Boolean
    ): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<MediaItemEntity>()
        if (scanAudio) {
            discovered += queryMediaStoreAudio()
        }
        if (scanVideo) {
            discovered += queryMediaStoreVideo()
        }
        discovered
    }

    private fun queryMediaStoreAudio(): List<MediaItemEntity> {
        val results = mutableListOf<MediaItemEntity>()
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.IS_MUSIC)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.TRACK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= 5000)"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(collectionUri, projection, selection, null, sortOrder)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val nameIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateAddedIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val yearIdx = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val trackIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val relPathIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1
                @Suppress("DEPRECATION")
                val dataIdx = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                } else -1
                val genreIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val displayName = cursor.safeString(nameIdx).ifBlank { "faixa_$id.mp3" }
                    val parsedFile = ArtistNormalizer.parseFilenameMetadata(displayName)
                    val title = ArtistNormalizer.cleanDisplayTag(cursor.safeString(titleIdx))
                        .ifBlank { parsedFile.title.ifBlank { displayName.substringBeforeLast('.', displayName) } }
                    val artist = ArtistNormalizer.normalizePrimaryArtist(
                        rawArtist = cursor.safeString(artistIdx),
                        fallbackFileName = displayName
                    )
                    val album = ArtistNormalizer.cleanDisplayTag(cursor.safeString(albumIdx))
                        .ifBlank { "Álbum desconhecido" }
                    val albumId = if (albumIdIdx >= 0 && !cursor.isNull(albumIdIdx)) cursor.getLong(albumIdIdx) else 0L
                    val durationMs = if (durationIdx >= 0 && !cursor.isNull(durationIdx)) cursor.getLong(durationIdx).coerceAtLeast(0L) else 0L
                    val mime = cursor.safeString(mimeIdx).ifBlank {
                        guessMimeFromExtension(displayName.substringAfterLast('.', "").lowercase(), MediaType.AUDIO)
                    }
                    val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx).coerceAtLeast(0L) else 0L
                    val addedSec = if (dateAddedIdx >= 0 && !cursor.isNull(dateAddedIdx)) cursor.getLong(dateAddedIdx) else 0L
                    val addedAt = if (addedSec > 0L) addedSec * 1000L else System.currentTimeMillis()
                    val (folderName, folderPath) = parseMediaStoreFolder(
                        relativePath = cursor.safeString(relPathIdx),
                        legacyDataPath = cursor.safeString(dataIdx)
                    )
                    val genre = ArtistNormalizer.cleanDisplayTag(cursor.safeString(genreIdx)).ifBlank {
                        ArtistNormalizer.inferGenre(title, artist, album, folderName, durationMs)
                    }
                    val rawYear = if (yearIdx >= 0 && !cursor.isNull(yearIdx)) cursor.getInt(yearIdx) else 0
                    val releaseYear = rawYear.takeIf { it in 1900..2100 } ?: parsedFile.releaseYear
                    val rawTrack = if (trackIdx >= 0 && !cursor.isNull(trackIdx)) cursor.getInt(trackIdx) else 0
                    val trackNumber = (rawTrack % 1000).takeIf { it > 0 } ?: parsedFile.trackNumber

                    val artUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    results.add(
                        MediaItemEntity(
                            uri = contentUri.toString(),
                            mediaType = MediaType.AUDIO,
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre,
                            releaseYear = releaseYear,
                            trackNumber = trackNumber,
                            durationMs = durationMs,
                            fileName = displayName,
                            folderName = folderName,
                            folderPath = folderPath,
                            mimeType = mime,
                            sizeBytes = size,
                            artworkPath = artUri,
                            addedAt = addedAt,
                            isAvailable = true
                        )
                    )
                }
            }
        }
        return results
    }

    private fun queryMediaStoreVideo(): List<MediaItemEntity> {
        val results = mutableListOf<MediaItemEntity>()
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.TITLE)
            add(MediaStore.Video.Media.ARTIST)
            add(MediaStore.Video.Media.ALBUM)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Video.Media.DATA)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        var thumbnailsGenerated = 0

        runCatching {
            context.contentResolver.query(collectionUri, projection, null, null, sortOrder)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val titleIdx = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val artistIdx = cursor.getColumnIndex(MediaStore.Video.Media.ARTIST)
                val albumIdx = cursor.getColumnIndex(MediaStore.Video.Media.ALBUM)
                val durationIdx = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val nameIdx = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthIdx = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightIdx = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val dateAddedIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val relPathIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else -1
                @Suppress("DEPRECATION")
                val dataIdx = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                } else -1

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    val displayName = cursor.safeString(nameIdx).ifBlank { "video_$id.mp4" }
                    val title = cursor.safeString(titleIdx)
                        .takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: displayName.substringBeforeLast('.', displayName)
                    val artist = cursor.safeString(artistIdx)
                        .takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: "Vídeo local"
                    val album = cursor.safeString(albumIdx)
                        .takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: "Vídeos"
                    val durationMs = if (durationIdx >= 0 && !cursor.isNull(durationIdx)) cursor.getLong(durationIdx).coerceAtLeast(0L) else 0L
                    val mime = cursor.safeString(mimeIdx).ifBlank {
                        guessMimeFromExtension(displayName.substringAfterLast('.', "").lowercase(), MediaType.VIDEO)
                    }
                    val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx).coerceAtLeast(0L) else 0L
                    val width = if (widthIdx >= 0 && !cursor.isNull(widthIdx)) cursor.getInt(widthIdx) else 0
                    val height = if (heightIdx >= 0 && !cursor.isNull(heightIdx)) cursor.getInt(heightIdx) else 0
                    val addedSec = if (dateAddedIdx >= 0 && !cursor.isNull(dateAddedIdx)) cursor.getLong(dateAddedIdx) else 0L
                    val addedAt = if (addedSec > 0L) addedSec * 1000L else System.currentTimeMillis()
                    val (folderName, folderPath) = parseMediaStoreFolder(
                        relativePath = cursor.safeString(relPathIdx),
                        legacyDataPath = cursor.safeString(dataIdx)
                    )

                    val thumbFile = File(artworkDir, "ms_vid_$id.jpg")
                    if (!thumbFile.exists() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thumbnailsGenerated < 20) {
                        thumbnailsGenerated++
                        runCatching {
                            val bmp = context.contentResolver.loadThumbnail(contentUri, Size(360, 360), null)
                            FileOutputStream(thumbFile).use { stream ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                            }
                            bmp.recycle()
                        }
                    }
                    val thumbPath = thumbFile.takeIf { it.exists() && it.length() > 0L }?.absolutePath

                    results.add(
                        MediaItemEntity(
                            uri = contentUri.toString(),
                            mediaType = MediaType.VIDEO,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            fileName = displayName,
                            folderName = folderName,
                            folderPath = folderPath,
                            mimeType = mime,
                            sizeBytes = size,
                            videoWidth = width,
                            videoHeight = height,
                            artworkPath = thumbPath,
                            addedAt = addedAt,
                            isAvailable = true
                        )
                    )
                }
            }
        }
        return results
    }

    private fun parseMediaStoreFolder(relativePath: String, legacyDataPath: String): Pair<String, String> {
        val cleanedRel = relativePath.trim().trimEnd('/')
        if (cleanedRel.isNotEmpty()) {
            val name = cleanedRel.substringAfterLast('/').ifBlank { "Armazenamento" }
            return name to cleanedRel
        }
        val parent = legacyDataPath.substringBeforeLast('/', "").trim()
        if (parent.isNotEmpty()) {
            val name = parent.substringAfterLast('/').ifBlank { "Armazenamento" }
            return name to parent
        }
        return "Armazenamento" to "MediaStore"
    }

    private fun Cursor.safeString(columnIndex: Int): String {
        if (columnIndex < 0 || isNull(columnIndex)) return ""
        return getString(columnIndex).orEmpty().trim()
    }

    suspend fun collectMediaUrisFromTree(treeUri: Uri): List<Uri> =
        collectMediaAndLrcFromTree(treeUri).first

    suspend fun collectMediaAndLrcFromTree(treeUri: Uri): Pair<List<Uri>, Map<String, Uri>> = withContext(Dispatchers.IO) {
        val collected = mutableListOf<Uri>()
        val lrcSidecars = mutableMapOf<String, Uri>()
        runCatching {
            tryPersistPermission(treeUri)
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            walkDocumentTree(treeUri, rootDocId, collected, lrcSidecars, maxDepth = 4, currentDepth = 0)
        }
        collected to lrcSidecars
    }

    private fun walkDocumentTree(
        treeUri: Uri,
        parentDocId: String,
        output: MutableList<Uri>,
        lrcSidecars: MutableMap<String, Uri>,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        walkDocumentTree(treeUri, docId, output, lrcSidecars, maxDepth, currentDepth + 1)
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext == "lrc" || ext == "txt") {
                            val baseKey = name.substringBeforeLast('.', name).lowercase().trim()
                            // Prefer .lrc over .txt if both exist
                            if (ext == "lrc" || baseKey !in lrcSidecars) {
                                lrcSidecars[baseKey] = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            }
                            continue
                        }
                        val isMedia = mime.startsWith("audio/") ||
                            mime.startsWith("video/") ||
                            ext in AppConfig.AUDIO_EXTENSIONS ||
                            ext in AppConfig.VIDEO_EXTENSIONS
                        if (isMedia) {
                            output.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, docId))
                        }
                    }
                }
            }
        }
    }

    suspend fun isUriAccessible(uriString: String): Boolean = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext false
        if (uri.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) return@withContext true
        runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun tryPersistPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun queryFileNameAndSize(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Ficheiro multimédia"
        var size = 0L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        }
        return name to size
    }

    private fun resolveFolderInfo(uri: Uri): Pair<String, String> {
        val path = uri.path.orEmpty()
        val segments = path.split('/').filter { it.isNotBlank() }
        val folderName = if (segments.size >= 2) {
            segments[segments.size - 2].substringAfter(':').substringAfterLast('/').ifBlank { "Importados" }
        } else {
            "Importados"
        }
        val folderPath = path.substringBeforeLast('/', "dispositivo/importados")
        return folderName to folderPath
    }

    private fun extractArtworkOrThumbnail(
        retriever: MediaMetadataRetriever,
        uri: Uri,
        mediaType: MediaType
    ): String? {
        val hash = uri.toString().hashCode().toUInt().toString(16)
        val outFile = File(artworkDir, "art_$hash.jpg")
        if (outFile.exists() && outFile.length() > 0L) {
            return outFile.absolutePath
        }

        if (mediaType == MediaType.AUDIO) {
            val bytes = runCatching { retriever.embeddedPicture }.getOrNull()
            if (bytes != null && bytes.isNotEmpty()) {
                runCatching {
                    FileOutputStream(outFile).use { it.write(bytes) }
                }
                if (outFile.exists() && outFile.length() > 0L) return outFile.absolutePath
            }
        } else {
            val frame = runCatching {
                retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            }.getOrNull()
            if (frame != null) {
                try {
                    runCatching {
                        FileOutputStream(outFile).use { stream ->
                            frame.compress(Bitmap.CompressFormat.JPEG, 82, stream)
                        }
                    }
                } finally {
                    runCatching { if (!frame.isRecycled) frame.recycle() }
                }
                if (outFile.exists() && outFile.length() > 0L) return outFile.absolutePath
            }
        }
        return null
    }

    suspend fun readLyricsFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Não foi possível ler o ficheiro de letras.")
            val text = bytes.toString(Charsets.UTF_8).trim()
            if (text.isBlank()) {
                throw IllegalArgumentException("O ficheiro de letras está vazio.")
            }
            text
        }
    }

    suspend fun saveCustomArtworkFromUri(mediaId: Long, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val outFile = File(artworkDir, "custom_art_${mediaId}_${System.currentTimeMillis()}.jpg")
            val sampled = com.example.config.LumaCrashGuard.decodeSampledBitmap(
                context = context,
                path = imageUri.toString(),
                maxDimensionPx = 1024
            )
            if (sampled != null) {
                try {
                    FileOutputStream(outFile).use { stream ->
                        sampled.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    }
                } finally {
                    runCatching { if (!sampled.isRecycled) sampled.recycle() }
                }
            } else {
                val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Não foi possível abrir a imagem selecionada.")
                FileOutputStream(outFile).use { it.write(bytes) }
            }
            outFile.absolutePath
        }
    }

    suspend fun calculateCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        dirSize(context.cacheDir)
    }

    suspend fun clearAppCache(): Long = withContext(Dispatchers.IO) {
        val before = dirSize(context.cacheDir)
        context.cacheDir.listFiles()?.forEach { child ->
            runCatching { child.deleteRecursively() }
        }
        artworkDir.mkdirs()
        val after = dirSize(context.cacheDir)
        (before - after).coerceAtLeast(0L)
    }

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var total = 0L
        dir.listFiles()?.forEach { child ->
            total += dirSize(child)
        }
        return total
    }

    private fun guessMimeFromExtension(ext: String, type: MediaType): String {
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg", "oga" -> "audio/ogg"
            "opus" -> "audio/opus"
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            else -> if (type == MediaType.VIDEO) "video/*" else "audio/*"
        }
    }
}
