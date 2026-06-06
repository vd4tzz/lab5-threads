package com.example.threadslite.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.threadslite.ui.screen.AuthScreen
import com.example.threadslite.ui.screen.HomeScreen
import com.example.threadslite.ui.screen.ProfileScreen
import com.example.threadslite.ui.screen.SearchScreen

sealed class AppScreen {
    object Home   : AppScreen()
    object Search : AppScreen()
    data class Profile(val userId: String) : AppScreen()
}

@Composable
fun ThreadsLiteApp(vm: MainViewModel = viewModel()) {
    val isLoggedIn  by vm.isLoggedIn.collectAsStateWithLifecycle()
    // Collect currentUser as State so uid is always fresh when profile button is tapped
    val currentUser by vm.currentUser.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    if (!isLoggedIn) {
        AuthScreen(vm = vm)
        return
    }

    when (val screen = currentScreen) {
        is AppScreen.Home -> HomeScreen(
            vm         = vm,
            onNavigate = { route ->
                when (route) {
                    "search"  -> currentScreen = AppScreen.Search
                    "profile" -> {
                        // Use the collected State<User?> — always current
                        val uid = currentUser?.uid ?: ""
                        if (uid.isNotBlank()) {
                            vm.openProfile(uid)
                            currentScreen = AppScreen.Profile(uid)
                        }
                        // If uid still blank, user hasn't loaded yet — stay on Home
                    }
                }
            },
            onOpenProfile = { userId ->
                vm.openProfile(userId)
                currentScreen = AppScreen.Profile(userId)
            }
        )

        is AppScreen.Search -> SearchScreen(
            vm            = vm,
            onBack        = { currentScreen = AppScreen.Home },
            onOpenProfile = { userId ->
                vm.openProfile(userId)
                currentScreen = AppScreen.Profile(userId)
            }
        )

        is AppScreen.Profile -> ProfileScreen(
            vm            = vm,
            userId        = screen.userId,
            currentUserId = currentUser?.uid ?: "",
            onBack        = {
                vm.clearSelectedUser()
                currentScreen = AppScreen.Home
            }
        )
    }
}
