package com.lucas.recontrole.screens

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import com.lucas.recontrole.dtos.UserLoginDTO
import com.lucas.recontrole.components.AppLogo
import com.lucas.recontrole.components.EmailInputField
import com.lucas.recontrole.components.ErrorDialog
import com.lucas.recontrole.components.PasswordInputField
import com.lucas.recontrole.components.SubmitButton
import com.lucas.recontrole.notifications.NotificationManager

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppLogo()
        EmailInputField(
            email = email,
            onEmailChange = { email = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PasswordInputField(
            password = password,
            "Senha",
            "Digite sua senha",
            onPasswordChange = { password = it }
        )
        Text(
            text = "Esqueceu a senha?",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                navController.navigate("forgotPassword")
            }
                .padding(8.dp)
                .align(Alignment.End)
        )
        SubmitButton(
            text = "Login",
            onClick = {
                login(
                    context = context,
                    userLoginDTO = UserLoginDTO(email, password),
                    onSuccess = {
                        showErrorDialog = false
                        // Iniciar monitoramento de notificações
                        NotificationManager.startMonitoring(context)
                        navController.navigate("labs") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onError = {
                        errorMessage = it
                        showErrorDialog = true
                    }
                )
            },
            modifier = Modifier.padding(12.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ainda não possui uma conta?")
            Text(
                text = " Clique aqui.",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    navController.navigate("register")
                }
            )
        }
        if (showErrorDialog) {
            ErrorDialog(
                onDismissRequest = { showErrorDialog = false },
                onConfirmation = { showErrorDialog = false },
                dialogText = errorMessage
            )
        }
    }
}

private fun login(
    context: android.content.Context,
    userLoginDTO: UserLoginDTO,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val validData = validate(userLoginDTO)

    if (!validData.first) {
        onError(validData.second)
        return
    }

    val auth = Firebase.auth
    auth.signInWithEmailAndPassword(userLoginDTO.email, userLoginDTO.password).addOnFailureListener { error ->
        when (error) {
            is FirebaseAuthInvalidUserException -> onError("Nenhum usuário com esse e-mail foi encontrado")
            is FirebaseAuthInvalidCredentialsException -> onError("E-mail ou senha inválidos")
            is FirebaseTooManyRequestsException -> onError("Muitas tentativas. Tente novamente mais tarde")
            else -> onError("Erro inesperado. Tente novamente ou entre em contato com o suporte")
        }
    }.addOnSuccessListener { result ->
        if (result.user?.isEmailVerified == false) {
            onError("Verifique seu e-mail! Um novo link de verificação foi enviado")
            result.user?.sendEmailVerification()
            return@addOnSuccessListener
        }
        onSuccess()
    }
}

private fun validate(userLoginDTO: UserLoginDTO): Pair<Boolean, String> {
    if (userLoginDTO.email.isBlank() || userLoginDTO.password.isBlank()) {
        return false to "Todos os campos devem ser preenchidos"
    }

    if (!Patterns.EMAIL_ADDRESS.matcher(userLoginDTO.email).matches()) {
        return false to "E-mail inválido"
    }

    return true to ""
}