package com.jetbrains.greeting.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun LoadingScreen(
    message: String = "Cargando...",
    icon: ImageVector = Icons.Default.Build,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, MaterialTheme.shapes.medium)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icono giratorio
                val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                Icon(
                    imageVector = icon,
                    contentDescription = "Cargando",
                    tint = iconColor,
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(rotation)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingIndicator(
    size: Int = 32,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 4f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = color,
            strokeWidth = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize,
            modifier = Modifier.fillMaxSize()
        )
    }
} 