package com.vibeplay.app.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.vibeplay.core.model.Track
import com.vibeplay.core.scanner.LocalMusicScanner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.io.InputStream

internal data class TreeEntry(val id: String, val name: String, val isDirectory: Boolean, val mimeType: String? = null)

internal interface MusicDocumentTree {
    fun children(id: String): List<TreeEntry>
    fun uri(id: String): String
    fun open(id: String): InputStream?
    fun otherAudio(entry: TreeEntry, parentName: String?): Track? = null
}

/** Traverses the selected tree and retains document URIs as playback locations. */
internal suspend fun scanMusicTree(tree: MusicDocumentTree, rootId: String): List<Track> {
    val tracks = mutableListOf<Track>()
    val pending = ArrayDeque<Pair<String, String?>>()
    val visited = mutableSetOf<String>()
    pending.add(rootId to null)
    while (pending.isNotEmpty()) {
        currentCoroutineContext().ensureActive()
        val (parentId, parentName) = pending.removeFirst()
        if (!visited.add(parentId)) continue
        for (entry in tree.children(parentId)) {
            currentCoroutineContext().ensureActive()
            if (entry.isDirectory) {
                pending.add(entry.id to entry.name)
            } else if (entry.name.endsWith(".flac", ignoreCase = true)) {
                try {
                    val info = tree.open(entry.id)?.use { LocalMusicScanner.parseFlacStream(it) }
                    if (info != null) {
                        val uri = tree.uri(entry.id)
                        tracks.add(LocalMusicScanner.trackFromFlac(info, entry.name, parentName, uri, uri))
                    }
                } catch (_: IOException) {
                    // Skip unreadable or malformed individual files.
                }
            } else if (entry.mimeType?.startsWith("audio/") == true ||
                entry.name.substringAfterLast('.', "").lowercase() in setOf("mp3", "wav", "m4a", "ogg", "aac")) {
                tree.otherAudio(entry, parentName)?.let(tracks::add)
            }
        }
    }
    return tracks
}

/** Call on an IO dispatcher. */
object SafMusicScanner {
    suspend fun scanTree(context: Context, treeUri: Uri): List<Track> {
        val resolver = context.contentResolver
        val tree = object : MusicDocumentTree {
            override fun children(id: String): List<TreeEntry> {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, id)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )
                val cursor = resolver.query(children, projection, null, null, null)
                    ?: throw IOException("Could not read the selected folder")
                return cursor.use {
                    val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    buildList {
                        while (it.moveToNext()) {
                            val childId = it.getString(idColumn) ?: continue
                            val name = it.getString(nameColumn) ?: continue
                            val mime = it.getString(mimeColumn)
                            add(TreeEntry(childId, name, mime == DocumentsContract.Document.MIME_TYPE_DIR, mime))
                        }
                    }
                }
            }

            override fun uri(id: String): String = DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString()
            override fun open(id: String): InputStream? = resolver.openInputStream(Uri.parse(uri(id)))
            override fun otherAudio(entry: TreeEntry, parentName: String?): Track {
                val documentUri = uri(entry.id)
                val retriever = MediaMetadataRetriever()
                var title: String? = null
                var artist: String? = null
                var album: String? = null
                var duration = 0L
                var sampleRate = 44100
                try {
                    retriever.setDataSource(context, Uri.parse(documentUri))
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()?.takeIf { it > 0 } ?: 44100
                } catch (_: Exception) {
                    // Keep the document available even when metadata is missing.
                } finally {
                    retriever.release()
                }
                val tags = mapOf("TITLE" to title, "ARTIST" to artist, "ALBUM" to album)
                    .mapNotNull { (key, value) -> value?.takeIf { it.isNotBlank() }?.let { key to it } }.toMap()
                val info = LocalMusicScanner.FlacInfo(sampleRate, 24, 2, 0, duration, tags)
                return LocalMusicScanner.trackFromFlac(info, entry.name, parentName, documentUri, documentUri)
                    .copy(format = entry.name.substringAfterLast('.', "AUDIO").uppercase())
            }
        }
        return scanMusicTree(tree, DocumentsContract.getTreeDocumentId(treeUri))
    }
}
