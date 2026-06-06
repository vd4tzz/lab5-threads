package com.example.threadslite.data.model

import com.google.firebase.Timestamp

/**
 * Represents a comment on a post.
 * Stored at: posts/{postId}/comments/{commentId}
 */
data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
