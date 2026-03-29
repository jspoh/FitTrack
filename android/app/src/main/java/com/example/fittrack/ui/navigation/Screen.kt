package com.example.fittrack.ui.navigation

sealed class Screen(val route: String) {
    object SplashScreen : Screen("splashscreen")
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Activity : Screen("activity")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}
