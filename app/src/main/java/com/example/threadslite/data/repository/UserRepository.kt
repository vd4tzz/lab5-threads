package com.example.threadslite.data.repository

import android.util.Log
import com.example.threadslite.data.model.User
import com.example.threadslite.util.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object { private const val TAG = "UserRepository" }

    // ─── Read ──────────────────────────────────────────────────────────────────

    suspend fun getUser(uid: String): Result<User> = runCatching {
        require(uid.isNotBlank()) { "uid must not be blank." }
        Log.d(TAG, "getUser() uid=$uid")

        val snapshot = db.collection(Constants.COLLECTION_USERS).document(uid).get().await()

        if (!snapshot.exists()) throw NoSuchElementException("User not found: uid=$uid")

        snapshot.toObject(User::class.java)
            ?: User(
                uid       = snapshot.getString("uid")       ?: uid,
                username  = snapshot.getString("username")  ?: "",
                email     = snapshot.getString("email")     ?: "",
                avatarUrl = snapshot.getString("avatarUrl") ?: "",
                createdAt = snapshot.getTimestamp("createdAt") ?: Timestamp.now()
            )
    }.also { it.onFailure { e -> Log.e(TAG, "getUser() FAILED: ${e.message}", e) } }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_USERS).document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                if (snap == null || !snap.exists()) { trySend(null); return@addSnapshotListener }

                val user = snap.toObject(User::class.java)
                    ?: User(
                        uid       = snap.getString("uid")       ?: uid,
                        username  = snap.getString("username")  ?: "",
                        email     = snap.getString("email")     ?: "",
                        avatarUrl = snap.getString("avatarUrl") ?: "",
                        createdAt = snap.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    // ─── Write ─────────────────────────────────────────────────────────────────

    suspend fun updateUsername(uid: String, newUsername: String): Result<Unit> = runCatching {
        require(uid.isNotBlank())         { "uid must not be blank." }
        require(newUsername.isNotBlank()) { "username must not be blank." }
        Log.d(TAG, "updateUsername() uid=$uid new=$newUsername")
        db.collection(Constants.COLLECTION_USERS).document(uid)
            .update("username", newUsername.trim()).await()
        Unit
    }.also { it.onFailure { e -> Log.e(TAG, "updateUsername() FAILED: ${e.message}", e) } }

    suspend fun updateAvatar(uid: String, avatarUrl: String): Result<Unit> = runCatching {
        require(uid.isNotBlank()) { "uid must not be blank." }
        Log.d(TAG, "updateAvatar() uid=$uid")
        db.collection(Constants.COLLECTION_USERS).document(uid)
            .update("avatarUrl", avatarUrl.trim()).await()
        Unit
    }.also { it.onFailure { e -> Log.e(TAG, "updateAvatar() FAILED: ${e.message}", e) } }

    // ─── Search ────────────────────────────────────────────────────────────────

    /**
     * Fetches up to 50 users and filters locally by username (case-insensitive).
     * Firestore doesn't support native full-text search; this approach is
     * acceptable for a small dataset. For production, use Algolia or similar.
     */
    suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        val trimmed = query.trim()
        Log.d(TAG, "searchUsers() query='$trimmed'")

        val snapshot = db.collection(Constants.COLLECTION_USERS)
            .limit(100)
            .get()
            .await()

        Log.d(TAG, "searchUsers() raw docs fetched: ${snapshot.size()}")

        val results = snapshot.documents.mapNotNull { doc ->
            try {
                // Use manual mapping only — avoids ProGuard/no-arg constructor issues
                val uid      = doc.getString("uid")      ?.takeIf { it.isNotBlank() } ?: doc.id
                val username = doc.getString("username") ?: ""
                val email    = doc.getString("email")    ?: ""
                val avatar   = doc.getString("avatarUrl") ?: ""
                val ts       = doc.getTimestamp("createdAt") ?: Timestamp.now()
                Log.v(TAG, "  doc id=${doc.id} uid=$uid username='$username' email='$email'")
                User(uid = uid, username = username, email = email, avatarUrl = avatar, createdAt = ts)
            } catch (e: Exception) {
                Log.e(TAG, "searchUsers() failed to parse doc ${doc.id}: ${e.message}")
                null
            }
        }.filter { user ->
            user.username.contains(trimmed, ignoreCase = true) ||
            user.email.contains(trimmed, ignoreCase = true)
        }

        Log.d(TAG, "searchUsers() results after filter: ${results.size}")
        results
    }.also { it.onFailure { e -> Log.e(TAG, "searchUsers() FAILED: ${e.message}", e) } }

    // ─── Follow ────────────────────────────────────────────────────────────────

    suspend fun followUser(currentUid: String, targetUid: String): Result<Unit> = runCatching {
        require(currentUid.isNotBlank() && targetUid.isNotBlank())
        Log.d(TAG, "followUser() current=$currentUid target=$targetUid")
        db.collection(Constants.COLLECTION_USERS).document(currentUid)
            .collection(Constants.COLLECTION_FOLLOWING).document(targetUid).set(mapOf("timestamp" to Timestamp.now())).await()
        db.collection(Constants.COLLECTION_USERS).document(targetUid)
            .collection(Constants.COLLECTION_FOLLOWERS).document(currentUid).set(mapOf("timestamp" to Timestamp.now())).await()
        val notifRef = db.collection(Constants.COLLECTION_NOTIFICATIONS).document(targetUid)
            .collection(Constants.SUBCOLLECTION_NOTIFICATION_ITEMS).document()
        val notif = mapOf(
            "id" to notifRef.id,
            "type" to Constants.NOTIFICATION_TYPE_FOLLOW,
            "senderId" to currentUid,
            "createdAt" to Timestamp.now(),
            "isRead" to false
        )
        notifRef.set(notif).await()
        Unit
    }.also { it.onFailure { e -> Log.e(TAG, "followUser() FAILED: ${e.message}", e) } }

    suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit> = runCatching {
        require(currentUid.isNotBlank() && targetUid.isNotBlank())
        db.collection(Constants.COLLECTION_USERS).document(currentUid)
            .collection(Constants.COLLECTION_FOLLOWING).document(targetUid).delete().await()
        db.collection(Constants.COLLECTION_USERS).document(targetUid)
            .collection(Constants.COLLECTION_FOLLOWERS).document(currentUid).delete().await()
        Unit
    }

    suspend fun isFollowing(currentUid: String, targetUid: String): Result<Boolean> = runCatching {
        require(currentUid.isNotBlank() && targetUid.isNotBlank())
        val doc = db.collection(Constants.COLLECTION_USERS).document(currentUid)
            .collection(Constants.COLLECTION_FOLLOWING).document(targetUid).get().await()
        doc.exists()
    }

    // ─── Block ─────────────────────────────────────────────────────────────────

    suspend fun blockUser(currentUid: String, targetUid: String): Result<Unit> = runCatching {
        require(currentUid.isNotBlank() && targetUid.isNotBlank())
        db.collection(Constants.COLLECTION_USERS).document(currentUid)
            .collection(Constants.COLLECTION_BLOCKED).document(targetUid).set(mapOf("timestamp" to Timestamp.now())).await()
        unfollowUser(currentUid, targetUid)
        unfollowUser(targetUid, currentUid)
        Unit
    }

    suspend fun unblockUser(currentUid: String, targetUid: String): Result<Unit> = runCatching {
        require(currentUid.isNotBlank() && targetUid.isNotBlank())
        db.collection(Constants.COLLECTION_USERS).document(currentUid)
            .collection(Constants.COLLECTION_BLOCKED).document(targetUid).delete().await()
        Unit
    }

    suspend fun isBlockedByTarget(currentUid: String, targetUid: String): Result<Boolean> = runCatching {
        require(currentUid.isNotBlank() && targetUid.isNotBlank())
        val doc = db.collection(Constants.COLLECTION_USERS).document(targetUid)
            .collection(Constants.COLLECTION_BLOCKED).document(currentUid).get().await()
        doc.exists()
    }

    suspend fun getBlockedUsers(uid: String): Result<List<String>> = runCatching {
        require(uid.isNotBlank())
        val snap = db.collection(Constants.COLLECTION_USERS).document(uid)
            .collection(Constants.COLLECTION_BLOCKED).get().await()
        snap.documents.map { it.id }
    }
}
