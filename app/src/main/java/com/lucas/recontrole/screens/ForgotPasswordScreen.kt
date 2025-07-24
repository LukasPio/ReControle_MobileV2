package com.lucas.recontrole.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.lucas.recontrole.components.AppLogo
import com.lucas.recontrole.components.EmailInputField
import com.lucas.recontrole.components.ErrorDialog
import com.lucas.recontrole.components.ResetPasswordButton
import com.lucas.recontrole.components.SimpleAlertDialog

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember  {mutableStateOf("")}
    var showSimpleAlertDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppLogo()
        Text(
            text = "Esqueceu a senha?",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Não se preocupe!",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.clickable(
                interactionSource = remember {  MutableInteractionSource() },
                indication = null
            ) {}
        )
        Spacer(modifier = Modifier.height(8.dp))
        EmailInputField (
            email = email,
            onEmailChange = {email = it},
        )
        ResetPasswordButton(modifier = Modifier.padding(16.dp), onClick = {
            sendPasswordResetLink(
                email, onComplete = {
                    showSimpleAlertDialog = true
                })
        })
        Text(
            text = "Voltar",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                navController.popBackStack()
            }
        )
        if (showSimpleAlertDialog) {
            SimpleAlertDialog(
                onDismissRequest = {
                    showSimpleAlertDialog = false
                    navController.navigate("login")
                },
                onConfirmation = {
                    showSimpleAlertDialog = false
                    navController.navigate("login")
                },
                "Redefina sua senha",
                "Um link para redefinição foi enviado"
            )
        }
    }
}

private fun sendPasswordResetLink(
    email: String,
    onComplete: () -> Unit
) {
    Firebase.auth.sendPasswordResetEmail(email)
    onComplete()
}
