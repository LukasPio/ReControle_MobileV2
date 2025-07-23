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
import com.lucas.recontrole.components.AppLogo
import com.lucas.recontrole.components.EmailInputField
import com.lucas.recontrole.components.ResetPasswordButton

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember  {mutableStateOf("")}

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
    }
}
