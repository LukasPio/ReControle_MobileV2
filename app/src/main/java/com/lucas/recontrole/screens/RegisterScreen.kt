package com.lucas.recontrole.screens

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lucas.recontrole.components.AppLogo
import com.lucas.recontrole.components.EmailInputField
import com.lucas.recontrole.components.PasswordInputField
import com.lucas.recontrole.components.SubmitButton
import com.lucas.recontrole.components.UserNameInputField

@Composable
fun RegisterScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {
        AppLogo()
        UserNameInputField(
            name,
            {it -> name = it}
        )
        EmailInputField(
            email,
            {it -> email = it}
        )
        PasswordInputField(
            password,
            "Senha",
            "Digite sua senha",
            {it -> password = it}
        )
        PasswordInputField(
            passwordConfirmation,
            "Confirme a senha",
            "Digite sua senha novamente",
            {it -> passwordConfirmation = it}
        )
        Spacer(modifier = Modifier.height(8.dp))
        SubmitButton(
            "Criar conta",
            {},
            modifier = Modifier.padding(12.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Já possui uma conta?")
            Text(
                text = " Clique aqui.",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    navController.navigate("login")
                }
            )
        }
    }
}