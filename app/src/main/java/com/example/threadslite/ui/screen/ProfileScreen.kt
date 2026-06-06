package com.example.threadslite.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.threadslite.ui.component.PostCard
import com.example.threadslite.util.UiState

@Composable
fun ProfileScreen(
    vm: MainViewModel,
    userId: String,
    currentUserId: String,
    onBack: () -> Unit
) {
    val selectedUser          by vm.selectedUser.collectAsStateWithLifecycle()
    val userPosts             by vm.selectedUserPosts.collectAsStateWithLifecycle()
    val currentUser           by vm.currentUser.collectAsStateWithLifecycle()
    val profileState          by vm.profileState.collectAsStateWithLifecycle()
    val isFollowing           by vm.isFollowingSelectedUser.collectAsStateWithLifecycle()
    val isBlockedByMe         by vm.isSelectedUserBlockedByMe.collectAsStateWithLifecycle()
    val isBlockedByTarget     by vm.isCurrentUserBlockedBySelectedUser.collectAsStateWithLifecycle()
    val profileBlockMessage   by vm.profileBlockMessage.collectAsStateWithLifecycle()
    val isBlocked             by vm.isSelectedUserBlocked.collectAsStateWithLifecycle()

    val isOwner     = userId == currentUserId
    val displayUser = if (isOwner) currentUser else selectedUser

    var newUsername      by remember { mutableStateOf("") }
    var profileError     by remember { mutableStateOf<String?>(null) }
    var postToDelete     by remember { mutableStateOf<Post?>(null) }
    var postToRepost     by remember { mutableStateOf<Post?>(null) }
    var repostContent    by remember { mutableStateOf("") }
    var showBlockConfirm by remember { mutableStateOf(false) }

    val isSaving = profileState is UiState.Loading

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.updateAvatarUrl(uri)
    }

    LaunchedEffect(userId) {
        if (!isOwner) vm.openProfileEnhanced(userId)
        else vm.observePostsByUser(userId)
    }

    LaunchedEffect(profileState) {
        when (val s = profileState) {
            is UiState.Success -> { newUsername = ""; profileError = null; vm.resetProfileState() }
            is UiState.Error   -> { profileError = s.message; vm.resetProfileState() }
            else               -> {}
        }
    }

    postToDelete?.let { post ->
        ConfirmDialog(
            title       = "Delete post",
            message     = "Delete this post permanently?",
            confirmText = "Delete",
            onConfirm   = { vm.deletePost(post) },
            onDismiss   = { postToDelete = null }
        )
    }

    if (showBlockConfirm && displayUser != null) {
        ConfirmDialog(
            title       = "Block @${displayUser!!.username}?",
            message     = "You will no longer see this user's posts or profile.",
            confirmText = "Block",
            dismissText = "Cancel",
            onConfirm   = { vm.blockUser(userId); showBlockConfirm = false },
            onDismiss   = { showBlockConfirm = false }
        )
    }

    postToRepost?.let { post ->
        AlertDialog(
            onDismissRequest = { postToRepost = null },
            containerColor   = Color.White,
            title  = { Text("Repost", fontWeight = FontWeight.Bold) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("↩ @${post.authorUsername}: ${post.content}", fontSize = 12.sp, color = Color(0xFF9E9E9E), maxLines = 2)
                    OutlinedTextField(
                        value         = repostContent,
                        onValueChange = { repostContent = it },
                        placeholder   = { Text("Add comment...", color = Color(0xFFBDBDBD)) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        minLines      = 2,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color.Black,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor          = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.repost(post, repostContent); postToRepost = null }) {
                    Text("Repost", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToRepost = null; repostContent = "" }) {
                    Text("Cancel", color = Color(0xFF9E9E9E))
                }
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
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Color(0xFF555555), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text       = if (isOwner) "My Profile" else "Profile",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.Black
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(72.dp))
        }

        HorizontalDivider(color = Color(0xFFF0F0F0))

        if (!isOwner && isBlockedByTarget) {
            EmptyState(message = profileBlockMessage ?: "You cannot view this profile.")
            return@Column
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Profile header
            item {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarImage(
                        avatarUrl = displayUser?.avatarUrl ?: "",
                        username  = displayUser?.username ?: "",
                        size      = 88
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = "@${displayUser?.username ?: "Loading…"}",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.Black
                    )
                    if (!displayUser?.email.isNullOrBlank()) {
                        Text(
                            text     = displayUser!!.email,
                            fontSize = 13.sp,
                            color    = Color(0xFFAAAAAA)
                        )
                    }

                    if (!isOwner && displayUser != null) {
                        Spacer(Modifier.height(8.dp))

                        if (isBlockedByMe) {
                            Text(
                                profileBlockMessage ?: "You have blocked this user.",
                                fontSize = 13.sp,
                                color    = Color(0xFFAAAAAA),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Button(
                                onClick  = { vm.unblockUser(userId) },
                                shape    = RoundedCornerShape(50.dp),
                                modifier = Modifier.fillMaxWidth(0.7f),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF0F0F0),
                                    contentColor   = Color.Black
                                )
                            ) { Text("Unblock") }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier              = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Button(
                                    onClick  = { vm.toggleFollow(userId) },
                                    shape    = RoundedCornerShape(50.dp),
                                    modifier = Modifier.weight(1f),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) Color.White else Color.Black,
                                        contentColor   = if (isFollowing) Color.Black else Color.White
                                    ),
                                    border = if (isFollowing)
                                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
                                    else null
                                ) {
                                    Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick  = { showBlockConfirm = true },
                                    shape    = RoundedCornerShape(50.dp),
                                    modifier = Modifier.weight(1f),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF5F5F5),
                                        contentColor   = Color(0xFFD32F2F)
                                    )
                                ) { Text("Block") }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))
            }

            // Edit profile (owner only)
            if (isOwner) {
                item {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Edit profile",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            color      = Color(0xFFAAAAAA),
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Profile photo", fontSize = 15.sp, color = Color.Black)
                            TextButton(
                                onClick = {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("Change", color = Color.Black, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedTextField(
                            value         = newUsername,
                            onValueChange = { newUsername = it },
                            placeholder   = { Text("New username", color = Color(0xFFBBBBBB)) },
                            enabled       = !isSaving,
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(10.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Color.Black,
                                unfocusedBorderColor = Color(0xFFEEEEEE),
                                cursorColor          = Color.Black
                            )
                        )

                        profileError?.let {
                            Text(it, color = Color(0xFFB00020), fontSize = 12.sp)
                        }

                        Button(
                            onClick  = { if (newUsername.isNotBlank()) vm.updateUsername(newUsername) },
                            enabled  = newUsername.isNotBlank() && !isSaving,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(50.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = Color.Black,
                                contentColor           = Color.White,
                                disabledContainerColor = Color(0xFFDDDDDD)
                            )
                        ) {
                            if (isSaving)
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else
                                Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }

            // Posts header
            item {
                Text(
                    text     = "Posts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color    = Color(0xFFAAAAAA),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            if (userPosts.isEmpty()) {
                item { EmptyState("No posts yet.") }
            } else {
                items(userPosts, key = { it.postId }) { post ->
                    PostCard(
                        data          = post,
                        currentUserId = currentUserId,
                        onDeleteClick = { postToDelete = it },
                        onRepostClick = { postToRepost = it; repostContent = "" }
                    )
                }
            }
        }
    }
}
