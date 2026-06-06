package com.example.threadslite.data.repository

import android.util.Log
import com.example.threadslite.data.model.User
import com.example.threadslite.util.Constants
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles all Firebase Authentication operations and initial user profile creation.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    // ─── Registration ──────────────────────────────────────────────────────────

    /**
     * Creates a new Firebase Auth account and writes the user profile to Firestore.
     *
     * Strategy: Auth account creation and Firestore write are treated separately.
     * If Firestore write fails, the function still returns success with the user
     * (Auth session is valid). Firestore write failure is logged but non-blocking
     * so the user isn't stuck on a loading screen.
     */
    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<User> = runCatching {
        Log.d(TAG, "register() start — email=$email username=$username")

        // ── Input validation ──────────────────────────────────────────────────
        require(email.isNotBlank())    { "Email must not be blank." }
        require(password.isNotBlank()) { "Password must not be blank." }
        require(username.isNotBlank()) { "Username must not be blank." }

        // ── Step 1: Create Firebase Auth account ──────────────────────────────
        Log.d(TAG, "register() step 1: creating Auth account")
        val authResult = auth
            .createUserWithEmailAndPassword(email.trim(), password)
            .await()

        val firebaseUser = authResult.user
            ?: throw IllegalStateException("Firebase Auth returned null user after registration.")

        Log.d(TAG, "register() step 1 OK — uid=${firebaseUser.uid}")

        // ── Step 2: Build domain model ────────────────────────────────────────
        val user = User(
            uid       = firebaseUser.uid,
            username  = username.trim(),
            email     = email.trim(),
            avatarUrl = "",
            createdAt = Timestamp.now()
        )

        // ── Step 3: Persist to Firestore using a plain Map (avoids Kotlin
        //   data-class serialization issues with Firebase Timestamp) ───────────
        Log.d(TAG, "register() step 2: writing Firestore document users/${firebaseUser.uid}")

        val userMap = mapOf(
            "uid"       to user.uid,
            "username"  to user.username,
            "email"     to user.email,
            "avatarUrl" to user.avatarUrl,
            "createdAt" to user.createdAt
        )

        try {
            db.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .set(userMap)
                .await()
            Log.d(TAG, "register() step 2 OK — Firestore write success")
        } catch (e: Exception) {
            // Firestore write failed (e.g. security rules), but Auth account is
            // valid. Log the error and continue — UI should not be stuck loading.
            Log.e(TAG, "register() Firestore write FAILED (non-fatal): ${e.message}", e)
        }

        user   // ← always return the user so ViewModel sets isLoggedIn = true
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    /**
     * Signs in an existing user with email and password.
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> = runCatching {
        Log.d(TAG, "login() start — email=$email")

        require(email.isNotBlank())    { "Email must not be blank." }
        require(password.isNotBlank()) { "Password must not be blank." }

        val authResult = auth
            .signInWithEmailAndPassword(email.trim(), password)
            .await()

        val firebaseUser = authResult.user
            ?: throw IllegalStateException("Firebase Auth returned null user after sign-in.")

        Log.d(TAG, "login() OK — uid=${firebaseUser.uid}")
        firebaseUser
    }.also { result ->
        result.onFailure { Log.e(TAG, "login() FAILED: ${it.message}", it) }
    }

    // ─── Session management ────────────────────────────────────────────────────

    /** Signs out the current user from Firebase Auth. */
    fun logout() {
        Log.d(TAG, "logout()")
        auth.signOut()
    }

    /** Returns the UID of the currently signed-in user, or null if not signed in. */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** Returns the raw [FirebaseUser] object, or null if not signed in. */
    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    /** Returns true if a user is currently signed in. */
    fun isLoggedIn(): Boolean = auth.currentUser != null
}
