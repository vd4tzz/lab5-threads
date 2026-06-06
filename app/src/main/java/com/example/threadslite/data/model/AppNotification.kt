package com.example.threadslite.data.model

import com.google.firebase.Timestamp

/**
 * Represents an in-app notification for a user.
 * Stored at: notifications/{uid}/items/{notificationId}
 *
 * [type] uses constants from [com.example.threadslite.util.Constants]:
 *   NOTIFICATION_TYPE_FOLLOW, NOTIFICATION_TYPE_COMMENT,
 *   NOTIFICATION_TYPE_REACT, NOTIFICATION_TYPE_REPOST
 */
data class AppNotification(
    val id: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val postId: String = "",
    val type: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)
