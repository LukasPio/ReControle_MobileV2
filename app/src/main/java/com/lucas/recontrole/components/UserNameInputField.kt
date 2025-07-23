package com.lucas.recontrole.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.lucas.recontrole.Constants
import com.lucas.recontrole.R

@Composable
fun UserNameInputField(
    userName: String,
    onUserNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = userName,
        onValueChange = {
            if (it.length <= Constants.MAX_USER_NAME_LENGTH) onUserNameChange(it)
        },
        label = { Text("Usuário") },
        placeholder = { Text("Digite seu nome de usuário") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.outline_person_24),
                contentDescription = "User icon"
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        )
    )
}