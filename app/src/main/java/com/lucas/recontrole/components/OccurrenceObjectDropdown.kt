package com.lucas.recontrole.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceObjectDropdown(
    onValueChange: (String) -> Unit,
    initialValue: String = "" // Adicionar valor inicial
) {
    val options = listOf(
        "Software", "Computador", "Notebook", "Mouse", "Teclado",
        "Cadeira", "Ar condicionado", "Televisão", "Cabo", "Outro"
    )

    var isExpanded by remember { mutableStateOf(false) }
    var isSelected by remember {
        mutableStateOf(initialValue.takeIf { it.isNotEmpty() } ?: "Selecione uma categoria")
    }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField(
            value = isSelected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria mais próxima do problema:") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.tertiary,
                focusedContainerColor = MaterialTheme.colorScheme.tertiary
            )
        )

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        isSelected = option
                        onValueChange(option) // Só chama quando realmente seleciona
                        isExpanded = false
                    }
                )
            }
        }
    }
}