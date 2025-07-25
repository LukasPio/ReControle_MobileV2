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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.lucas.recontrole.Constants
import com.lucas.recontrole.components.AppLogo
import com.lucas.recontrole.components.EmailInputField
import com.lucas.recontrole.components.ErrorDialog
import com.lucas.recontrole.components.PasswordInputField
import com.lucas.recontrole.components.SubmitButton
import com.lucas.recontrole.components.UserNameInputField
import com.lucas.recontrole.dtos.UserRequestDTO
import com.lucas.recontrole.components.SimpleAlertDialog

@Composable
fun RegisterScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSimpleAlertDialog by remember { mutableStateOf(false) }

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
            {
                createAccount(
                    UserRequestDTO(name, email, password, passwordConfirmation),
                    onError = {
                        errorMessage = it
                        showErrorDialog = true
                    },
                    onSuccess = {
                        showSimpleAlertDialog = true
                    }
                )
            },
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
        if (showErrorDialog) {
            ErrorDialog(
                onDismissRequest = {showErrorDialog = false},
                onConfirmation = {showErrorDialog = false},
                dialogText = errorMessage
            )
        }
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
                "Sua conta foi criada!",
                "Por favor, cheque seu email para verificação do mesmo"
            )
        }
    }
}

private fun createAccount(
    userRequestDTO: UserRequestDTO,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    val validData = validateUser(userRequestDTO)

    if (!validData.first) {
        onError(validData.second)
        return
    }

    val auth = Firebase.auth

    auth.createUserWithEmailAndPassword(
            userRequestDTO.email,
            userRequestDTO.password
    ).addOnSuccessListener { result ->
        val user = result.user
        val profileUpdate = userProfileChangeRequest { displayName = userRequestDTO.name }
        user?.updateProfile(profileUpdate)
        user!!.sendEmailVerification()
        onSuccess()
    }
    .addOnFailureListener { error ->
        if (error is FirebaseAuthUserCollisionException) {
            onError("Este e-mail já está em uso")
            return@addOnFailureListener
        }
        onError("Erro interno, tente novamente mais tarde")
    }
}

private fun validateUser(userRequestDTO: UserRequestDTO): Pair<Boolean, String> {
    if (userRequestDTO.name.isBlank() || userRequestDTO.email.isBlank() || userRequestDTO.password.isBlank() || userRequestDTO.passwordConfirmation.isBlank()) {
        return false to "Todos os campos devem ser preenchidos"
    }

    if (!Patterns.EMAIL_ADDRESS.matcher(userRequestDTO.email).matches()) {
        return false to "E-mail inválido"
    }

    if (userRequestDTO.password.length < Constants.MINIMUM_PASSWORD_LENGTH) {
        return false to "Sua senha deve ter no mínimo 6 caracteres"
    }

    if (userRequestDTO.password != userRequestDTO.passwordConfirmation) {
        return false to "As senhas não coincidem"
    }

    return true to ""
}
