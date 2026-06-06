package com.example.threadslite.data.model

import com.google.firebase.Timestamp

/**
 * Represents a post in the feed.
 *
 * For a regular post: [isRepost] = false, original* fields are empty.
 * For a repost: [isRepost] = true and original* fields carry the source post's data.
 *
 * [reactions] maps emoji string → count (e.g. "👍" → 3).
 * All fields default to empty/zero for safe Firestore deserialization.
 */
data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val locationUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val reactions: Map<String, Int> = emptyMap(),
    val commentsCount: Int = 0,

    // Repost metadata
    val isRepost: Boolean = false,
    val originalPostId: String = "",
    val originalAuthorId: String = "",
    val originalAuthorUsername: String = "",
    val originalContent: String = "",
    val originalImageUrls: List<String> = emptyList()
)
