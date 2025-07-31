package com.lucas.recontrole.components

import androidx.annotation.DrawableRes
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
fun GenericInputField(
    title: String,
    placeholder: String,
    text: String,
    onTextChange: (String) -> Unit,
    @DrawableRes icon: Int,
    iconDescription: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = text,
        onValueChange = {
            if (it.length <= Constants.MAX_EMAIL_LENGTH) onTextChange(it)
        },
        label = { Text(title) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = iconDescription
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        )
    )
}