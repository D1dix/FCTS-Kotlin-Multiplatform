package com.jetbrains.greeting

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.jetbrains.greeting.components.LoadingIndicator
import com.jetbrains.greeting.components.CreateMenuItemDialog
import com.jetbrains.greeting.data.entitys.MenuItemEntity
import com.jetbrains.greeting.data.entitys.UserEntity
import com.jetbrains.greeting.data.entitys.UserType
import com.jetbrains.greeting.data.entitys.CartItemEntity
import com.jetbrains.greeting.viewmodels.CartViewModel
import com.jetbrains.greeting.viewmodels.MenuViewModel
import com.jetbrains.greeting.viewmodels.UserViewModel
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.jetbrains.greeting.R

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, KoinExperimentalAPI::class)
@Composable
fun MenuScreen(
    categoryId: String,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateBack: () -> Unit,
    menuViewModel: MenuViewModel = koinViewModel(),
    cartViewModel: CartViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val menuItems by menuViewModel.menuItems.collectAsState()
    val restaurants by menuViewModel.restaurants.collectAsState()
    val selectedRestaurant by menuViewModel.selectedRestaurant.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<MenuItemEntity?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var showOrderAlert by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val httpClient: HttpClient = koinInject()
    val currentUser by userViewModel.currentUser.collectAsState()
    val activeCart by cartViewModel.activeCart.collectAsState()
    val hasActiveCart by cartViewModel.hasActiveCart.collectAsState()
    var showRestaurantSelector by remember { mutableStateOf(false) }

    LaunchedEffect(categoryId) {
        scope.launch {
            println("🔄 [MenuScreen] Cargando elementos del menú para categoría: $categoryId")
            menuViewModel.loadMenuItems()
            delay(1500)
            isLoading = false
            println("✅ [MenuScreen] Elementos cargados: ${menuItems.size}")
        }
    }

    LaunchedEffect(currentUser?.email) {
        currentUser?.email?.let { email ->
            cartViewModel.loadActiveCartForUser(email)
            cartViewModel.loadUserCarts(email)
            cartViewModel.loadRestaurants(email)
        }
    }

    LaunchedEffect(activeCart) {
        if (activeCart == null) {
            menuViewModel.selectRestaurant(null)
        } else {
            activeCart?.restaurantId?.let { restaurantId ->
                menuViewModel.selectRestaurant(restaurantId)
            }
        }
    }

    LaunchedEffect(Unit) {
        cartViewModel.checkAndExpireCarts()
    }


    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Menú") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Ver carrito"
                        )
                    }
                    if (!hasActiveCart) {
                        IconButton(onClick = { showRestaurantSelector = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_restaurant_menu_24),
                                contentDescription = "Seleccionar restaurante",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (currentUser?.rol == UserType.ROLE_ADMIN.toString()) {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir nuevo item")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedRestaurant != null) {
                Text(
                    text = "Restaurante: $selectedRestaurant",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(
                        size = 48,
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4f
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    println("🔍 [MenuScreen] Estado actual - HasActiveCart: $hasActiveCart, ActiveCart: $activeCart")
                    
                    val filteredItems = menuItems.filter { 
                        val categoryMatch = categoryId == "all" || it.category == categoryId
                        val restaurantMatch = if (hasActiveCart && activeCart != null) {
                            println("🔍 [MenuScreen] Comparando con carrito activo - Item: ${it.restaurant}, Cart: ${activeCart!!.restaurantId}")
                            it.restaurant == activeCart!!.restaurantId
                        } else {
                            println("🔍 [MenuScreen] Comparando con restaurante seleccionado - Item: ${it.restaurant}, Selected: $selectedRestaurant")
                            selectedRestaurant == null || it.restaurant == selectedRestaurant
                        }
                        val visibilityMatch = !it.oculto || currentUser?.rol == UserType.ROLE_ADMIN.toString()
                        categoryMatch && restaurantMatch && visibilityMatch
                    }
                    println("📋 [MenuScreen] Total items: ${menuItems.size}, Filtered: ${filteredItems.size}")
                    items(filteredItems) { item ->
                        val canManageItem = when {
                            currentUser?.rol != UserType.ROLE_ADMIN.toString() -> false
                            currentUser?.restaurant == "all" -> true
                            currentUser?.restaurant == item.restaurant -> true
                            else -> false
                        }

                        MenuItemCard(
                            item = item,
                            onItemClick = { onNavigateToDetail(item.id.toString()) },
                            onAddToCart = {
                                if (!hasActiveCart) {
                                    showOrderAlert = true
                                } else {
                                    item.id?.let { cartViewModel.addItemToCart(it) }
                                    showSnackbar = true
                                }
                            },
                            showDeleteButton = canManageItem,
                            onDelete = { itemToDelete = item },
                            onToggleVisibility = if (canManageItem) {
                                { menuViewModel.toggleMenuItemVisibility(item.id!!) }
                            } else null
                        )
                    }
                }
            }
        }

        if (showDialog) {
                CreateMenuItemDialog(
                    onDismiss = { showDialog = false },
                    menuViewModel = menuViewModel,
                    httpClient = httpClient,
                    userViewModel = userViewModel,
                    restrictedRestaurant = if (currentUser?.restaurant != "all") currentUser?.restaurant else null
                )
        }

        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                confirmButton = {
                    Button(onClick = {
                        itemToDelete?.let { it.id?.let { it1 -> menuViewModel.deleteMenuItem(it1) } }
                        itemToDelete = null
                    }) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    Button(onClick = { itemToDelete = null }) {
                        Text("Cancelar")
                    }
                },
                title = { Text("Confirmar eliminación") },
                text = { Text("¿Estás seguro de que deseas eliminar este elemento del menú?") }
            )
        }

        if (showSnackbar) {
            LaunchedEffect(Unit) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Producto añadido al carrito",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.Dismissed) {
                        showSnackbar = false
                    }
                }
            }
        }

        if (showOrderAlert) {
            AlertDialog(
                onDismissRequest = { showOrderAlert = false },
                title = { Text("No hay pedido activo") },
                text = { Text("Debes crear un pedido desde tu perfil antes de añadir items al carrito.") },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            showOrderAlert = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Ir a Perfil")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOrderAlert = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showRestaurantSelector) {
            AlertDialog(
                onDismissRequest = { showRestaurantSelector = false },
                title = { Text("Seleccionar Restaurante") },
                text = {
                    Column {
                        restaurants.forEach { restaurant ->
                            Button(
                                onClick = {
                                    menuViewModel.selectRestaurant(restaurant)
                                    showRestaurantSelector = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (restaurant == selectedRestaurant) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(restaurant)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRestaurantSelector = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}

@Composable
fun MenuItemCard(
    item: MenuItemEntity,
    onItemClick: () -> Unit,
    onAddToCart: () -> Unit,
    showDeleteButton: Boolean = false,
    onDelete: () -> Unit,
    onToggleVisibility: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    if (onToggleVisibility != null) {
                        IconButton(
                            onClick = onToggleVisibility,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (item.oculto) R.drawable.visibility_off_24 
                                         else R.drawable.visibility_24
                                ),
                                contentDescription = if (item.oculto) "Mostrar item" else "Ocultar item",
                                tint = if (item.oculto) Color.Gray else Color.Black
                            )
                        }
                    }
                    if (showDeleteButton) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Restaurante: ${item.restaurant}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${item.price}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onItemClick, shape = RoundedCornerShape(8.dp)) {
                            Text("Ver detalles")
                        }
                        Button(onClick = onAddToCart, shape = RoundedCornerShape(8.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.add_shopping_cart_24),
                                contentDescription = "Añadir al carrito",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
