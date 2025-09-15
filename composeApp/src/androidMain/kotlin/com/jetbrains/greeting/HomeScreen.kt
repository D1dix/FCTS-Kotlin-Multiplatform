package com.jetbrains.greeting

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import com.jetbrains.greeting.data.entitys.ChatMessage
import com.jetbrains.greeting.viewmodels.UserViewModel
import com.jetbrains.greeting.viewmodels.ChatViewModel
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import androidx.compose.ui.res.painterResource

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    KoinExperimentalAPI::class
)
@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: UserViewModel = koinViewModel(),
    chatViewModel: ChatViewModel = koinViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showLogoutMenu by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    // Fuera de cualquier función (al principio del archivo)
    val carruselImageUrls = listOf(
        "https://images.squarespace-cdn.com/content/v1/5f849c09a42e5b65481ec27e/445249c4-db0b-4a05-bf77-3d39c2c30de5/Promo-restaurante.jpg",
        "https://img.freepik.com/psd-gratis/plantilla-oferta-descuentos-restaurantes_23-2148287296.jpg",
        "https://static.rfstat.com/gm-media/template/132/slider-images/369/90529b14e3b9_1x.jpeg",
        "https://dv7zfk0hwmxgu.cloudfront.net/sites/default/files/styles/auto_1500_width/public/article-images/137243/slideshow-1629806260.jpg"
    )

    val bienvenidaImageUrl = "https://m.media-amazon.com/images/I/61LKiDoGYoL.jpg"
    val pagerState = rememberPagerState(pageCount = { carruselImageUrls.size })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(7000)
            pagerState.animateScrollToPage(
                page = (pagerState.currentPage + 1) % pagerState.pageCount,
                animationSpec = tween(1000)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logotipowashabi),
                        contentDescription = "Logo Washabi",
                        modifier = Modifier.size(48.dp)
                    )
                },
                actions = {
                    if (currentUser == null) {
                        TextButton(onClick = onNavigateToLogin) {
                            Text("Iniciar Sesión")
                        }
                    } else {
                        Box {
                            TextButton(onClick = { showLogoutMenu = true }) {
                                Text(
                                    text = currentUser?.nombre ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showLogoutMenu,
                                onDismissRequest = { showLogoutMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mi Perfil") },
                                    onClick = {
                                        onNavigateToProfile()
                                        showLogoutMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cerrar Sesión") },
                                    onClick = {
                                        viewModel.logout()
                                        showLogoutMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(
                        painter = painterResource(id = R.drawable.restaurant_24),
                        contentDescription = "Menú") },
                    label = { Text("Menú") },
                    selected = false,
                    onClick = onNavigateToMenu
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = onNavigateToProfile
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    showChat = true
                    chatViewModel.clearChat()
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_24),
                    contentDescription = "Chat",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(16.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = carruselImageUrls[page],
                                contentDescription = "Imagen promocional $page",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    PageIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "¡Bienvenido!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Descubre nuestra deliciosa selección de platos",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        AsyncImage(
                            model = bienvenidaImageUrl,
                            contentDescription = "Imagen de bienvenida",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            if (showChat) {
                ChatDialog(
                    messages = messages,
                    isLoading = isLoading,
                    messageText = messageText,
                    onMessageTextChange = { messageText = it },
                    onSendMessage = {
                        if (messageText.isNotBlank()) {
                            chatViewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    onDismiss = { showChat = false }
                )
            }
        }
    }
}

@Composable
fun PageIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pagerState.pageCount) { page ->
            val color = if (page == pagerState.currentPage) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun ChatDialog(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat de Ayuda",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { message ->
                        ChatMessageItem(message)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = onMessageTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe tu mensaje...") },
                        enabled = !isLoading
                    )
                    IconButton(
                        onClick = onSendMessage,
                        enabled = messageText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = if (messageText.isNotBlank() && !isLoading)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = message.message,
                modifier = Modifier.padding(8.dp),
                color = if (message.isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
} 