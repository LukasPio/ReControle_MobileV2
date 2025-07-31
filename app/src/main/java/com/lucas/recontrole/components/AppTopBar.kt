package com.lucas.recontrole.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.lucas.recontrole.R

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    title: String = "",
    navController: NavController,
    onSync: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth()
            .height(110.dp)
            .background(MaterialTheme.colorScheme.tertiary)
            .shadow(1.dp)
            .padding(end = 16.dp, start = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo of Recontrole",
            Modifier.size(70.dp).clickable(
                indication = ripple(radius = 45.dp),
                interactionSource = remember {MutableInteractionSource()}
            ) {}
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row {
            Image(
                painter = painterResource(R.drawable.outline_account_circle),
                contentDescription = "Avatar icon",
                Modifier.size(30.dp).clickable(
                    indication = ripple(radius = 15.dp),
                    interactionSource = remember {MutableInteractionSource()}
                ){}
            )
            Spacer(Modifier.width(16.dp))
            Box {
                Image(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More vert icon",
                    Modifier.size(30.dp).clickable(
                        indication = ripple(radius = 15.dp),
                        interactionSource = remember {MutableInteractionSource()}
                    ){
                        expanded = true
                    }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {expanded = false}
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Sair",
                                fontSize = 16.sp
                            )
                        }, {
                            expanded = false
                            Firebase.auth.signOut()
                            navController.navigate("login") {
                                popUpTo("home") {
                                    inclusive = true
                                }
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Sincronizar",
                                fontSize = 16.sp
                            )
                        }, {
                            expanded = false
                            onSync()
                        }
                    )
                }
            }
        }
    }
}