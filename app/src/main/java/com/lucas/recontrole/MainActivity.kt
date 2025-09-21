package com.lucas.recontrole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.lucas.recontrole.screens.ForgotPasswordScreen
import com.lucas.recontrole.screens.HomeScreen
import com.lucas.recontrole.screens.LoginScreen
import com.lucas.recontrole.screens.RegisterScreen
import com.lucas.recontrole.ui.theme.ReControleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReControleTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val initialScreen = if (Firebase.auth.currentUser != null) "home" else "login"
    NavHost(navController = navController, startDestination = initialScreen) {
        composable("home") { HomeScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("login") {LoginScreen(navController)}
        composable("forgotPassword") {ForgotPasswordScreen(navController)}
    }
}
