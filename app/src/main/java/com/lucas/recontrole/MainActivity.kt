package com.lucas.recontrole

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    // Launcher para solicitar permissão de notificação (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permissão concedida, iniciar monitoramento
            NotificationManager.startMonitoring(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permissão de notificação (Android 13+)
        requestNotificationPermission()

        // Iniciar monitoramento de ocorrências se usuário estiver logado
        if (Firebase.auth.currentUser != null) {
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
                    // Permissão já concedida
                }
                else -> {
                    // Solicitar permissão ao usuário
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Após login, redirecionar para tela de laboratórios
    val initialScreen = if (Firebase.auth.currentUser != null) "labs" else "login"

    NavHost(navController = navController, startDestination = initialScreen) {
        // Tela de laboratórios (nova tela inicial após login)
        composable("labs") {
            LabsScreen(navController)
        }

        // Tela de ocorrências por laboratório
        composable(
            route = "labOccurrences/{labName}",
            arguments = listOf(navArgument("labName") { type = NavType.StringType })
        ) { backStackEntry ->
            val labName = backStackEntry.arguments?.getString("labName") ?: ""
            LabOccurrencesScreen(navController, labName)
        }

        // Tela home (mantida para compatibilidade - opcional)
        composable("home") {
            HomeScreen(navController)
        }

        // Rotas de autenticação
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