package com.example.threadslite.ui.navigation

/**
 * Sealed class defining all navigation routes in the app.
 * Routes with dynamic arguments use "{param}" template syntax.
 */
sealed class Screen(val route: String) {
    object Auth        : Screen("auth")
    object Home        : Screen("home")
    object CreatePost  : Screen("create_post")
    object Search      : Screen("search")

    /** Profile can display any user's profile; pass userId as a nav argument */
    object Profile     : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}
