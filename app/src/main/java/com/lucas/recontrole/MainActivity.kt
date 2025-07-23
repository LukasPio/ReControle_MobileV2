package com.lucas.recontrole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lucas.recontrole.screens.ForgotPasswordScreen
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
    NavHost(navController = navController, startDestination = "login") {
        composable("register") { RegisterScreen(navController) }
        composable("login") {LoginScreen(navController)}
        composable("forgotPassword") {ForgotPasswordScreen(navController)}
    }
}

