package com.audioshelf.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

fun Modifier.tap(enabled: Boolean = true, description: String? = null, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .semantics { if (description != null) contentDescription = description }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
}
