package com.example.threadslite.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.threadslite.data.model.Post
import com.example.threadslite.data.model.User
import com.example.threadslite.data.repository.AuthRepository
import com.example.threadslite.data.repository.PostRepository
import com.example.threadslite.data.repository.StorageRepository
import com.example.threadslite.data.repository.UserRepository
import com.example.threadslite.util.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
) : ViewModel() {

    companion object { private const val TAG = "MainViewModel" }

    // ─── Auth ──────────────────────────────────────────────────────────────────

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(authRepository.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val authState: StateFlow<UiState<Unit>> = _authState.asStateFlow()

    // ─── Feed ──────────────────────────────────────────────────────────────────

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _postState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val postState: StateFlow<UiState<Unit>> = _postState.asStateFlow()

    private var feedJob: Job? = null
    private var blockedUsers = emptyList<String>()

    // ─── Profile (selected user) ───────────────────────────────────────────────

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    private val _selectedUserPosts = MutableStateFlow<List<Post>>(emptyList())
    val selectedUserPosts: StateFlow<List<Post>> = _selectedUserPosts.asStateFlow()

    private val _profileState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val profileState: StateFlow<UiState<Unit>> = _profileState.asStateFlow()

    private var userPostsJob: Job? = null

    // ─── Interactions ──────────────────────────────────────────────────────────

    private val _isFollowingSelectedUser = MutableStateFlow(false)
    val isFollowingSelectedUser = _isFollowingSelectedUser.asStateFlow()

    private val _isSelectedUserBlocked = MutableStateFlow(false)
    val isSelectedUserBlocked = _isSelectedUserBlocked.asStateFlow()

    private val _amIBlocked = MutableStateFlow(false)
    val amIBlocked = _amIBlocked.asStateFlow()

    // ─── Search ────────────────────────────────────────────────────────────────

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _searchState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val searchState: StateFlow<UiState<Unit>> = _searchState.asStateFlow()

    // ─── Reactions ─────────────────────────────────────────────────────────────

    /** postId → emoji chosen by current user (real-time from Firestore) */
    private val _userReactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val userReactions: StateFlow<Map<String, String>> = _userReactions.asStateFlow()

    private var reactionsJob: Job? = null

    // ─── Recent searches (in-memory) ──────────────────────────────────────────

    private val _recentSearches = MutableStateFlow<List<User>>(emptyList())
    val recentSearches: StateFlow<List<User>> = _recentSearches.asStateFlow()

    // ─── Comments ──────────────────────────────────────────────────────────────

    private val _selectedPostComments = MutableStateFlow<List<com.example.threadslite.data.model.Comment>>(emptyList())
    val selectedPostComments: StateFlow<List<com.example.threadslite.data.model.Comment>> = _selectedPostComments.asStateFlow()

    private val _commentState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val commentState: StateFlow<UiState<Unit>> = _commentState.asStateFlow()

    private var commentsJob: Job? = null

    // ─── Block (enhanced) ──────────────────────────────────────────────────────

    private val _blockedUserIds = MutableStateFlow<List<String>>(emptyList())
    val blockedUserIds: StateFlow<List<String>> = _blockedUserIds.asStateFlow()

    private val _isSelectedUserBlockedByMe = MutableStateFlow(false)
    val isSelectedUserBlockedByMe: StateFlow<Boolean> = _isSelectedUserBlockedByMe.asStateFlow()

    private val _isCurrentUserBlockedBySelectedUser = MutableStateFlow(false)
    val isCurrentUserBlockedBySelectedUser: StateFlow<Boolean> = _isCurrentUserBlockedBySelectedUser.asStateFlow()

    private val _profileBlockMessage = MutableStateFlow<String?>(null)
    val profileBlockMessage: StateFlow<String?> = _profileBlockMessage.asStateFlow()

    // ─── Init ──────────────────────────────────────────────────────────────────

    init {
        if (authRepository.isLoggedIn()) loadCurrentUser()
    }


    // ─── Auth operations ───────────────────────────────────────────────────────

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            authRepository.register(email, password, username)
                .onSuccess { user ->
                    _currentUser.value = user
                    _isLoggedIn.value  = true
                    _authState.value   = UiState.Success(Unit)
                }
                .onFailure { ex ->
                    _isLoggedIn.value = false
                    _authState.value  = UiState.Error(ex.message ?: "Registration failed.")
                }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            authRepository.login(email, password)
                .onSuccess { firebaseUser ->
                    userRepository.getUser(firebaseUser.uid).onSuccess { _currentUser.value = it }
                    _isLoggedIn.value = true
                    _authState.value  = UiState.Success(Unit)
                    loadCurrentUser() // Load block list and refresh feed
                }
                .onFailure { ex ->
                    _isLoggedIn.value = false
                    _authState.value  = UiState.Error(ex.message ?: "Login failed.")
                }
        }
    }

    fun logout() {
        feedJob?.cancel();   feedJob = null
        userPostsJob?.cancel(); userPostsJob = null
        authRepository.logout()
        _currentUser.value    = null
        _isLoggedIn.value     = false
        _posts.value          = emptyList()
        _selectedUser.value   = null
        _selectedUserPosts.value = emptyList()
        _searchResults.value  = emptyList()
        _authState.value      = UiState.Idle
        _postState.value      = UiState.Idle
        _profileState.value   = UiState.Idle
        blockedUsers          = emptyList()
    }

    fun resetAuthState()    { _authState.value    = UiState.Idle }
    fun resetPostState()    { _postState.value    = UiState.Idle }
    fun resetProfileState() { _profileState.value = UiState.Idle }

    // ─── Current user profile ──────────────────────────────────────────────────

    fun loadCurrentUser() {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            userRepository.getBlockedUsers(uid).onSuccess { blockedUsers = it }
            userRepository.getUser(uid).onSuccess { _currentUser.value = it }
            // Refresh feed if active
            if (feedJob?.isActive == true) {
                feedJob?.cancel()
                observeFeed()
            }
            // Start watching current user's reactions
            startObservingReactions(uid)
        }
    }

    fun updateUsername(newUsername: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            userRepository.updateUsername(uid, newUsername)
                .onSuccess { loadCurrentUser(); _profileState.value = UiState.Success(Unit) }
                .onFailure { _profileState.value = UiState.Error(it.message ?: "Update failed.") }
        }
    }

    fun updateAvatarUrl(uri: Uri) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            storageRepository.uploadAvatar(uid, uri)
                .onSuccess { url ->
                    userRepository.updateAvatar(uid, url)
                        .onSuccess { loadCurrentUser(); _profileState.value = UiState.Success(Unit) }
                        .onFailure { e -> _profileState.value = UiState.Error("Firestore update failed: ${e.message}") }
                }
                .onFailure { e ->
                    _profileState.value = UiState.Error("Image upload failed: ${e.message ?: e.javaClass.simpleName}")
                }
        }
    }

    // ─── Selected user profile & Interactions ──────────────────────────────────

    fun openProfile(userId: String) {
        Log.d(TAG, "openProfile() userId=$userId")
        viewModelScope.launch {
            userRepository.getUser(userId)
                .onSuccess { _selectedUser.value = it }
                .onFailure { Log.w(TAG, "openProfile load failed: ${it.message}") }
            checkInteractions(userId)
        }
        observePostsByUser(userId)
    }

    private fun checkInteractions(targetUid: String) {
        val currentUid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isFollowingSelectedUser.value = userRepository.isFollowing(currentUid, targetUid).getOrDefault(false)
            _isSelectedUserBlocked.value = userRepository.getBlockedUsers(currentUid).getOrDefault(emptyList()).contains(targetUid)
            _amIBlocked.value = userRepository.isBlockedByTarget(currentUid, targetUid).getOrDefault(false)
        }
    }

    fun toggleFollow(targetUid: String) {
        val currentUid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val isFollowing = _isFollowingSelectedUser.value
            _isFollowingSelectedUser.value = !isFollowing // Optimistic UI
            if (isFollowing) userRepository.unfollowUser(currentUid, targetUid)
            else userRepository.followUser(currentUid, targetUid)
        }
    }

    fun toggleBlock(targetUid: String) {
        val currentUid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val isBlocked = _isSelectedUserBlocked.value
            _isSelectedUserBlocked.value = !isBlocked // Optimistic UI
            if (isBlocked) {
                userRepository.unblockUser(currentUid, targetUid)
                blockedUsers = blockedUsers.filter { it != targetUid }
            } else {
                userRepository.blockUser(currentUid, targetUid)
                blockedUsers = blockedUsers + targetUid
                _isFollowingSelectedUser.value = false // Automatic unfollow
            }
            // Refresh feed
            feedJob?.cancel()
            observeFeed()
        }
    }

    fun observePostsByUser(userId: String) {
        userPostsJob?.cancel()
        userPostsJob = viewModelScope.launch {
            postRepository.observePostsByUser(userId)
                .catch { e -> Log.e(TAG, "userPosts error: ${e.message}"); _selectedUserPosts.value = emptyList() }
                .collect { _selectedUserPosts.value = it }
        }
    }

    fun clearSelectedUser() {
        userPostsJob?.cancel(); userPostsJob = null
        _selectedUser.value      = null
        _selectedUserPosts.value = emptyList()
        _amIBlocked.value        = false
    }

    // ─── Feed ──────────────────────────────────────────────────────────────────

    fun observeFeed() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            postRepository.observeFeed()
                .catch { e -> Log.e(TAG, "feed error: ${e.message}"); _posts.value = emptyList() }
                .collect { list ->
                    // Filter out blocked users
                    _posts.value = list.filter { it.authorId !in blockedUsers }
                }
        }
    }

    // ─── Reaction operations ───────────────────────────────────────────────────

    fun startObservingReactions(uid: String) {
        reactionsJob?.cancel()
        reactionsJob = viewModelScope.launch {
            postRepository.observeUserReactions(uid)
                .catch { e -> Log.e(TAG, "reactionsJob error: ${e.message}") }
                .collect { _userReactions.value = it }
        }
    }

    fun react(post: Post, emoji: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            postRepository.toggleReaction(post.postId, uid, emoji)
                .onFailure { e -> Log.e(TAG, "react() error: ${e.message}") }
        }
    }

    // ─── Post actions ──────────────────────────────────────────────────────────

    fun createPost(content: String, locationUrl: String = "", imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            val uid = _currentUser.value?.uid ?: authRepository.getCurrentUserId()
            if (uid.isNullOrBlank()) { _postState.value = UiState.Error("Please login first."); return@launch }
            if (imageUris.isNotEmpty() && imageUris.size < 2) {
                _postState.value = UiState.Error("Please select at least 2 images.")
                return@launch
            }

            val username  = _currentUser.value?.username?.takeIf { it.isNotBlank() } ?: "user"
            val avatarUrl = _currentUser.value?.avatarUrl ?: ""

            _postState.value = UiState.Loading
            var uploadedUrls = emptyList<String>()

            if (imageUris.isNotEmpty()) {
                val uploadRes = storageRepository.uploadPostImages(uid, imageUris)
                if (uploadRes.isFailure) {
                    _postState.value = UiState.Error("Failed to upload images.")
                    return@launch
                }
                uploadedUrls = uploadRes.getOrDefault(emptyList())
            }

            postRepository.createPost(uid, username, avatarUrl, content, locationUrl, uploadedUrls)
                .onSuccess { _postState.value = UiState.Success(Unit) }
                .onFailure { _postState.value = UiState.Error(it.message ?: "Failed to post.") }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId()
            if (uid == null || uid != post.authorId) {
                _postState.value = UiState.Error("You can only delete your own posts."); return@launch
            }
            _postState.value = UiState.Loading
            postRepository.deletePost(post.postId)
                .onSuccess { _postState.value = UiState.Success(Unit) }
                .onFailure { _postState.value = UiState.Error(it.message ?: "Failed to delete.") }
        }
    }

    fun repost(originalPost: Post, content: String) {
        viewModelScope.launch {
            val uid = _currentUser.value?.uid ?: authRepository.getCurrentUserId()
            if (uid.isNullOrBlank()) { _postState.value = UiState.Error("Please login first."); return@launch }
            if (content.isBlank()) { _postState.value = UiState.Error("Repost comment cannot be empty."); return@launch }
            
            val username  = _currentUser.value?.username?.takeIf { it.isNotBlank() } ?: "user"
            val avatarUrl = _currentUser.value?.avatarUrl ?: ""

            _postState.value = UiState.Loading
            postRepository.repost(originalPost, uid, username, avatarUrl, content)
                .onSuccess { _postState.value = UiState.Success(Unit) }
                .onFailure { _postState.value = UiState.Error(it.message ?: "Failed to repost.") }
        }
    }

    // ─── Search ────────────────────────────────────────────────────────────────

    fun searchUsers(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            userRepository.searchUsers(query)
                .onSuccess { results ->
                    Log.d(TAG, "searchUsers() got ${results.size} results")
                    _searchResults.value = results
                    _searchState.value   = UiState.Success(Unit)
                }
                .onFailure { e ->
                    Log.e(TAG, "searchUsers() error: ${e.message}", e)
                    _searchResults.value = emptyList()
                    _searchState.value   = UiState.Error(e.message ?: "Search failed.")
                }
        }
    }
    fun clearSearch() { _searchResults.value = emptyList(); _searchState.value = UiState.Idle }

    // ─── Recent searches ───────────────────────────────────────────────────────

    fun addRecentSearch(user: User) {
        val current = _recentSearches.value.filter { it.uid != user.uid }
        _recentSearches.value = (listOf(user) + current).take(5)
    }

    fun clearRecentSearches() { _recentSearches.value = emptyList() }

    // ─── Comments ──────────────────────────────────────────────────────────────

    fun observeComments(postId: String) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            postRepository.observeComments(postId)
                .catch { e -> Log.e(TAG, "observeComments error: ${e.message}"); _selectedPostComments.value = emptyList() }
                .collect { _selectedPostComments.value = it }
        }
    }

    fun addComment(post: Post, content: String) {
        val user = _currentUser.value
        if (user == null) { _commentState.value = UiState.Error("Please login first."); return }
        if (content.isBlank()) { _commentState.value = UiState.Error("Comment cannot be empty."); return }
        viewModelScope.launch {
            _commentState.value = UiState.Loading
            postRepository.addComment(
                postId          = post.postId,
                authorId        = user.uid,
                authorUsername  = user.username,
                authorAvatarUrl = user.avatarUrl,
                content         = content
            ).onSuccess { _commentState.value = UiState.Success(Unit) }
             .onFailure { e -> _commentState.value = UiState.Error(e.message ?: "Failed to comment.") }
        }
    }

    fun resetCommentState() { _commentState.value = UiState.Idle }

    fun clearComments() {
        commentsJob?.cancel(); commentsJob = null
        _selectedPostComments.value = emptyList()
    }

    // ─── Block (enhanced) ──────────────────────────────────────────────────────

    fun refreshBlockedUsers() {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            userRepository.getBlockedUsers(uid).onSuccess { list ->
                blockedUsers          = list
                _blockedUserIds.value = list
            }
        }
    }

    fun blockUser(targetUid: String) {
        val currentUid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            userRepository.blockUser(currentUid, targetUid)
                .onSuccess {
                    _isSelectedUserBlocked.value      = true
                    _isSelectedUserBlockedByMe.value  = true
                    _isFollowingSelectedUser.value    = false
                    _profileBlockMessage.value        = "You have blocked this user."
                    _selectedUserPosts.value          = emptyList()
                    refreshBlockedUsers()
                    _posts.value = _posts.value.filter { it.authorId != targetUid }
                    Log.d(TAG, "blockUser() OK targetUid=$targetUid")
                }
                .onFailure { e -> Log.e(TAG, "blockUser() FAILED: ${e.message}") }
        }
    }

    fun unblockUser(targetUid: String) {
        val currentUid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            userRepository.unblockUser(currentUid, targetUid)
                .onSuccess {
                    _isSelectedUserBlocked.value      = false
                    _isSelectedUserBlockedByMe.value  = false
                    _profileBlockMessage.value        = null
                    refreshBlockedUsers()
                    openProfileEnhanced(targetUid)
                    Log.d(TAG, "unblockUser() OK targetUid=$targetUid")
                }
                .onFailure { e -> Log.e(TAG, "unblockUser() FAILED: ${e.message}") }
        }
    }

    /** openProfile with full bidirectional block checks */
    fun openProfileEnhanced(userId: String) {
        Log.d(TAG, "openProfileEnhanced() userId=$userId")
        val currentUid = authRepository.getCurrentUserId() ?: return
        _isSelectedUserBlockedByMe.value          = false
        _isCurrentUserBlockedBySelectedUser.value = false
        _profileBlockMessage.value                = null
        _selectedUserPosts.value                  = emptyList()
        userPostsJob?.cancel()

        viewModelScope.launch {
            userRepository.getUser(userId)
                .onSuccess { _selectedUser.value = it }
                .onFailure { Log.w(TAG, "openProfileEnhanced load user failed: ${it.message}") }

            val myBlockList     = userRepository.getBlockedUsers(currentUid).getOrDefault(emptyList())
            val blockedByMe     = myBlockList.contains(userId)
            val blockedByTarget = userRepository.isBlockedByTarget(currentUid, userId).getOrDefault(false)

            when {
                blockedByMe -> {
                    _isSelectedUserBlockedByMe.value = true
                    _isSelectedUserBlocked.value     = true
                    _profileBlockMessage.value       = "You have blocked this user."
                }
                blockedByTarget -> {
                    _isCurrentUserBlockedBySelectedUser.value = true
                    _amIBlocked.value          = true
                    _profileBlockMessage.value = "You cannot view this profile."
                }
                else -> {
                    _isSelectedUserBlocked.value   = false
                    _amIBlocked.value              = false
                    _isFollowingSelectedUser.value = userRepository.isFollowing(currentUid, userId).getOrDefault(false)
                    observePostsByUser(userId)
                }
            }
        }
    }

}
