package com.spotik.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spotik.app.ui.theme.BorderDark
import com.spotik.app.ui.theme.BorderLight
import com.spotik.app.ui.theme.CardDark

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    padding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(CardDark.copy(alpha = 0.55f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(BorderLight, BorderDark)),
                shape = shape,
            )
            .padding(padding),
        content = content,
    )
}
