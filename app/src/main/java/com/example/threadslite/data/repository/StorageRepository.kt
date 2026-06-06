package com.example.threadslite.data.repository

import android.net.Uri
import android.util.Log
import com.example.threadslite.util.Constants
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    companion object {
        private const val TAG = "StorageRepository"
    }

    suspend fun uploadAvatar(uid: String, uri: Uri): Result<String> = runCatching {
        require(uid.isNotBlank()) { "uid must not be blank." }
        Log.d(TAG, "uploadAvatar() uid=$uid uri=$uri")
        val ref = storage.reference.child("${Constants.STORAGE_AVATARS}/$uid.jpg")
        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Log.d(TAG, "uploadAvatar() OK url=$downloadUrl")
        downloadUrl
    }.also { result ->
        result.onFailure { e ->
            Log.e(TAG, "uploadAvatar() FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    suspend fun uploadPostImages(uid: String, uris: List<Uri>): Result<List<String>> = runCatching {
        require(uid.isNotBlank()) { "uid must not be blank." }
        require(uris.isNotEmpty()) { "uris must not be empty." }
        Log.d(TAG, "uploadPostImages() uid=$uid count=${uris.size}")
        val timestamp = System.currentTimeMillis()
        val urls = uris.mapIndexed { index, uri ->
            val ref = storage.reference
                .child("${Constants.STORAGE_POST_IMAGES}/$uid/${timestamp}_${index}.jpg")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Log.d(TAG, "uploadPostImages() [$index] OK url=$url")
            url
        }
        Log.d(TAG, "uploadPostImages() ALL done ${urls.size} files")
        urls
    }.also { result ->
        result.onFailure { e ->
            Log.e(TAG, "uploadPostImages() FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }
}
