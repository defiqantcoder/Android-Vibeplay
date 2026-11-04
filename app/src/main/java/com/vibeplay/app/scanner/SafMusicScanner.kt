package com.vibeplay.app.scanner

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.vibeplay.core.model.Track
import com.vibeplay.core.scanner.LocalMusicScanner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.io.InputStream

internal data class TreeEntry(val id: String, val name: String, val isDirectory: Boolean)

internal interface MusicDocumentTree {
    fun children(id: String): List<TreeEntry>
    fun uri(id: String): String
    fun open(id: String): InputStream?
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
            }
        }
    }
    return tracks
}

/** Call on an IO dispatcher. */
object SafMusicScanner {
    suspend fun scanTree(resolver: ContentResolver, treeUri: Uri): List<Track> {
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
                            add(TreeEntry(childId, name, it.getString(mimeColumn) == DocumentsContract.Document.MIME_TYPE_DIR))
                        }
                    }
                }
            }

            override fun uri(id: String): String = DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString()
            override fun open(id: String): InputStream? = resolver.openInputStream(Uri.parse(uri(id)))
        }
        return scanMusicTree(tree, DocumentsContract.getTreeDocumentId(treeUri))
    }
}
