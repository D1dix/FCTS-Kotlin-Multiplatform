package com.jetbrains.greeting

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.compose.foundation.Image
import coil3.compose.AsyncImage
import com.jetbrains.greeting.data.entitys.UserType
import com.jetbrains.greeting.viewmodels.LoginState
import com.jetbrains.greeting.viewmodels.UserViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.jetbrains.greeting.viewmodels.MenuViewModel
import com.preat.peekaboo.image.picker.FilterOptions
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: UserViewModel = koinViewModel(),
    menuViewModel: MenuViewModel = koinViewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contrasenia by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserType.ROLE_USER.toString()) }
    var selectedRestaurant by remember { mutableStateOf("") }
    var showRestaurantInput by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.collectAsState()
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val restaurants by menuViewModel.restaurants.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }

    val coroutineScope = rememberCoroutineScope()
    val imageByteArray = remember { mutableStateOf<ByteArray?>(null) }
    val image = remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    val singleImagePicker =
        rememberImagePickerLauncher(
            selectionMode = SelectionMode.Single,
            scope = coroutineScope,
            resizeOptions = ResizeOptions(compressionQuality = 0.5),
            filterOptions = FilterOptions.Default,
            onResult = { byteArrays ->
                byteArrays.firstOrNull()?.let {
                    imageByteArray.value = it
                    image.value = it.toImageBitmap()
                }
            },
        )

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                imageByteArray.value?.let { bytes ->
                    viewModel.uploadProfileImage(email, bytes)
                }
                onRegisterSuccess()
            }
            is LoginState.Error -> {
                showError = true
                errorMessage = (loginState as LoginState.Error).message
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { singleImagePicker.launch() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (image.value != null) {
                    Image(
                        bitmap = image.value!!,
                        contentDescription = "Imagen de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Crear Cuenta",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Regístrate para comenzar",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = contrasenia,
                onValueChange = { contrasenia = it },
                label = { Text("Contraseña") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = "Toggle password visibility",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedRole = UserType.ROLE_USER.toString()
                        showRestaurantInput = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedRole == UserType.ROLE_USER.toString())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Usuario")
                }

                OutlinedButton(
                    onClick = {
                        selectedRole = UserType.ROLE_ADMIN.toString()
                        showRestaurantInput = true
                        selectedRestaurant = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedRole == UserType.ROLE_ADMIN.toString())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin")
                }
            }

            if (showRestaurantInput) {
                OutlinedTextField(
                    value = selectedRestaurant,
                    onValueChange = { selectedRestaurant = it },
                    label = { Text("Restaurante (dejar vacío para acceso total)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (selectedRestaurant.isNotBlank()) {
                            IconButton(onClick = { selectedRestaurant = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar"
                                )
                            }
                        }
                    }
                )

                if (selectedRestaurant.isNotBlank() && !restaurants.contains(selectedRestaurant)) {
                    Text(
                        text = "El restaurante no existe en la lista",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (showError) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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

            Button(
                onClick = {
                    if (email.isNotBlank() && contrasenia.isNotBlank() && nombre.isNotBlank()) {
                        if (selectedRole == UserType.ROLE_ADMIN.toString()) {
                            if (selectedRestaurant.isBlank()) {
                                viewModel.register(
                                    email = email,
                                    contrasenia = contrasenia,
                                    nombre = nombre,
                                    role = selectedRole,
                                    restaurant = "all",
                                    context = context,
                                    imageBytes = imageByteArray.value
                                )

                            } else if (restaurants.contains(selectedRestaurant)) {
                                viewModel.register(
                                    email = email,
                                    contrasenia = contrasenia,
                                    nombre = nombre,
                                    role = selectedRole,
                                    restaurant = selectedRestaurant,
                                    context = context,
                                    imageBytes = imageByteArray.value
                                )

                            } else {
                                showError = true
                                errorMessage = "El restaurante seleccionado no existe"
                            }
                        } else {
                            viewModel.register(
                                email = email,
                                contrasenia = contrasenia,
                                nombre = nombre,
                                role = selectedRole,
                                context = context
                            )
                        }
                    } else {
                        showError = true
                        errorMessage = "Por favor, completa todos los campos"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = loginState !is LoginState.Loading
            ) {
                if (loginState is LoginState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Registrarse")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Ya tienes cuenta?",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        "Inicia sesión",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
