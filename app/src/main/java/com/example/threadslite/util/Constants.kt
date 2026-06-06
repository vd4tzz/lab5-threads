package com.example.threadslite.util

/**
 * Firestore collection name constants — single source of truth.
 * All repositories must reference these instead of hardcoding strings.
 */
object Constants {
    const val COLLECTION_USERS         = "users"
    const val COLLECTION_POSTS         = "posts"
    const val COLLECTION_REACTIONS     = "reactions"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_COMMENTS      = "comments"
    const val COLLECTION_FOLLOWING     = "following"
    const val COLLECTION_FOLLOWERS     = "followers"
    const val COLLECTION_BLOCKED       = "blocked"

    // Firestore sub-collection name for notifications
    const val SUBCOLLECTION_NOTIFICATION_ITEMS = "items"

    // Firebase Storage paths
    const val STORAGE_AVATARS    = "avatars"
    const val STORAGE_POST_IMAGES = "post_images"

    // Notification types
    const val NOTIFICATION_TYPE_FOLLOW  = "follow"
    const val NOTIFICATION_TYPE_COMMENT = "comment"
    const val NOTIFICATION_TYPE_REACT   = "react"
    const val NOTIFICATION_TYPE_REPOST  = "repost"

    // Feed limit
    const val FEED_LIMIT = 50L
}
