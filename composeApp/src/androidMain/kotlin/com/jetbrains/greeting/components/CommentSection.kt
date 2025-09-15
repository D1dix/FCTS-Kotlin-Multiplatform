package com.jetbrains.greeting.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.greeting.data.entitys.CommentEntity
import com.jetbrains.greeting.viewmodels.CommentViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlinx.coroutines.launch


// Lista de colores predefinidos para los bordes
private val commentBorderColors = listOf(
    Color(0xFF4CAF50), // Verde
    Color(0xFF2196F3), // Azul
    Color(0xFFE91E63), // Rosa
    Color(0xFFFFC107), // Amarillo
    Color(0xFF9C27B0)  // Púrpura
)

// Función para obtener un color aleatorio de la lista
fun getRandomCommentColor(): Color {
    return commentBorderColors.random()
}

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun CommentSection(
    comments: List<CommentEntity>,
    onAddComment: (String, Int, Boolean) -> Unit,
    onDeleteComment: (Long) -> Unit,
    canDeleteComments: Boolean,
    canAddSpecialComments: Boolean,
    canAddComments: Boolean,
    puedeComentar: Boolean,
    commentViewModel: CommentViewModel = koinViewModel()
) {
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var isDestacado by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Cargar los comentarios cuando se muestra la sección
    LaunchedEffect(Unit) {
        commentViewModel.loadComments(comments.firstOrNull()?.menuItemId ?: 0)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comentarios (${comments.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canAddComments) {
                    IconButton(
                        onClick = {
                            if (puedeComentar) {
                                showAddCommentDialog = true
                            } else {
                                showError = true
                                errorMessage = "Debes haber pedido este plato antes de poder comentarlo"
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir comentario",
                            modifier = Modifier.size(32.dp),
                            tint = if (puedeComentar) Color(0xFFFFD700) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                IconButton(
                    onClick = { showComments = !showComments },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (showComments) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (showComments) "Ocultar comentarios" else "Mostrar comentarios",
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFFFD700)
                    )
                }
            }
        }

        if (showError) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (showComments) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comments.forEach { comment ->
                        CommentCard(
                            comment = comment,
                            onDelete = {
                                scope.launch {
                                    try {
                                        onDeleteComment(comment.id)
                                    } catch (e: Exception) {
                                        showError = true
                                        errorMessage = "Error al eliminar el comentario: ${e.message}"
                                    }
                                }
                            },
                            canDelete = canDeleteComments
                        )
                    }
                }
            }
        }
    }

    if (showAddCommentDialog) {
        AlertDialog(
            onDismissRequest = { showAddCommentDialog = false },
            title = { Text("Añadir comentario") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Tu comentario") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 5
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Calificación:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(5) { index ->
                                IconButton(
                                    onClick = { rating = index + 1 },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = if (index < rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }

                    if (canAddSpecialComments) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDestacado,
                                onCheckedChange = { isDestacado = it }
                            )
                            Text(
                                text = "Comentario destacado",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (commentText.isNotBlank() && rating > 0) {
                            try {
                                onAddComment(commentText, rating, isDestacado)
                                commentText = ""
                                rating = 0
                                isDestacado = false
                                showAddCommentDialog = false
                            } catch (e: Exception) {
                                showError = true
                                errorMessage = "Error al añadir el comentario: ${e.message}"
                            }
                        } else {
                            showError = true
                            errorMessage = "Por favor, completa todos los campos"
                        }
                    },
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    )
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCommentDialog = false },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CommentCard(
    comment: CommentEntity,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val borderColor = if (comment.destacado) {
        MaterialTheme.colorScheme.primary
    } else {
        getRandomCommentColor()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (comment.destacado)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = comment.user,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = comment.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar comentario",
                            tint = Color.Red
                        )
                    }
                }
            }

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                repeat(comment.rating) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
 