package com.lucas.recontrole.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lucas.recontrole.R

@Composable
fun AppTopBar(modifier: Modifier = Modifier) {
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
            Modifier.size(90.dp).clickable(
                indication = ripple(radius = 35.dp),
                interactionSource = remember {MutableInteractionSource()}
            ) {}
        )
        Row {
            Image(
                painter = painterResource(R.drawable.outline_account_circle),
                contentDescription = "Avatar icon",
                Modifier.size(35.dp).clickable(
                    indication = ripple(radius = 15.dp),
                    interactionSource = remember {MutableInteractionSource()}
                ){}
            )
            Spacer(Modifier.width(16.dp))
            Image(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Avatar icon",
                Modifier.size(35.dp).clickable(
                    indication = ripple(radius = 15.dp),
                    interactionSource = remember {MutableInteractionSource()}
                ){}
            )
        }
    }
}