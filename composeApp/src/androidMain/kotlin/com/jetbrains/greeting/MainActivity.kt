package com.jetbrains.greeting

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import com.jetbrains.greeting.components.LoadingScreen
import com.jetbrains.greeting.di.appModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.PaymentConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        

        enableEdgeToEdge()
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@MainActivity)
            modules(appModule)
        }

        PaymentConfiguration.init(
        context = applicationContext,
        publishableKey = "pk_test_51RRZ062cdmCzxJrf4ullXjgQ8CRmXXhGxDZ9rJTc1hmV2J91kzAGeBJBKzSdgKlzE6N7BBMQJN6IX98kwhXoyT0E00DoxIWgOx"
        ) 
        
        setContent {
            EnableTransparentStatusBar()
            App()
        }
    }


}

@Composable
fun EnableTransparentStatusBar() {
    val darkMode = isSystemInDarkTheme()
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()

        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkMode
        insetsController.isAppearanceLightNavigationBars = !darkMode
        
        // Configurar el comportamiento del teclado
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}