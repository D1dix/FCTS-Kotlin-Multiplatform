package com.jetbrains.greeting.di

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.jetbrains.greeting.data.local.AppDatabase
import com.jetbrains.greeting.data.local.CartItemDao
import com.jetbrains.greeting.data.local.CommentDao
import com.jetbrains.greeting.data.local.MenuItemDao

import com.jetbrains.greeting.data.local.UserDao
import com.jetbrains.greeting.data.remote.CartService
import com.jetbrains.greeting.data.remote.MenuService
import com.jetbrains.greeting.data.remote.CommentService
import com.jetbrains.greeting.data.remote.PaymentService
import com.jetbrains.greeting.data.remote.UserService
import com.jetbrains.greeting.data.repositories.CartRepository
import com.jetbrains.greeting.data.repositories.CommentRepository
import com.jetbrains.greeting.data.repositories.MenuRepository

import com.jetbrains.greeting.data.repositories.UserRepository
import com.jetbrains.greeting.viewmodels.CartViewModel
import com.jetbrains.greeting.viewmodels.CommentViewModel
import com.jetbrains.greeting.viewmodels.MenuViewModel

import com.jetbrains.greeting.viewmodels.UserViewModel
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit
import com.jetbrains.greeting.data.repository.ChatRepository
import com.jetbrains.greeting.data.service.ChatService
import com.jetbrains.greeting.viewmodels.ChatViewModel

@RequiresApi(Build.VERSION_CODES.O)
val appModule = module {

    // Constantes
    single { "http://192.168.1.139:8080" } // Reemplaza con tu URL del backend

    // Room Database
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration() // Solo para desarrollo
            .build()
    }

    // DAOs
    single<CartItemDao> { get<AppDatabase>().cartItemDao() }
    single<CommentDao> { get<AppDatabase>().commentDao() }
    single<MenuItemDao> { get<AppDatabase>().menuItemDao() }
    single<UserDao> { get<AppDatabase>().userDao() }

    // HttpClient para el backend
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    // Servicios remotos
    single { UserService(get(), get()) }
    single { CommentService(get(), get()) }
    single { CartService(get(), get()) }
    single { MenuService(get(), get()) }
    single { PaymentService(get(), get()) }
    single { ChatService(get(), get()) }

    // Repositories
    single { CartRepository(get(), get(),get(), get()) }
    single { CommentRepository(get(), get(), get()) }
    single { MenuRepository(get(), get()) }
    single { UserRepository(get(), get()) }
    single { ChatRepository(get()) }

    // ViewModels
    viewModel { MenuViewModel(get(), get(), get()) }
    viewModel { CartViewModel(get(), get()) }
    viewModel { CommentViewModel(get(), get()) }
    viewModel { UserViewModel(get()) }
    single { ChatViewModel(get()) }

}
