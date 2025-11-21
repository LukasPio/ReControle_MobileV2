package com.lucas.recontrole

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.lucas.recontrole.notifications.NotificationManager
import com.lucas.recontrole.screens.ForgotPasswordScreen
import com.lucas.recontrole.screens.HomeScreen
import com.lucas.recontrole.screens.LabOccurrencesScreen
import com.lucas.recontrole.screens.LabsScreen
import com.lucas.recontrole.screens.LoginScreen
import com.lucas.recontrole.screens.RegisterScreen
import com.lucas.recontrole.ui.theme.ReControleTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Permissão de notificação concedida")
            // Iniciar monitoramento quando permissão é concedida
            if (Firebase.auth.currentUser != null) {
                NotificationManager.startMonitoring(this)
            }
        } else {
            Log.d("MainActivity", "Permissão de notificação negada")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permissão de notificação (Android 13+)
        requestNotificationPermission()

        // Se usuário está logado e permissão está concedida, iniciar monitoramento
        if (Firebase.auth.currentUser != null && hasNotificationPermission()) {
            Log.d("MainActivity", "Usuário logado, iniciando monitoramento")
            NotificationManager.startMonitoring(this)
        }

        setContent {
            ReControleTheme {
                AppNavigation()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Permissão de notificação já concedida")
                }
                else -> {
                    Log.d("MainActivity", "Solicitando permissão de notificação")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Nota: NÃO parar o monitoramento aqui!
        // O Worker continuará rodando em background
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val initialScreen = if (Firebase.auth.currentUser != null) "labs" else "login"

    NavHost(navController = navController, startDestination = initialScreen) {
        composable("labs") {
            LabsScreen(navController)
        }

        composable(
            route = "labOccurrences/{labName}",
            arguments = listOf(navArgument("labName") { type = NavType.StringType })
        ) { backStackEntry ->
            val labName = backStackEntry.arguments?.getString("labName") ?: ""
            LabOccurrencesScreen(navController, labName)
        }

        composable("home") {
            HomeScreen(navController)
        }

        composable("register") {
            RegisterScreen(navController)
        }

        composable("login") {
            LoginScreen(navController)
        }

        composable("forgotPassword") {
            ForgotPasswordScreen(navController)
        }
    }
}