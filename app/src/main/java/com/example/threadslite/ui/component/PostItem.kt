package com.example.threadslite.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.threadslite.data.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val REACTION_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "😡")

@Composable
fun PostCard(
    data: Post,
    currentUserId: String? = null,
    currentUserReaction: String? = null,
    onDeleteClick: ((Post) -> Unit)? = null,
    onRepostClick: ((Post) -> Unit)? = null,
    onAuthorClick: ((Post) -> Unit)? = null,
    onReact: ((Post, String) -> Unit)? = null,
    onOpenComments: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context      = LocalContext.current
    val relativeTime = remember(data.createdAt) { formatRelativeTime(data.createdAt.toDate()) }

    var showReactionPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: avatar column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarImage(
                    avatarUrl = data.authorAvatarUrl,
                    username  = data.authorUsername,
                    size      = 40,
                    modifier  = if (onAuthorClick != null)
                        Modifier.clickable { onAuthorClick(data) }
                    else Modifier
                )
            }

            // Right: content column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header: name + timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text       = "@${data.authorUsername.ifBlank { "unknown" }}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            color      = Color.Black,
                            modifier   = if (onAuthorClick != null)
                                Modifier.clickable { onAuthorClick(data) }
                            else Modifier
                        )
                        if (data.isRepost) {
                            Text(
                                text     = "· repost",
                                fontSize = 12.sp,
                                color    = Color(0xFFAAAAAA)
                            )
                        }
                    }
                    Text(
                        text     = relativeTime,
                        fontSize = 12.sp,
                        color    = Color(0xFFAAAAAA)
                    )
                }

                // Content text
                if (data.content.isNotBlank()) {
                    Text(
                        text       = data.content,
                        fontSize   = 15.sp,
                        lineHeight = 22.sp,
                        color      = Color(0xFF1A1A1A),
                        maxLines   = 10,
                        overflow   = TextOverflow.Ellipsis
                    )
                }

                // Images
                if (data.imageUrls.isNotEmpty()) {
                    ImageGrid(imageUrls = data.imageUrls)
                }

                // Location link
                if (data.locationUrl.isNotBlank()) {
                    TextButton(
                        onClick          = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(data.locationUrl))
                                )
                            }
                        },
                        contentPadding   = PaddingValues(0.dp)
                    ) {
                        Text("📍 View location", fontSize = 13.sp, color = Color(0xFF1565C0))
                    }
                }

                // Original post preview (repost only)
                if (data.isRepost) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF7F7F7))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = "@${data.originalAuthorUsername.ifBlank { "unknown" }}",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF757575)
                        )
                        if (data.originalContent.isNotBlank()) {
                            Text(
                                text     = data.originalContent,
                                fontSize = 13.sp,
                                color    = Color(0xFF555555),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (data.originalImageUrls.isNotEmpty()) {
                            ImageGrid(imageUrls = data.originalImageUrls)
                        }
                    }
                }

                // Reaction picker row
                AnimatedVisibility(visible = showReactionPicker) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        REACTION_EMOJIS.forEach { emoji ->
                            val isSelected = currentUserReaction == emoji
                            Text(
                                text     = emoji,
                                fontSize = if (isSelected) 22.sp else 18.sp,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Color(0xFFEEEEEE) else Color.Transparent,
                                        RoundedCornerShape(50)
                                    )
                                    .padding(6.dp)
                                    .clickable {
                                        onReact?.invoke(data, emoji)
                                        showReactionPicker = false
                                    }
                            )
                        }
                    }
                }

                // Action bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val totalReactions = data.reactions.values.sumOf { it }

                    // React button
                    Row(
                        modifier = Modifier.clickable {
                            if (onReact != null) showReactionPicker = !showReactionPicker
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when {
                            currentUserReaction != null -> {
                                Text(currentUserReaction, fontSize = 16.sp)
                                if (totalReactions > 0)
                                    Text("$totalReactions", fontSize = 13.sp, color = Color(0xFF757575))
                            }
                            totalReactions > 0 -> {
                                val topEmoji = data.reactions.maxByOrNull { it.value }?.key ?: "👍"
                                Text(topEmoji, fontSize = 16.sp)
                                Text("$totalReactions", fontSize = 13.sp, color = Color(0xFF757575))
                            }
                            else -> Text("♡", fontSize = 18.sp, color = Color(0xFFAAAAAA))
                        }
                    }

                    // Comment button
                    Row(
                        modifier = if (onOpenComments != null)
                            Modifier.clickable { onOpenComments(data) }
                        else Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("💬", fontSize = 16.sp)
                        if (data.commentsCount > 0)
                            Text("${data.commentsCount}", fontSize = 13.sp, color = Color(0xFF757575))
                    }

                    Spacer(Modifier.weight(1f))

                    // Repost
                    onRepostClick?.let { callback ->
                        Text(
                            "🔁",
                            fontSize = 16.sp,
                            modifier = Modifier.clickable { callback(data) }
                        )
                    }

                    // Delete
                    if (currentUserId != null && currentUserId == data.authorId) {
                        onDeleteClick?.let { callback ->
                            Text(
                                "🗑",
                                fontSize = 16.sp,
                                modifier = Modifier.clickable { callback(data) }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
    }
}

@Composable
fun PostItem(
    data: Post,
    currentUserId: String? = null,
    onDeleteClick: ((Post) -> Unit)? = null,
    onRepostClick: ((Post) -> Unit)? = null,
    onOpenComments: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) = PostCard(
    data            = data,
    currentUserId   = currentUserId,
    onDeleteClick   = onDeleteClick,
    onRepostClick   = onRepostClick,
    onOpenComments  = onOpenComments,
    modifier        = modifier
)

private fun formatRelativeTime(date: Date): String {
    val diffMs = System.currentTimeMillis() - date.time
    val secs   = TimeUnit.MILLISECONDS.toSeconds(diffMs)
    val mins   = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours  = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days   = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        secs  < 60 -> "now"
        mins  < 60 -> "${mins}m"
        hours < 24 -> "${hours}h"
        days  <  7 -> "${days}d"
        else       -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }
}
