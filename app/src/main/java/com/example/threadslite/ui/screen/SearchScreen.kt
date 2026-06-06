package com.example.threadslite.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threadslite.ui.MainViewModel
import com.example.threadslite.ui.component.EmptyState
import com.example.threadslite.ui.component.UserListItem
import com.example.threadslite.util.UiState

@Composable
fun SearchScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val searchResults  by vm.searchResults.collectAsStateWithLifecycle()
    val searchState    by vm.searchState.collectAsStateWithLifecycle()
    val recentSearches by vm.recentSearches.collectAsStateWithLifecycle()

    var query       by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    val isSearching = searchState is UiState.Loading
    val showRecent  = query.isBlank()

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
            TextButton(onClick = { vm.clearSearch(); onBack() }) {
                Text("← Back", color = Color(0xFF555555), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Text(
                text       = "Search",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                color      = Color.Black,
                modifier   = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }

        // Search input
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value         = query,
                onValueChange = { query = it; if (it.isBlank()) hasSearched = false },
                placeholder   = { Text("Search by username…", color = Color(0xFFBBBBBB)) },
                singleLine    = true,
                enabled       = !isSearching,
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(50.dp),
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    disabledContainerColor  = Color(0xFFF5F5F5),
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor  = Color.Transparent,
                    cursorColor             = Color.Black
                )
            )
            Button(
                onClick  = { hasSearched = true; vm.searchUsers(query) },
                enabled  = query.isNotBlank() && !isSearching,
                shape    = RoundedCornerShape(50.dp),
                modifier = Modifier.height(52.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color.Black,
                    contentColor           = Color.White,
                    disabledContainerColor = Color(0xFFDDDDDD)
                )
            ) {
                if (isSearching)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else
                    Text("Go", fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = Color(0xFFF0F0F0))

        // Content
        if (showRecent) {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "Recent",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFFAAAAAA),
                            letterSpacing = 0.8.sp
                        )
                        if (recentSearches.isNotEmpty()) {
                            TextButton(onClick = { vm.clearRecentSearches() }) {
                                Text("Clear all", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                            }
                        }
                    }
                }
                if (recentSearches.isEmpty()) {
                    item { EmptyState("No recent searches.") }
                } else {
                    items(recentSearches, key = { it.uid }) { user ->
                        UserListItem(user = user, onClick = {
                            vm.addRecentSearch(user)
                            onOpenProfile(user.uid)
                        })
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        } else if (!hasSearched || searchResults.isEmpty()) {
            EmptyState(
                message = if (!hasSearched) "Enter a username to search."
                          else "No users found for \"$query\"."
            )
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
            ) {
                item {
                    Text(
                        "${searchResults.size} result${if (searchResults.size > 1) "s" else ""}",
                        fontSize = 12.sp,
                        color    = Color(0xFFAAAAAA),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(searchResults, key = { it.uid }) { user ->
                    UserListItem(user = user, onClick = {
                        vm.addRecentSearch(user)
                        onOpenProfile(user.uid)
                    })
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                }
            }
        }
    }
}
