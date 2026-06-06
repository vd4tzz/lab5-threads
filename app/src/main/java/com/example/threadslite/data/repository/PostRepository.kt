package com.example.threadslite.data.repository

import android.util.Log
import com.example.threadslite.data.model.Post
import com.example.threadslite.util.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "PostRepository"
    }

    // ─── Feed ──────────────────────────────────────────────────────────────────

    fun observeFeed(): Flow<List<Post>> = callbackFlow {
        Log.d(TAG, "observeFeed() attaching listener")
        val listener = db.collection(Constants.COLLECTION_POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(Constants.FEED_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeFeed() error: ${error.message}", error)
                    trySend(emptyList()); return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { documentToPost(it) } ?: emptyList()
                Log.d(TAG, "observeFeed() ${posts.size} posts")
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    fun observePostsByUser(userId: String): Flow<List<Post>> = callbackFlow {
        // Note: no .orderBy() here to avoid requiring a Firestore composite index.
        // We sort locally after receiving the snapshot.
        val listener = db.collection(Constants.COLLECTION_POSTS)
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observePostsByUser() error: ${error.message}", error)
                    trySend(emptyList()); return@addSnapshotListener
                }
                val posts = snapshot?.documents
                    ?.mapNotNull { documentToPost(it) }
                    ?.sortedByDescending { it.createdAt.seconds }
                    ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    // ─── Create ────────────────────────────────────────────────────────────────

    suspend fun createPost(
        uid: String,
        username: String,
        avatarUrl: String,
        content: String,
        locationUrl: String = "",
        imageUrls: List<String> = emptyList()
    ): Result<Post> = runCatching {
        require(uid.isNotBlank())     { "uid must not be blank." }
        require(content.isNotBlank()) { "Post content must not be blank." }

        Log.d(TAG, "createPost() uid=$uid")
        val docRef = db.collection(Constants.COLLECTION_POSTS).document()

        val post = Post(
            postId              = docRef.id,
            authorId            = uid,
            authorUsername      = username,
            authorAvatarUrl     = avatarUrl,
            content             = content.trim(),
            imageUrls           = imageUrls,
            locationUrl         = locationUrl.trim(),
            createdAt           = Timestamp.now(),
            reactions           = emptyMap(),
            commentsCount       = 0,
            isRepost            = false,
            originalPostId      = "",
            originalAuthorId    = "",
            originalAuthorUsername = "",
            originalContent     = "",
            originalImageUrls   = emptyList()
        )

        docRef.set(postToMap(post)).await()
        Log.d(TAG, "createPost() OK postId=${post.postId}")
        post
    }.also { it.onFailure { e -> Log.e(TAG, "createPost() FAILED: ${e.message}", e) } }

    // ─── Repost ────────────────────────────────────────────────────────────────

    suspend fun repost(
        originalPost: Post,
        uid: String,
        username: String,
        avatarUrl: String,
        content: String
    ): Result<Post> = runCatching {
        require(uid.isNotBlank())     { "uid must not be blank." }
        require(content.isNotBlank()) { "Repost comment must not be blank." }

        Log.d(TAG, "repost() uid=$uid originalPostId=${originalPost.postId}")
        val docRef = db.collection(Constants.COLLECTION_POSTS).document()

        val post = Post(
            postId                 = docRef.id,
            authorId               = uid,
            authorUsername         = username,
            authorAvatarUrl        = avatarUrl,
            content                = content.trim(),
            imageUrls              = emptyList(),
            locationUrl            = "",
            createdAt              = Timestamp.now(),
            reactions              = emptyMap(),
            commentsCount          = 0,
            isRepost               = true,
            originalPostId         = originalPost.postId,
            originalAuthorId       = originalPost.authorId,
            originalAuthorUsername = originalPost.authorUsername,
            originalContent        = originalPost.content,
            originalImageUrls      = originalPost.imageUrls
        )

        docRef.set(postToMap(post)).await()
        Log.d(TAG, "repost() OK postId=${post.postId}")
        post
    }.also { it.onFailure { e -> Log.e(TAG, "repost() FAILED: ${e.message}", e) } }

    // ─── Delete ────────────────────────────────────────────────────────────────

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        require(postId.isNotBlank()) { "postId must not be blank." }
        Log.d(TAG, "deletePost() postId=$postId")
        db.collection(Constants.COLLECTION_POSTS).document(postId).delete().await()
        Log.d(TAG, "deletePost() OK")
        Unit
    }.also { it.onFailure { e -> Log.e(TAG, "deletePost() FAILED: ${e.message}", e) } }

    // ─── Comments ──────────────────────────────────────────────────────────────

    suspend fun addComment(
        postId: String,
        authorId: String,
        authorUsername: String,
        authorAvatarUrl: String,
        content: String
    ): Result<Unit> = runCatching {
        require(postId.isNotBlank())   { "postId must not be blank." }
        require(authorId.isNotBlank()) { "authorId must not be blank." }
        require(content.isNotBlank())  { "Comment content must not be blank." }
        Log.d(TAG, "addComment() postId=$postId authorId=$authorId")

        val postRef    = db.collection(Constants.COLLECTION_POSTS).document(postId)
        val commentRef = postRef.collection(Constants.COLLECTION_COMMENTS).document()

        val commentMap = mapOf(
            "commentId"       to commentRef.id,
            "postId"          to postId,
            "authorId"        to authorId,
            "authorUsername"  to authorUsername,
            "authorAvatarUrl" to authorAvatarUrl,
            "content"         to content.trim(),
            "createdAt"       to com.google.firebase.Timestamp.now()
        )
        commentRef.set(commentMap).await()
        postRef.update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
        Log.d(TAG, "addComment() OK commentId=${commentRef.id}")
        Unit
    }.also { it.onFailure { e -> Log.e(TAG, "addComment() FAILED: ${e.message}", e) } }

    fun observeComments(postId: String): kotlinx.coroutines.flow.Flow<List<com.example.threadslite.data.model.Comment>> = callbackFlow {
        require(postId.isNotBlank())
        val listener = db.collection(Constants.COLLECTION_POSTS).document(postId)
            .collection(Constants.COLLECTION_COMMENTS)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeComments() error: ${err.message}")
                    trySend(emptyList()); return@addSnapshotListener
                }
                val comments = snap?.documents?.mapNotNull { doc ->
                    try {
                        com.example.threadslite.data.model.Comment(
                            commentId       = doc.getString("commentId")?.takeIf { it.isNotBlank() } ?: doc.id,
                            postId          = doc.getString("postId")          ?: postId,
                            authorId        = doc.getString("authorId")        ?: "",
                            authorUsername  = doc.getString("authorUsername")  ?: "",
                            authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                            content         = doc.getString("content")         ?: "",
                            createdAt       = doc.getTimestamp("createdAt")    ?: com.google.firebase.Timestamp.now()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "observeComments() parse error: ${e.message}")
                        null
                    }
                }?.sortedByDescending { it.createdAt.seconds } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }



    /**
     * Toggle reaction for [uid] on [postId].
     * - If no prior reaction: add reaction doc + increment post.reactions[emoji].
     * - If same emoji: remove reaction doc + decrement.
     * - If different emoji: update doc + swap counts.
     * Reaction doc id = "${uid}_${postId}" in collection "reactions".
     */
    suspend fun toggleReaction(postId: String, uid: String, emoji: String): Result<Unit> = runCatching {
        require(postId.isNotBlank() && uid.isNotBlank() && emoji.isNotBlank())
        Log.d(TAG, "toggleReaction() postId=$postId uid=$uid emoji=$emoji")

        val reactionDocId  = "${uid}_${postId}"
        val reactionRef    = db.collection(Constants.COLLECTION_REACTIONS).document(reactionDocId)
        val postRef        = db.collection(Constants.COLLECTION_POSTS).document(postId)

        db.runTransaction { tx ->
            val reactionSnap = tx.get(reactionRef)
            val postSnap     = tx.get(postRef)

            @Suppress("UNCHECKED_CAST")
            val reactionsMap = (postSnap.get("reactions") as? Map<String, Long>)
                ?.mapValues { it.value.toInt() }?.toMutableMap() ?: mutableMapOf()

            if (!reactionSnap.exists()) {
                // New reaction
                tx.set(reactionRef, mapOf("userId" to uid, "postId" to postId, "emoji" to emoji))
                reactionsMap[emoji] = (reactionsMap[emoji] ?: 0) + 1
                tx.update(postRef, "reactions", reactionsMap)
            } else {
                val oldEmoji = reactionSnap.getString("emoji") ?: ""
                if (oldEmoji == emoji) {
                    // Remove reaction
                    tx.delete(reactionRef)
                    val newCount = (reactionsMap[emoji] ?: 1) - 1
                    if (newCount <= 0) reactionsMap.remove(emoji) else reactionsMap[emoji] = newCount
                    tx.update(postRef, "reactions", reactionsMap)
                } else {
                    // Swap emoji
                    tx.update(reactionRef, "emoji", emoji)
                    val oldCount = (reactionsMap[oldEmoji] ?: 1) - 1
                    if (oldCount <= 0) reactionsMap.remove(oldEmoji) else reactionsMap[oldEmoji] = oldCount
                    reactionsMap[emoji] = (reactionsMap[emoji] ?: 0) + 1
                    tx.update(postRef, "reactions", reactionsMap)
                }
            }
        }.await()
        Log.d(TAG, "toggleReaction() OK")
        Unit
    }.also { it.onFailure { e -> Log.e(TAG, "toggleReaction() FAILED: ${e.message}", e) } }

    /**
     * Observe all reactions by [uid] and return a map of postId -> emoji.
     */
    fun observeUserReactions(uid: String): kotlinx.coroutines.flow.Flow<Map<String, String>> = callbackFlow {
        val listener = db.collection(Constants.COLLECTION_REACTIONS)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyMap()); return@addSnapshotListener }
                val map = snap?.documents?.associate { doc ->
                    val postId = doc.getString("postId") ?: ""
                    val emoji  = doc.getString("emoji")  ?: ""
                    postId to emoji
                }?.filter { it.key.isNotBlank() && it.value.isNotBlank() } ?: emptyMap()
                trySend(map)
            }
        awaitClose { listener.remove() }
    }



    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun documentToPost(doc: DocumentSnapshot): Post? {
        return try {
            @Suppress("UNCHECKED_CAST")
            Post(
                postId                 = doc.getString("postId")?.takeIf { it.isNotBlank() } ?: doc.id,
                authorId               = doc.getString("authorId")               ?: "",
                authorUsername         = doc.getString("authorUsername")         ?: "",
                authorAvatarUrl        = doc.getString("authorAvatarUrl")        ?: "",
                content                = doc.getString("content")                ?: "",
                imageUrls              = (doc.get("imageUrls") as? List<String>) ?: emptyList(),
                locationUrl            = doc.getString("locationUrl")            ?: "",
                createdAt              = doc.getTimestamp("createdAt")           ?: Timestamp.now(),
                reactions              = run {
                    val raw = doc.get("reactions")
                    when (raw) {
                        is Map<*, *> -> raw.entries.associate { (k, v) ->
                            k.toString() to when (v) {
                                is Long   -> v.toInt()
                                is Int    -> v
                                is Double -> v.toInt()
                                else      -> 0
                            }
                        }
                        else -> emptyMap()
                    }
                },
                commentsCount          = (doc.getLong("commentsCount") ?: 0L).toInt(),
                isRepost               = doc.getBoolean("isRepost")              ?: false,
                originalPostId         = doc.getString("originalPostId")         ?: "",
                originalAuthorId       = doc.getString("originalAuthorId")       ?: "",
                originalAuthorUsername = doc.getString("originalAuthorUsername") ?: "",
                originalContent        = doc.getString("originalContent")        ?: "",
                originalImageUrls      = (doc.get("originalImageUrls") as? List<String>) ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "documentToPost() failed for ${doc.id}: ${e.message}")
            null
        }
    }


    private fun postToMap(post: Post): Map<String, Any?> = mapOf(
        "postId"                 to post.postId,
        "authorId"               to post.authorId,
        "authorUsername"         to post.authorUsername,
        "authorAvatarUrl"        to post.authorAvatarUrl,
        "content"                to post.content,
        "imageUrls"              to post.imageUrls,
        "locationUrl"            to post.locationUrl,
        "createdAt"              to post.createdAt,
        "reactions"              to post.reactions,
        "commentsCount"          to post.commentsCount,
        "isRepost"               to post.isRepost,
        "originalPostId"         to post.originalPostId,
        "originalAuthorId"       to post.originalAuthorId,
        "originalAuthorUsername" to post.originalAuthorUsername,
        "originalContent"        to post.originalContent,
        "originalImageUrls"      to post.originalImageUrls
    )
}
