package com.likeminds.feedsx.utils.file

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.likeminds.feedsx.utils.file.Constants.PathUri.COLUMN_DATA
import com.likeminds.feedsx.utils.file.Constants.PathUri.COLUMN_DISPLAY_NAME
import com.likeminds.feedsx.utils.file.Constants.PathUri.FOLDER_DOWNLOAD
import com.likeminds.feedsx.utils.file.ContentUriUtils.getPathFromColumn
import com.likeminds.feedsx.utils.file.FileUtil.getSubFolders
import com.likeminds.feedsx.utils.file.SDCardUtils.getStorageDirectories
import java.io.File

object PathUtils {
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     */
    internal fun getPath(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        //Document Provider
        return when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                when {
                    uri.isExternalStorageDocument -> externalStorageDocument(context, uri)
                    uri.isRawDownloadsDocument -> rawDownloadsDocument(contentResolver, uri)
                    uri.isDownloadsDocument -> downloadsDocument(contentResolver, uri)
                    uri.isMediaDocument -> mediaDocument(contentResolver, uri)
                    else -> {
                        return ""
                    }
                }
            }
            // MediaStore (and general)
            uri.isMediaStore -> {
                return if (uri.isGooglePhotosUri) {
                    googlePhotosUri(uri) ?: ""
                } else {
                    ""
                }
            }
            uri.isFile -> uri.path ?: ""
            else -> ""
        }
    }

    /**
     * Method for external document
     *
     */
    private fun externalStorageDocument(context: Context, uri: Uri): String {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":").toTypedArray()
        val type = split[0]
        if ("primary".equals(type, ignoreCase = true)) {
            return if (split.size > 1) {
                "${Environment.getExternalStorageDirectory()}/${split[1]}"
            } else {
                "${Environment.getExternalStorageDirectory()}/"
            }
        } else {
            val path = "storage/${docId.replace(":", "/")}"
            if (File(path).exists()) {
                return "/$path"
            }
            val availableExternalStorage = getStorageDirectories(context)
            var root = ""
            availableExternalStorage.forEach { storage ->
                root = if (split[1].startsWith("/")) {
                    "$storage${split[1]}"
                } else {
                    "$storage/${split[1]}"
                }
            }
            return if (root.contains(type)) {
                path
            } else {
                if (root.startsWith("/storage/") || root.startsWith("storage/")) {
                    root
                } else if (root.startsWith("/")) {
                    "/storage$root"
                } else {
                    "/storage/$root"
                }
            }
        }
    }

    /**
     * Method for rawDownloadDocument
     *
     */
    private fun rawDownloadsDocument(contentResolver: ContentResolver, uri: Uri): String {
        val fileName = getPathFromColumn(contentResolver, uri, COLUMN_DISPLAY_NAME)
        val subFolderName = getSubFolders(uri.toString())
        return if (fileName.isNotBlank()) {
            "${Environment.getExternalStorageDirectory()}/$FOLDER_DOWNLOAD/$subFolderName$fileName"
        } else {
            val id = DocumentsContract.getDocumentId(uri)
            val contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"),
                id.toLong()
            )
            getPathFromColumn(contentResolver, contentUri, COLUMN_DATA)
        }
    }

    /**
     * Method for downloadsDocument
     *
     */
    private fun downloadsDocument(contentResolver: ContentResolver, uri: Uri): String {
        val fileName = getPathFromColumn(contentResolver, uri, COLUMN_DISPLAY_NAME)
        val subFolderName = getSubFolders(uri.toString())
        if (fileName.isNotBlank()) {
            return "${Environment.getExternalStorageDirectory()}/$FOLDER_DOWNLOAD/$subFolderName$fileName"
        }
        var id = DocumentsContract.getDocumentId(uri)
        if (id.startsWith("raw:")) {
            id = id.replaceFirst("raw:".toRegex(), "")
            val file = File(id)
            if (file.exists()) return id
        } else if (id.startsWith("raw%3A%2F")) {
            id = id.replaceFirst("raw%3A%2F".toRegex(), "")
            val file = File(id)
            if (file.exists()) return id
        }
        val contentUri = ContentUris.withAppendedId(
            Uri.parse("content://downloads/public_downloads"),
            id.toLong()
        )
        return getPathFromColumn(contentResolver, contentUri, COLUMN_DATA)
    }

    /**
     * Method for MediaDocument
     *
     */
    private fun mediaDocument(contentResolver: ContentResolver, uri: Uri): String {
        val docId = DocumentsContract.getDocumentId(uri)
        val split: Array<String?> = docId.split(":").toTypedArray()
        val contentUri: Uri =
            when (split[0]) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri(docId)
            }
        val selection = "_id=?"
        val selectionArgs = arrayOf(split[1])
        return getPathFromColumn(
            contentResolver,
            contentUri,
            COLUMN_DATA,
            selection,
            selectionArgs
        )
    }

    /**
     * Method for googlePhotos
     *
     */
    private fun googlePhotosUri(uri: Uri): String? {
        // Return the remote address
        return uri.lastPathSegment
    }
}