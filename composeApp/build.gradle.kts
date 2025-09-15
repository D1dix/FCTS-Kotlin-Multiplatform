import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidxRoom)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.sqlite.bundled)
            implementation("com.google.code.gson:gson:2.10.1")
            implementation(libs.peekaboo.image.picker)
            implementation(libs.peekaboo.ui)
            implementation("com.stripe:stripe-android:20.39.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.ktor.client.contentnegotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.plugins)
            implementation(libs.ktor.client.core.jvm)
            implementation(libs.androidx.navigation.compose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.room.runtime)

        }
    }

    sourceSets.commonMain{
        kotlin.srcDir("build/generated/ksp/metadata")
    }
}

android {
    namespace = "com.jetbrains.greeting"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jetbrains.greeting"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.ui.android)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.core)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.ui.test.android)
    implementation(libs.volley)
    implementation(libs.firebase.perf.ktx)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.firebase.dataconnect)
    debugImplementation(compose.uiTooling)
    ksp(libs.androidx.room.compiler.get())
    ksp("com.google.code.gson:gson:2.10.1")
    implementation("org.json:json:20231013")
}

/*tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach{
    if(name != "kspCommonMainKotlinMetaData"){
        dependsOn("kspCommonMainKotlinMetaData")
    }
}*/

room {
    schemaDirectory("$projectDir/schemas")
}
