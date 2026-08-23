package com.vyrena.mconbridge.importer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

object StoragePathMapper {
    fun primaryTreePath(context: Context, treeUri: Uri): String? = runCatching {
        require(treeUri.authority == "com.android.externalstorage.documents")
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val parts = documentId.split(':', limit = 2)
        require(parts.size == 2 && parts[0].equals("primary", ignoreCase = true))
        val relative = parts[1].trim('/').takeIf { it.isNotEmpty() } ?: return@runCatching "/storage/emulated/0"
        require(relative.split('/').none { it == ".." || it == "." || it.isBlank() })
        "/storage/emulated/0/$relative"
    }.getOrNull()
}
