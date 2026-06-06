package com.example.threadslite.data.model

import com.google.firebase.Timestamp

/**
 * Represents a registered user in the app.
 * All fields have default values to support Firestore's no-arg deserialization.
 */
data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
