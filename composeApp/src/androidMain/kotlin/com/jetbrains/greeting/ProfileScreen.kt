package com.jetbrains.greeting

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.greeting.data.entitys.CartItemEntity
import com.jetbrains.greeting.data.entitys.CartStatus
import com.jetbrains.greeting.data.entitys.UserEntity
import com.jetbrains.greeting.data.entitys.UserType
import com.jetbrains.greeting.viewmodels.UserViewModel
import com.jetbrains.greeting.viewmodels.CartViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import com.jetbrains.greeting.viewmodels.MenuViewModel
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.res.painterResource
import com.jetbrains.greeting.data.remote.TopComidaResponse
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    userViewModel: UserViewModel = koinViewModel(),
    menuViewModel: MenuViewModel = koinViewModel(),
    cartViewModel: CartViewModel = koinViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val userCarts by cartViewModel.userCarts.collectAsState()
    val restaurants by menuViewModel.restaurants.collectAsState()
    var showCreateOrderDialog by remember { mutableStateOf(false) }
    var showActiveCarts by remember { mutableStateOf(false) }
    var showClosedCarts by remember { mutableStateOf(false) }
    var showRestaurantMenu by remember { mutableStateOf(false) }
    val scrollBehavior = rememberScrollState()

    val scope = rememberCoroutineScope()
    val hasActiveCart by cartViewModel.hasActiveCart.collectAsState()

    var showTop5Dialog by remember { mutableStateOf(false) }
    var showChart by remember { mutableStateOf(false) }
    var selectedQuery by remember { mutableStateOf("Global") }
    var selectedRestaurante by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val top5Comidas by cartViewModel.top5Comidas.collectAsState()

    LaunchedEffect(currentUser?.email, currentUser?.restaurant) {
        currentUser?.email?.let { email ->
            cartViewModel.loadUserCarts(
                email,
                if (currentUser?.rol == UserType.ROLE_ADMIN.toString()) {
                    if (currentUser?.restaurant == "all") "all" else currentUser?.restaurant
                } else null
            )
        }
    }

    // Efecto para verificar y expirar carritos cada minuto
    LaunchedEffect(Unit) {
        while (true) {
            cartViewModel.checkAndExpireCarts()
            delay(60000) // 1 minuto
        }
    }

    // Efecto para cargar los datos cuando se muestra el gráfico
    LaunchedEffect(showChart) {
        if (showChart) {
            isLoading = true
            try {
                if (selectedQuery == "Global") {
                    cartViewModel.loadTop5ComidasGlobal()
                } else if (selectedRestaurante.isNotEmpty()) {
                    cartViewModel.loadTop5ComidasPorRestaurante(selectedRestaurante)
                }
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (userViewModel.currentUser.value?.rol == UserType.ROLE_ADMIN.toString()) {
                FloatingActionButton(
                    onClick = { showRestaurantMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Seleccionar restaurante"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        if (currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No has iniciado sesión",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Iniciar Sesión")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollBehavior),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar circular
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentUser?.imageUrl ?: "https://picsum.photos/200/200?random=${currentUser?.email}",
                        contentDescription = "Avatar de usuario",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
                            Text(
                                text = "Información Personal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (currentUser?.rol == UserType.ROLE_ADMIN.toString())
                                    Icons.Default.Star
                                else
                                    Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Divider()

                        ProfileItem(
                            icon = Icons.Default.Person,
                            label = "Nombre",
                            value = currentUser?.nombre ?: ""
                        )
                        ProfileItem(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = currentUser?.email ?: ""
                        )
                        ProfileItem(
                            icon = Icons.Default.AccountBox,
                            label = "Rol",
                            value = if (currentUser?.rol == UserType.ROLE_ADMIN.toString())
                                "Administrador"
                            else
                                "Usuario"
                        )
                    }
                }

                if (currentUser?.rol == UserType.ROLE_ADMIN.toString()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Funciones de Administrador",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Gestionar menú\n• Gestionar comentarios\n• Ver estadísticas\n• Gestionar carritos",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Sección de carritos activos
                            OutlinedButton(
                                onClick = { showActiveCarts = !showActiveCarts },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Carritos Activos (${userCarts.count { it.status == CartStatus.ACTIVE }})")
                                    Icon(
                                        imageVector = if (showActiveCarts)
                                            Icons.Default.KeyboardArrowDown
                                        else
                                            Icons.Default.KeyboardArrowUp,
                                        contentDescription = null
                                    )
                                }
                            }

                            if (showActiveCarts) {
                                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val filteredCarts = if (currentUser?.restaurant == "all") {
                                            userCarts.filter { it.status == CartStatus.ACTIVE }
                                        } else {
                                            userCarts.filter {
                                                it.status == CartStatus.ACTIVE &&
                                                        it.restaurantId == currentUser?.restaurant
                                            }
                                        }
                                        items(filteredCarts) { cart ->
                                            CartCard(
                                                cart = cart,
                                                menuViewModel = menuViewModel,
                                                onDelete = if (currentUser?.rol == UserType.ROLE_ADMIN.toString()) {
                                                    { cartViewModel.eliminarPedido(cart.id) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }

                            // Sección de carritos cerrados
                            OutlinedButton(
                                onClick = { showClosedCarts = !showClosedCarts },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Carritos Cerrados (${userCarts.count { it.status == CartStatus.CLOSED }})")
                                    Icon(
                                        imageVector = if (showClosedCarts)
                                            Icons.Default.KeyboardArrowDown
                                        else
                                            Icons.Default.KeyboardArrowUp,
                                        contentDescription = null
                                    )
                                }
                            }

                            if (showClosedCarts) {
                                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val filteredCarts = if (currentUser?.restaurant == "all") {
                                            userCarts.filter { it.status == CartStatus.CLOSED }
                                        } else {
                                            userCarts.filter {
                                                it.status == CartStatus.CLOSED &&
                                                        it.restaurantId == currentUser?.restaurant
                                            }
                                        }
                                        items(filteredCarts) { cart ->
                                            CartCard(cart = cart, menuViewModel = menuViewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        userViewModel.logout()
                        onNavigateToLogin()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión")
                }

                Button(
                    onClick = { showTop5Dialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ver Top 5 Comidas")
                }
            }
        }

        if (showRestaurantMenu) {
            var showTableError by remember { mutableStateOf(false) }
            var userEmail by remember { mutableStateOf("") }
            var showUserHasActiveOrder by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showRestaurantMenu = false },
                title = { Text("Seleccionar Restaurante y Usuario") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = {
                                userEmail = it
                                showUserHasActiveOrder = false
                            },
                            label = { Text("Email del usuario") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        if (showUserHasActiveOrder) {
                            Text(
                                text = "Este usuario ya tiene un pedido activo",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        LazyColumn {
                            items(restaurants) { restaurant ->
                                ListItem(
                                    headlineContent = { Text(restaurant) },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        if (userEmail.isNotBlank()) {
                                            scope.launch {
                                                try {
                                                    // Verificar si el usuario ya tiene un pedido activo
                                                    val userCarts = cartViewModel.userCarts.value
                                                    val hasActiveOrder = userCarts.any {
                                                        it.userEmail == userEmail &&
                                                                it.status == CartStatus.ACTIVE
                                                    }

                                                    if (hasActiveOrder) {
                                                        showUserHasActiveOrder = true
                                                    } else {
                                                        cartViewModel.createCart(userEmail, emptyMap(), restaurant)
                                                        showRestaurantMenu = false
                                                    }
                                                } catch (e: Exception) {
                                                    showTableError = true
                                                }
                                            }
                                        } else {
                                            showTableError = true
                                        }
                                    }
                                )
                            }
                        }

                        if (showTableError) {
                            Text(
                                text = "Por favor, ingresa el email del usuario",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (userEmail.isNotBlank()) {
                                val restaurant = restaurants.firstOrNull()
                                if (restaurant != null) {
                                    scope.launch {
                                        try {
                                            // Verificar si el usuario ya tiene un pedido activo
                                            val userCarts = cartViewModel.userCarts.value
                                            val hasActiveOrder = userCarts.any {
                                                it.userEmail == userEmail &&
                                                        it.status == CartStatus.ACTIVE
                                            }

                                            if (hasActiveOrder) {
                                                showUserHasActiveOrder = true
                                            } else {
                                                cartViewModel.createCart(userEmail, emptyMap(), restaurant)
                                                showRestaurantMenu = false
                                            }
                                        } catch (e: Exception) {
                                            showTableError = true
                                        }
                                    }
                                } else {
                                    showTableError = true
                                }
                            } else {
                                showTableError = true
                            }
                        }
                    ) {
                        Text("Crear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestaurantMenu = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showTop5Dialog) {
            AlertDialog(
                onDismissRequest = { showTop5Dialog = false },
                title = { Text("Selecciona tipo de consulta") },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedQuery == "Global",
                                onClick = {
                                    selectedQuery = "Global"
                                    selectedRestaurante = ""
                                }
                            )
                            Text("Global")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedQuery == "Restaurante",
                                onClick = { selectedQuery = "Restaurante" }
                            )
                            Text("Por Restaurante")
                        }
                        if (selectedQuery == "Restaurante") {
                            Spacer(Modifier.height(8.dp))
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (selectedRestaurante.isEmpty()) "Seleccionar restaurante" else selectedRestaurante)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    restaurants.forEach { rest ->
                                        DropdownMenuItem(
                                            text = { Text(rest) },
                                            onClick = {
                                                selectedRestaurante = rest
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (selectedRestaurante.isEmpty()) {
                                Text("Selecciona un restaurante", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (selectedQuery == "Global" || (selectedQuery == "Restaurante" && selectedRestaurante.isNotEmpty())) {
                                showChart = true
                                showTop5Dialog = false
                            }
                        },
                        enabled = !isLoading && (selectedQuery == "Global" || (selectedQuery == "Restaurante" && selectedRestaurante.isNotEmpty()))
                    ) {
                        Text("Ver")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTop5Dialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    // Diálogo para crear nueva orden
    if (showCreateOrderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateOrderDialog = false },
            title = { Text("Crear Nueva Orden") },
            text = {
                Column {
                    if (hasActiveCart) {
                        Text("Ya tienes un pedido activo. Debes cerrar el pedido actual antes de crear uno nuevo.")
                    } else {
                        Text("¿Desea crear una nueva orden?")
                        Text("La orden expirará en 24 horas.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!hasActiveCart) {
                            currentUser?.email?.let { email ->
                                cartViewModel.createCart(email, emptyMap())
                                scope.launch {
                                    cartViewModel.loadActiveCartForUser(email)
                                }
                            }
                        }
                        showCreateOrderDialog = false
                    }
                ) {
                    Text(if (hasActiveCart) "Entendido" else "Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateOrderDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showChart) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Top5Chart(top5 = top5Comidas) {
                showChart = false
                selectedQuery = "Global"
                selectedRestaurante = ""
            }
        }
    }
}

@Composable
private fun ProfileItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}


@Composable
fun CartCard(cart: CartItemEntity, onDelete: (() -> Unit)? = null, menuViewModel: MenuViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cart.status == CartStatus.ACTIVE)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (cart.status == CartStatus.ACTIVE)
                            Icons.Default.ShoppingCart
                        else
                            Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (cart.status == CartStatus.ACTIVE)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Pedido #${cart.id.takeLast(4)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (cart.status == CartStatus.ACTIVE && onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar pedido",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Información del usuario
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cart.userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cart.creationDate.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Estado
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (cart.status == CartStatus.ACTIVE)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (cart.status == CartStatus.ACTIVE)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.Clear,
                        contentDescription = null,
                        tint = if (cart.status == CartStatus.ACTIVE)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (cart.status == CartStatus.ACTIVE) "Activo" else "Cerrado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (cart.status == CartStatus.ACTIVE)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items
            Text(
                text = "Items del Pedido",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            cart.items.forEach { (itemId, quantity) ->
                val menuItem by menuViewModel.fetchMenuItemById(itemId).collectAsState(initial = null)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.fastfood_24),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = menuItem?.name ?: "Cargando...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "x$quantity",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Total
            if (cart.total > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${cart.total}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun Top5Chart(top5: List<TopComidaResponse>, onClose: () -> Unit) {
    val colores = listOf(
        Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF2196F3),
        Color(0xFFF44336), Color(0xFF9C27B0)
    )
    val maxCantidad = top5.maxOfOrNull { it.cantidad } ?: 1

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Top 5 Comidas") },
        text = {
            if (top5.isEmpty()) {
                Text("No hay datos suficientes para mostrar el gráfico.")
            } else {
                Column {
                    top5.forEachIndexed { idx, comida ->
                        val animCantidad by animateFloatAsState(
                            targetValue = comida.cantidad.toFloat() / maxCantidad,
                            label = "BarAnim"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width((200 * animCantidad).dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(colores[idx % colores.size], colores[(idx + 1) % colores.size])
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${comida.nombre} (${comida.cantidad})")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Cerrar") }
        }
    )
} 