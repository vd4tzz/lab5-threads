package com.example.threadslite.data.model

/**
 * Reaction definitions for the app.
 *
 * [REACTION_LIST] is the ordered list of emoji reactions users can choose from.
 * Each [ReactionOption] pairs a display emoji with a string key stored in Firestore.
 *
 * In Firestore, a post's `reactions` field is Map<String, Int>:
 *   { "👍": 5, "❤️": 2, ... }
 *
 * A user's own reaction is stored in reactions/{uid_postId}:
 *   { userId, postId, emoji, createdAt }
 */
data class ReactionOption(
    val emoji: String,   // Unicode emoji displayed in UI
    val key: String      // Key used in Firestore reactions map
)

object Reaction {
    val REACTION_LIST = listOf(
        ReactionOption("👍", "like"),
        ReactionOption("❤️", "love"),
        ReactionOption("😂", "laugh"),
        ReactionOption("😮", "wow"),
        ReactionOption("😢", "sad")
    )

    /** Returns the emoji for a given key, or the key itself as fallback */
    fun emojiForKey(key: String): String =
        REACTION_LIST.firstOrNull { it.key == key }?.emoji ?: key

    /** Returns the key for a given emoji, or the emoji itself as fallback */
    fun keyForEmoji(emoji: String): String =
        REACTION_LIST.firstOrNull { it.emoji == emoji }?.key ?: emoji
}
