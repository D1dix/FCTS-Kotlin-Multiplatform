package com.jetbrains.greeting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jetbrains.greeting.data.entitys.CartItemEntity
import com.jetbrains.greeting.data.entitys.CartStatus
import com.jetbrains.greeting.viewmodels.CartViewModel
import com.jetbrains.greeting.viewmodels.MenuViewModel
import com.jetbrains.greeting.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import com.jetbrains.greeting.components.StripePaymentForm
import com.jetbrains.greeting.data.remote.PedidoListadoDTO
import android.util.Log


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun CartScreen(
    onNavigateBack: () -> Unit,
    cartViewModel: CartViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel(),
    menuviewmodel: MenuViewModel = koinViewModel()
) {
    val activeCart by cartViewModel.activeCart.collectAsState()
    val userCarts by cartViewModel.userCarts.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showCloseCartDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val allMenuItems by menuviewmodel.menuItems.collectAsState()
    var showPaymentDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var ultimosPedidos by remember { mutableStateOf<List<PedidoListadoDTO>>(emptyList()) }

    LaunchedEffect(currentUser?.email) {
        currentUser?.email?.let { email ->
            isLoading = true
            try {
                cartViewModel.loadActiveCartForUser(email)
                cartViewModel.loadUserCarts(email)
                ultimosPedidos = cartViewModel.getUltimos5Pedidos(email)
                Log.d("CartScreen", "✅ Últimos 5 pedidos cargados: ${ultimosPedidos.size}")
            } catch (e: Exception) {
                Log.e("CartScreen", "❌ Error cargando datos: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carrito") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (activeCart != null) {
                        IconButton(onClick = { showHistoryDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Ver historial"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (activeCart == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay productos en el carrito",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ){
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pedido Actual") },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Historial") },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(
                                activeCart?.items?.toList() ?: emptyList()
                            ) { (itemId, quantity) ->
                                val menuItem = allMenuItems.find { it.id == itemId }
                                menuItem?.let { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = "Cantidad: $quantity",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = "Precio: $${item.price}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            cartViewModel.eliminarItemDelCarrito(
                                                                itemId
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Eliminar",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                cartViewModel.restarItemDelCarrito(
                                                                    itemId
                                                                )
                                                            }
                                                        }
                                                    ) {
                                                        Text("-")
                                                    }
                                                    Text(text = "$quantity")
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                cartViewModel.updateItemQuantity(
                                                                    itemId,
                                                                    quantity + 1
                                                                )
                                                            }
                                                        }
                                                    ) {
                                                        Text("+")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Total: $${activeCart?.total ?: 0}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { cartViewModel.updateCart() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text("Actualizar Pedido")
                                    }
                                    Button(
                                        onClick = { showCloseCartDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("Finalizar Pedido")
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    1 -> {
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (ultimosPedidos.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No hay pedidos en el historial",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(ultimosPedidos) { pedido ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
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
                                                        imageVector = Icons.Default.ShoppingCart,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "Pedido #${pedido.id.takeLast(4)}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = "$${pedido.cantidadFinal}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

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
                                                    text = pedido.fechaCreacion.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Items del Pedido",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            pedido.items.forEach { item ->
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
                                                            imageVector = Icons.Default.Home,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = item.nombreComida,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                    Text(
                                                        text = "x${item.cantidad}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    if (showCloseCartDialog) {
        AlertDialog(
            onDismissRequest = { showCloseCartDialog = false },
            title = { Text("Confirmar Pedido") },
            text = { Text("¿Estás seguro de que quieres realizar este pedido?") },
            confirmButton = {
                Button(
                    onClick = {
                        showCloseCartDialog = false
                        showPaymentDialog = true
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseCartDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showPaymentDialog) {
        activeCart?.let { cart ->
            StripePaymentDialog(
                amount = cart.total.toDouble(),
                cartId = cart.id,
                onDismiss = { showPaymentDialog = false },
                onPaymentSuccess = {
                    showPaymentDialog = false
                },
                cartViewModel = cartViewModel
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StripePaymentDialog(
    amount: Double,
    cartId: String,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit,
    cartViewModel: CartViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagar con tarjeta") },
        text = {
            StripePaymentForm(
                amount = amount,
                cartId = cartId,
                onDismiss = onDismiss,
                onPaymentSuccess = onPaymentSuccess,
                cartViewModel = cartViewModel
            )
        },
        confirmButton = {},
        dismissButton = {}
    )
}