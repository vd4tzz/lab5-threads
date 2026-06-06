package com.example.threadslite.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threadslite.data.model.Post
import com.example.threadslite.ui.MainViewModel
import com.example.threadslite.ui.component.AvatarImage
import com.example.threadslite.ui.component.ConfirmDialog
import com.example.threadslite.ui.component.EmptyState
import com.example.threadslite.ui.component.ImageGrid
import com.example.threadslite.ui.component.PostCard
import com.example.threadslite.util.UiState

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNavigate: (String) -> Unit = {},
    onOpenProfile: (String) -> Unit = {}
) {
    val currentUser    by vm.currentUser.collectAsStateWithLifecycle()
    val posts          by vm.posts.collectAsStateWithLifecycle()
    val postState      by vm.postState.collectAsStateWithLifecycle()
    val userReactions  by vm.userReactions.collectAsStateWithLifecycle()
    val currentUserId  = currentUser?.uid

    var showCreateDialog    by remember { mutableStateOf(false) }
    var content             by remember { mutableStateOf("") }
    var locationUrl         by remember { mutableStateOf("") }
    var imageUris           by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showLocation        by remember { mutableStateOf(false) }
    var postError           by remember { mutableStateOf<String?>(null) }
    var postToDelete        by remember { mutableStateOf<Post?>(null) }
    var postToRepost        by remember { mutableStateOf<Post?>(null) }
    var repostContent       by remember { mutableStateOf("") }
    var selectedCommentPost by remember { mutableStateOf<Post?>(null) }

    val comments     by vm.selectedPostComments.collectAsStateWithLifecycle()
    val commentState by vm.commentState.collectAsStateWithLifecycle()
    val isPosting    = postState is UiState.Loading

    LaunchedEffect(Unit) {
        vm.observeFeed()
        if (currentUser == null) vm.loadCurrentUser()
    }
    LaunchedEffect(currentUserId) {
        if (!currentUserId.isNullOrBlank()) vm.startObservingReactions(currentUserId)
    }
    LaunchedEffect(postState) {
        when (val s = postState) {
            is UiState.Success -> {
                content = ""; locationUrl = ""; imageUris = emptyList()
                showLocation = false; showCreateDialog = false; postError = null
                vm.resetPostState()
            }
            is UiState.Error -> { postError = s.message; vm.resetPostState() }
            else -> {}
        }
    }

    postToDelete?.let { post ->
        ConfirmDialog(
            title = "Delete post", message = "Are you sure? This cannot be undone.",
            confirmText = "Delete", dismissText = "Cancel",
            onConfirm = { vm.deletePost(post) }, onDismiss = { postToDelete = null }
        )
    }

    postToRepost?.let { post ->
        RepostDialog(
            originalPost    = post,
            repostContent   = repostContent,
            isPosting       = isPosting,
            onContentChange = { repostContent = it },
            onConfirm       = { vm.repost(post, repostContent); postToRepost = null },
            onDismiss       = { postToRepost = null; repostContent = "" }
        )
    }

    selectedCommentPost?.let { post ->
        CommentDialog(
            post         = post,
            comments     = comments,
            commentState = commentState,
            onAddComment = { text -> vm.addComment(post, text) },
            onDismiss    = {
                selectedCommentPost = null
                vm.clearComments()
                vm.resetCommentState()
            }
        )
    }

    if (showCreateDialog) {
        CreatePostDialog(
            currentUsername  = currentUser?.username ?: "",
            currentAvatarUrl = currentUser?.avatarUrl ?: "",
            content          = content,
            locationUrl      = locationUrl,
            imageUris        = imageUris,
            showLocation     = showLocation,
            isPosting        = isPosting,
            errorMsg         = postError,
            onContentChange  = { content = it },
            onLocationChange = { locationUrl = it },
            onToggleLocation = { showLocation = !showLocation },
            onImagesSelected = { uris -> imageUris = uris },
            onPost           = { vm.createPost(content, locationUrl, imageUris) },
            onDismiss        = {
                showCreateDialog = false
                content = ""; locationUrl = ""; imageUris = emptyList()
                showLocation = false; postError = null
                vm.resetPostState()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = "Threads Lite",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                color      = Color.Black
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = { onNavigate("search") }) {
                    Text("Search", fontSize = 13.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onNavigate("profile") }) {
                    Text("Profile", fontSize = 13.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { vm.logout() }) {
                    Text("Logout", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF0F0F0))

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                // Composer prompt
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateDialog = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentUser?.username?.isNotBlank() == true) {
                        AvatarImage(
                            avatarUrl = currentUser?.avatarUrl ?: "",
                            username  = currentUser?.username ?: "",
                            size      = 40
                        )
                    } else {
                        Spacer(Modifier.size(40.dp))
                    }
                    Column {
                        Text(
                            text     = "What's on your mind?",
                            fontSize = 15.sp,
                            color    = Color(0xFFCCCCCC)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFF0F0F0))
            }

            if (posts.isEmpty()) {
                item {
                    EmptyState("No posts yet.\nBe the first to post!")
                }
            } else {
                items(posts, key = { it.postId }) { post ->
                    PostCard(
                        data                = post,
                        currentUserId       = currentUserId,
                        currentUserReaction = userReactions[post.postId],
                        onDeleteClick       = { postToDelete = it },
                        onRepostClick       = { postToRepost = it; repostContent = "" },
                        onAuthorClick       = { onOpenProfile(post.authorId) },
                        onReact             = { p, emoji -> vm.react(p, emoji) },
                        onOpenComments      = { p ->
                            selectedCommentPost = p
                            vm.observeComments(p.postId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePostDialog(
    currentUsername: String,
    currentAvatarUrl: String,
    content: String,
    locationUrl: String,
    imageUris: List<Uri>,
    showLocation: Boolean,
    isPosting: Boolean,
    errorMsg: String?,
    onContentChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onToggleLocation: () -> Unit,
    onImagesSelected: (List<Uri>) -> Unit,
    onPost: () -> Unit,
    onDismiss: () -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris -> onImagesSelected(uris) }

    AlertDialog(
        onDismissRequest = { if (!isPosting) onDismiss() },
        containerColor   = Color.White,
        title = {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("New post", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                TextButton(onClick = onDismiss, enabled = !isPosting) {
                    Text("✕", fontSize = 16.sp, color = Color(0xFF9E9E9E))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentUsername.isNotBlank()) {
                        AvatarImage(avatarUrl = currentAvatarUrl, username = currentUsername, size = 36)
                        Text("@$currentUsername", fontWeight = FontWeight.SemiBold, color = Color.Black)
                    } else {
                        Text("Loading...", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                    }
                }

                OutlinedTextField(
                    value         = content,
                    onValueChange = onContentChange,
                    placeholder   = { Text("What's on your mind?", color = Color(0xFFBDBDBD)) },
                    enabled       = !isPosting,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp),
                    minLines      = 3,
                    maxLines      = 8,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.Black,
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        cursorColor          = Color.Black
                    )
                )

                if (showLocation) {
                    OutlinedTextField(
                        value         = locationUrl,
                        onValueChange = onLocationChange,
                        placeholder   = { Text("https://maps.google.com/...", color = Color(0xFFBDBDBD)) },
                        label         = { Text("📍 Maps link") },
                        enabled       = !isPosting,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color.Black,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor          = Color.Black
                        )
                    )
                }

                if (imageUris.isNotEmpty()) {
                    ImageGrid(imageUris = imageUris)
                    if (imageUris.size == 1)
                        Text("⚠ Select at least 2 images", fontSize = 11.sp, color = Color(0xFFD32F2F))
                }

                if (errorMsg != null)
                    Text(errorMsg, fontSize = 12.sp, color = Color(0xFFB00020))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick  = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled  = !isPosting
                    ) { Text("📷 Photos", color = Color(0xFF555555), fontSize = 13.sp) }

                    TextButton(onClick = onToggleLocation, enabled = !isPosting) {
                        Text(
                            if (showLocation) "📍 Hide" else "📍 Location",
                            color    = if (showLocation) Color(0xFF1565C0) else Color(0xFF555555),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onPost,
                enabled  = (content.isNotBlank() || imageUris.size >= 2) && !isPosting && imageUris.size != 1,
                shape    = RoundedCornerShape(50.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color.Black,
                    contentColor           = Color.White,
                    disabledContainerColor = Color(0xFFDDDDDD)
                )
            ) {
                if (isPosting)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else
                    Text("Post", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun RepostDialog(
    originalPost: Post,
    repostContent: String,
    isPosting: Boolean,
    onContentChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        title = {
            Column {
                Text("Repost", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.Black)
                Text("Add your thoughts", fontSize = 12.sp, color = Color(0xFF9E9E9E))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = repostContent,
                    onValueChange = onContentChange,
                    placeholder   = { Text("Add your thoughts...", color = Color(0xFFBDBDBD)) },
                    enabled       = !isPosting,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp),
                    minLines      = 2,
                    maxLines      = 5,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.Black,
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        cursorColor          = Color.Black
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F7F7), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "↩ @${originalPost.authorUsername}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = Color(0xFF555555)
                    )
                    Text(
                        originalPost.content,
                        fontSize = 13.sp,
                        color    = Color(0xFF777777),
                        maxLines = 4
                    )
                    if (originalPost.imageUrls.isNotEmpty())
                        Text("📷 ${originalPost.imageUrls.size} image(s)", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = repostContent.isNotBlank() && !isPosting,
                shape   = RoundedCornerShape(50.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor         = Color.Black,
                    contentColor           = Color.White,
                    disabledContainerColor = Color(0xFFDDDDDD)
                )
            ) {
                if (isPosting)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else
                    Text("Repost", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF9E9E9E)) }
        }
    )
}

@Composable
private fun CommentDialog(
    post: Post,
    comments: List<com.example.threadslite.data.model.Comment>,
    commentState: com.example.threadslite.util.UiState<Unit>,
    onAddComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val isSending   = commentState is UiState.Loading

    LaunchedEffect(commentState) {
        if (commentState is UiState.Success) commentText = ""
    }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        containerColor   = Color.White,
        title = {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Comments", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                TextButton(onClick = onDismiss, enabled = !isSending) {
                    Text("✕", fontSize = 16.sp, color = Color(0xFF9E9E9E))
                }
            }
        },
        text = {
            Column(
                modifier            = Modifier.fillMaxWidth().height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Post preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F7F7), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("@${post.authorUsername}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF555555))
                    if (post.content.isNotBlank())
                        Text(post.content, fontSize = 13.sp, color = Color(0xFF777777), maxLines = 3)
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                if (comments.isEmpty()) {
                    androidx.compose.foundation.layout.Box(
                        modifier         = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No comments yet. Be the first!", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(comments, key = { it.commentId }) { comment ->
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier         = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        comment.authorUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color(0xFF555555)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("@${comment.authorUsername}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                    Text(comment.content, fontSize = 13.sp, color = Color(0xFF444444))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = commentText,
                        onValueChange = { commentText = it },
                        placeholder   = { Text("Write a comment...", color = Color(0xFFBDBDBD)) },
                        enabled       = !isSending,
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        shape         = RoundedCornerShape(50.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color.Black,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor          = Color.Black
                        )
                    )
                    Button(
                        onClick  = { onAddComment(commentText) },
                        enabled  = commentText.isNotBlank() && !isSending,
                        shape    = RoundedCornerShape(50.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Color.Black,
                            contentColor           = Color.White,
                            disabledContainerColor = Color(0xFFDDDDDD)
                        )
                    ) {
                        if (isSending)
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        else
                            Text("Send", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
