package com.jetbrains.greeting.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jetbrains.greeting.data.entitys.MenuItemEntity
import com.jetbrains.greeting.data.entitys.UserType
import com.jetbrains.greeting.viewmodels.MenuViewModel
import com.jetbrains.greeting.viewmodels.UserViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import com.preat.peekaboo.image.picker.FilterOptions
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap


@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateMenuItemDialog(
    onDismiss: () -> Unit,
    httpClient: HttpClient,
    menuViewModel: MenuViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel(),
    restrictedRestaurant: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by userViewModel.currentUser.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var preparationTime by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var featuresText by remember { mutableStateOf("") }
    var showNewRestaurantField by remember { mutableStateOf(false) }
    var newRestaurantName by remember { mutableStateOf("") }

    // Variables para la imagen
    val imageByteArray = remember { mutableStateOf<ByteArray?>(null) }
    val image = remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    // Características dinámicas
    val availableFeatures = listOf(
        "Vegetariano", 
        "Picante", 
        "Sin gluten", 
        "Sin lácteos", 
        "Sin frutos secos"
    )
    var selectedFeatures by remember { mutableStateOf<List<String>>(emptyList()) }

    // Restaurantes disponibles
    val restaurants by menuViewModel.restaurants.collectAsState()
    var selectedRestaurant by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Determinar el restaurante asignado al admin
    val adminRestaurant = currentUser?.restaurant ?: restrictedRestaurant

    // Determinar si el usuario es admin con acceso total
    val isAdminWithFullAccess = currentUser?.rol == UserType.ROLE_ADMIN.toString() && currentUser?.restaurant == "all"

    val singleImagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        resizeOptions = ResizeOptions(compressionQuality = 0.5),
        filterOptions = FilterOptions.Default,
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let {
                imageByteArray.value = it
                image.value = it.toImageBitmap()
            }
        },
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            val finalRestaurant = if (showNewRestaurantField) newRestaurantName else selectedRestaurant
                            if (finalRestaurant != null) {
                                // Primero creamos el item sin imagen
                                val item = MenuItemEntity(
                                    name = name,
                                    description = description,
                                    price = price.toInt(),
                                    imageUrl = null, // Inicialmente sin imagen
                                    category = category,
                                    restaurant = finalRestaurant,
                                    features = selectedFeatures,
                                    preparationTime = preparationTime.toIntOrNull() ?: 0,
                                    isVegetarian = selectedFeatures.contains("Vegetariano"),
                                    isSpicy = selectedFeatures.contains("Picante"),
                                    isGlutenFree = selectedFeatures.contains("Sin gluten"),
                                    isDairyFree = selectedFeatures.contains("Sin lácteos"),
                                    isNutFree = selectedFeatures.contains("Sin frutos secos")
                                )

                                menuViewModel.addMenuItemWithImage(item, imageByteArray.value) { success ->
                                    if (success) {
                                        println("✅ [CreateMenuItemDialog] Item creado con imagen")
                                        onDismiss()
                                    } else {
                                        println("❌ [CreateMenuItemDialog] Fallo al crear el item")
                                    }
                                }
                            } else {
                                println("❌ [CreateMenuItemDialog] Error: No se ha seleccionado un restaurante")
                            }
                        } catch (e: Exception) {
                            println("❌ [CreateMenuItemDialog] Error al crear item: ${e.message}")
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && description.isNotBlank() &&
                        price.isNotBlank() && preparationTime.isNotBlank() &&
                        category.isNotBlank() && imageByteArray.value != null && 
                        (selectedRestaurant != null || (showNewRestaurantField && newRestaurantName.isNotBlank()))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Añadir nuevo ítem") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    minLines = 2
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = preparationTime,
                    onValueChange = { preparationTime = it },
                    label = { Text("Tiempo de preparación (minutos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    singleLine = true
                )

                // Campo para características
                OutlinedTextField(
                    value = featuresText,
                    onValueChange = { 
                        featuresText = it
                        selectedFeatures = it.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    },
                    label = { Text("Características (separadas por comas)") },
                    singleLine = true
                )

                // Menú desplegable para restaurantes
                if (currentUser?.rol == UserType.ROLE_ADMIN.toString()) {
                    if (adminRestaurant == "all") {
                        // Admin con acceso total puede seleccionar cualquier restaurante
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedRestaurant ?: "",
                                onValueChange = { 
                                    selectedRestaurant = it
                                    showNewRestaurantField = true
                                },
                                label = { Text("Seleccionar o crear restaurante") },
                                trailingIcon = { 
                                    Row {
                                        if (selectedRestaurant?.isNotEmpty() == true) {
                                            IconButton(
                                                onClick = {
                                                    selectedRestaurant = ""
                                                    showNewRestaurantField = false
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Limpiar"
                                                )
                                            }
                                        }
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    }
                                },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                // Opción para crear nuevo restaurante
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text("Crear nuevo restaurante")
                                        }
                                    },
                                    onClick = {
                                        showNewRestaurantField = true
                                        expanded = false
                                    }
                                )
                                Divider()
                                restaurants.forEach { restaurant ->
                                    DropdownMenuItem(
                                        text = { Text(restaurant) },
                                        onClick = {
                                            selectedRestaurant = restaurant
                                            showNewRestaurantField = false
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (showNewRestaurantField) {
                            OutlinedTextField(
                                value = newRestaurantName,
                                onValueChange = { 
                                    newRestaurantName = it
                                    selectedRestaurant = it
                                },
                                label = { Text("Nombre del nuevo restaurante") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                singleLine = true
                            )
                        }
                    } else {
                        // Admin con restaurante específico
                        selectedRestaurant = adminRestaurant
                        Text(
                            text = "Restaurante: $adminRestaurant",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "Restaurante: ${restrictedRestaurant ?: currentUser?.restaurant}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Selección de características
                Column {
                    Text("Características")
                    availableFeatures.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedFeatures.contains(feature),
                                onCheckedChange = {
                                    selectedFeatures = if (it) {
                                        selectedFeatures + feature
                                    } else {
                                        selectedFeatures - feature
                                    }
                                }
                            )
                            Text(feature)
                        }
                    }
                }

                Button(
                    onClick = { singleImagePicker.launch() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seleccionar Imagen")
                }

                image.value?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    )
}


