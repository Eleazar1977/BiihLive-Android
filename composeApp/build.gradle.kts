import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.github.triplet.play")
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.gms.google-services")
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
            
            // Firebase para autenticación y Firestore (moved up)
            
            // ExoPlayer for video playback
            implementation("androidx.media3:media3-exoplayer:1.2.1")
            implementation("androidx.media3:media3-ui:1.2.1")
            implementation("androidx.media3:media3-common:1.2.1")
            
            // HTTP client
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
            
            // Coil for image loading
            implementation("io.coil-kt:coil-compose:2.5.0")
            
            // Ktor para cliente HTTP en chat
            implementation("io.ktor:ktor-client-core:2.3.5")
            implementation("io.ktor:ktor-client-android:2.3.5")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
            implementation("io.ktor:ktor-client-logging:2.3.5")
            
            // Accompanist para SwipeRefresh y otras utilidades
            implementation("com.google.accompanist:accompanist-swiperefresh:0.30.1")
            implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
            
            // Serialización
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            
            // Google Sign-In con Credential Manager para autenticación nativa
            implementation("com.google.android.gms:play-services-auth:21.2.0")
            implementation("androidx.credentials:credentials:1.2.2")
            implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

            // Firebase para autenticación y Firestore
            implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
            implementation("com.google.firebase:firebase-auth-ktx")
            implementation("com.google.firebase:firebase-firestore-ktx")
            implementation("com.google.firebase:firebase-functions-ktx")
            implementation("com.google.firebase:firebase-common-ktx")
            
            // Additional Android dependencies
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
            implementation("androidx.compose.runtime:runtime-livedata:1.7.5")

            // Palette API para extraer colores dominantes de imágenes
            implementation("androidx.palette:palette-ktx:1.0.0")
            
            // BlurHash para placeholders progresivos de imágenes
            implementation("com.vanniktech:blurhash:0.2.0")
            
            // AWS S3 only for media storage (keeping only S3)
            implementation("com.amazonaws:aws-android-sdk-core:2.77.0")
            implementation("com.amazonaws:aws-android-sdk-s3:2.77.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(projects.shared)
            
            // Navigation
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
            
            // Material 3
            implementation("androidx.compose.material3:material3:1.3.1")
            implementation("androidx.compose.material:material-icons-extended:1.7.5")
        }
    }
}

android {
    namespace = "com.mision.biihlive"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mision.biihlive"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = "1.4"
    }
    
    signingConfigs {
        getByName("debug") {
            // Usar el keystore específico de BiihLive para Google Sign-In
            storeFile = file("${System.getProperty("user.home")}/.android/biihlive_debug.keystore")
            storePassword = "android"
            keyAlias = "biihlivekey"
            keyPassword = "android"
        }
        create("release") {
            // Configuración temporal - vamos a generar el keystore
            storeFile = file("../biihlive-release.jks")
            storePassword = "biihlive123"
            keyAlias = "biihlive"
            keyPassword = "biihlive123"
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// Configuración de Compose Resources para multi-idioma
compose.resources {
    publicResClass = true
    packageOfResClass = "biihlive.composeapp.generated.resources"
    generateResClass = always
}

// Configuración para Google Play Console
play {
    // Cuando tengas el service-account.json, descomenta esta línea:
    // serviceAccountCredentials = file("../service-account.json")
    
    defaultToAppBundles.set(true)
    track.set("internal")  // Prueba cerrada (internal testing)
    
    // Los testers se configuran directamente en Google Play Console
    // en la sección de Internal Testing, no desde el archivo gradle
    
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
}

