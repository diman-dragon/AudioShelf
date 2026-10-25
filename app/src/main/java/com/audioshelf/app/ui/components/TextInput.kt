package com.audioshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    shape: Shape = RoundedCornerShape(10.dp),
    background: Color = AppTheme.colors.bg,
    horizontal: Int = 12,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    maxLength: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
) {
    val c = AppTheme.colors
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.take(maxLength)) },
        singleLine = singleLine,
        textStyle = TextStyle(color = c.text, fontSize = 16.sp),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { if (singleLine) focus.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(if (focused) 2.dp else 1.dp, if (focused) c.accent else c.line, shape)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = horizontal.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) AText(placeholder, 16f, c.muted, maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
fun FieldBox(label: String, hint: String, content: @Composable () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AText(label, 13f, c.text, weight = FontWeight.SemiBold)
        content()
        AText(hint, 12f, c.muted)
    }
}

@Composable
fun Field(
    label: String,
    hint: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int = Int.MAX_VALUE,
) {
    FieldBox(label, hint) { TextInput(value, onChange, placeholder = placeholder, keyboardType = keyboardType, maxLength = maxLength) }
}
