package com.jetbrains.greeting

import android.database.sqlite.SQLiteDatabase.deleteDatabase
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jetbrains.greeting.components.LoadingScreen
import kotlinx.coroutines.delay
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.jetbrains.greeting.di.appModule
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.dsl.KoinAppDeclaration

import proyectorest.composeapp.generated.resources.Res
import proyectorest.composeapp.generated.resources.compose_multiplatform

@OptIn(ExperimentalCoilApi::class)
@Composable
@Preview
fun App() {
    var isLoading by remember { mutableStateOf(true) }
    val navController = rememberNavController()

    // La función LaunchedEffect garantiza que el código solo se ejecute una vez, al cargar la pantalla
    LaunchedEffect(Unit) {
        delay(2000) // Simula una carga
        isLoading = false
    }
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).crossfade(true).logger(DebugLogger()).build()
    }
    // La comprobación del tema se hace aquí
    AppTheme {
        if (isLoading) {
            LoadingScreen(message = "Cargando la aplicación...")
        } else {
            AppNavHost()
        }
    }
}



