package com.mision.biihlive.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mision.biihlive.screens.*
import com.mision.biihlive.presentation.auth.screens.EmailVerificationScreen
import com.mision.biihlive.presentation.auth.screens.PasswordRecoveryScreen
import com.mision.biihlive.presentation.auth.screens.PasswordRecoveryScreenTemp
import com.mision.biihlive.viewmodels.FirebaseAuthViewModel
import com.mision.biihlive.viewmodels.AuthState
import com.mision.biihlive.presentation.chat.providers.GlobalChatProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.net.URLDecoder

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: FirebaseAuthViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Solo envolver con provider de chat si hay usuario autenticado
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    if (isAuthenticated) {
        GlobalChatProvider(context = context) {
            AppNavigationContent(
                navController = navController,
                authViewModel = authViewModel,
                context = context
            )
        }
    } else {
        AppNavigationContent(
            navController = navController,
            authViewModel = authViewModel,
            context = context
        )
    }
}

@Composable
private fun AppNavigationContent(
    navController: NavHostController,
    authViewModel: FirebaseAuthViewModel,
    context: android.content.Context
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    var isInitialized by remember { mutableStateOf(false) }

    // Hacer que hasGoogleSession sea reactivo usando un estado
    var hasGoogleSession by remember { mutableStateOf(com.mision.biihlive.utils.SessionManager.isLoggedIn(context)) }

    // Actualizar hasGoogleSession cuando cambie isAuthenticated
    LaunchedEffect(isAuthenticated) {
        hasGoogleSession = com.mision.biihlive.utils.SessionManager.isLoggedIn(context)
        Log.d("AppNavigation", "Actualizado hasGoogleSession: $hasGoogleSession después de cambio en isAuthenticated: $isAuthenticated")
    }

    LaunchedEffect(Unit) {
        delay(500) // Minimum splash time (reducido para mejor UX)
        isInitialized = true
    }

    LaunchedEffect(isAuthenticated, isInitialized, hasGoogleSession) {
        Log.d("AppNavigation", "Auth: $isAuthenticated, Init: $isInitialized, GoogleSession: $hasGoogleSession")
        if (isInitialized) {
            val currentDestination = navController.currentDestination?.route
            Log.d("AppNavigation", "Current destination: $currentDestination")

            if (isAuthenticated || hasGoogleSession) {
                // Usuario autenticado - ir a Home si está en pantalla de auth
                val authScreens = setOf(
                    Screen.SignIn.route,
                    Screen.SignUp.route,
                    Screen.ForgotPassword.route,
                    Screen.PasswordRecovery.route,
                    Screen.PasswordRecoveryTemp.route,
                    Screen.Splash.route
                )

                val isInAuthScreen = currentDestination?.let { dest ->
                    authScreens.contains(dest) ||
                    dest.startsWith(Screen.EmailVerification.route)
                } ?: true // Si currentDestination es null, asumir que está en auth screen

                if (isInAuthScreen) {
                    Log.d("AppNavigation", "Usuario autenticado, navegando a Home desde $currentDestination")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                // Usuario NO autenticado - ir a SignIn
                val authScreens = setOf(
                    Screen.SignIn.route,
                    Screen.SignUp.route,
                    Screen.ForgotPassword.route,
                    Screen.PasswordRecovery.route,
                    Screen.PasswordRecoveryTemp.route
                )

                val isAlreadyInAuthFlow = currentDestination?.let { dest ->
                    authScreens.any { authScreen -> dest.startsWith(authScreen) } ||
                    dest.startsWith(Screen.EmailVerification.route)
                } ?: false

                if (!isAlreadyInAuthFlow) {
                    Log.d("AppNavigation", "Usuario NO autenticado, navegando a SignIn desde $currentDestination")
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        
        composable(Screen.Splash.route) {
            SplashScreen()
        }


        composable(Screen.SignUp.route) {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                onNavigateToConfirmation = { email ->
                    navController.navigate(Screen.EmailVerification.route)
                }
            )
        }

        composable(Screen.SignIn.route) {
            SignInScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.PasswordRecovery.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onNavigateToPhoneVerification = {
                    navController.navigate(Screen.PhoneVerification.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onNavigateToSigningIn = {
                    // Cambiar a autenticación nativa en lugar de SigningIn
                    navController.navigate(Screen.NativeGoogleSignIn.route)
                }
            )
        }

        composable(Screen.NativeGoogleSignIn.route) {
            NativeGoogleAccountPicker(
                onAccountSelected = { accountName ->
                    Log.d("AppNavigation", "Cuenta Google seleccionada: $accountName")
                    // Guardar el email de Google e integrar con Firebase Auth
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Removed AWS Cognito/Amplify SigningIn screen - replaced with Firebase Auth

        composable(Screen.EmailVerification.route) {
            val authState by authViewModel.authState.collectAsState()
            val currentUser = FirebaseAuth.getInstance().currentUser

            // Obtener email y userId del estado de autenticación
            val currentAuthState = authState  // Extract to local variable for smart cast
            val userEmail = when (currentAuthState) {
                is AuthState.SignUpRequiresConfirmation -> currentAuthState.username
                else -> currentUser?.email ?: ""
            }
            val userId = currentUser?.uid ?: ""

            EmailVerificationScreen(
                userEmail = userEmail,
                userId = userId,
                onVerificationComplete = {
                    // Llamar a completeEmailVerification para crear perfil y completar autenticación
                    authViewModel.completeEmailVerification()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onNavigateToResetPassword = { email ->
                    navController.navigate(Screen.ResetPassword.createRoute(email))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedEmail = backStackEntry.arguments?.getString("email") ?: ""
            val email = URLDecoder.decode(encodedEmail, "UTF-8")
            ResetPasswordScreen(
                email = email,
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PasswordRecovery.route) {
            PasswordRecoveryScreen(
                onRecoveryComplete = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.PasswordRecovery.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PasswordRecoveryTemp.route) {
            PasswordRecoveryScreenTemp(
                onRecoveryComplete = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.PasswordRecoveryTemp.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Logout.route) {
            LogoutScreen()
            LaunchedEffect(Unit) {
                delay(500) // Show logout screen briefly
                // Clear Firebase session
                authViewModel.signOut()
                delay(500) // Wait for signOut to complete
                navController.navigate(Screen.SignIn.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        composable(
            route = Screen.Home.route,
            arguments = listOf(
                navArgument("initialTab") {
                    type = NavType.IntType
                    defaultValue = 1
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 1
            val application = (context.applicationContext as android.app.Application)
            val chatRepository = remember {
                com.mision.biihlive.data.chat.repository.ChatFirestoreRepository(context)
            }

            // Obtener userId actual
            val currentUserId = remember {
                com.mision.biihlive.utils.SessionManager.getUserId(context)
            }

            // Observar mensajes no leídos
            val unreadCount by remember(currentUserId) {
                if (currentUserId != null) {
                    chatRepository.observeUnreadCount(currentUserId)
                } else {
                    kotlinx.coroutines.flow.flowOf(0)
                }
            }.collectAsState(initial = 0)

            HomeScreen(
                onNavigateToInitial = {
                    navController.navigate(Screen.Logout.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.PerfilUsuario.route)
                },
                onNavigateToUsersSearch = {
                    navController.navigate(Screen.UsersSearch.route)
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.MessagesList.route)
                },
                onNavigateToRanking = {
                    navController.navigate(Screen.Ranking.createRoute())
                },
                onNavigateToUserProfile = { userId, photoIndex ->
                    navController.navigate(Screen.PerfilConsultado.createRoute(
                        userId = userId,
                        returnTo = "photo_feed",
                        photoIndex = photoIndex
                    ))
                },
                unreadMessagesCount = unreadCount
            )
        }
        
        composable(Screen.PerfilUsuario.route) {
            val application = (context.applicationContext as android.app.Application)
            val perfilViewModel = remember {
                com.mision.biihlive.di.PerfilModule.providePerfilPersonalLogueadoViewModel(application)
            }
            com.mision.biihlive.presentation.perfil.PerfilPersonalLogueadoScreen(
                navController = navController,
                viewModel = perfilViewModel
            )
        }

        composable(Screen.EditarPerfil.route) {
            com.mision.biihlive.presentation.perfil.EditarPerfilScreen(
                navController = navController
            )
        }

        composable(Screen.Suscripciones.route) {
            com.mision.biihlive.presentation.suscripciones.screens.ListSuscripcionesScreen(
                navController = navController
            )
        }

        composable(Screen.ConfiguracionSuscripcion.route) {
            com.mision.biihlive.presentation.suscripciones.screens.ConfiguracionSuscripcionScreen(
                navController = navController
            )
        }

        // ✅ PANTALLAS DE PATROCINIOS - IMPLEMENTADAS COMPLETAMENTE
        composable(Screen.Patrocinios.route) {
            com.mision.biihlive.presentation.patrocinios.screens.ListPatrociniosScreen(
                navController = navController
            )
        }

        composable(Screen.ConfiguracionPatrocinio.route) {
            com.mision.biihlive.presentation.patrocinios.screens.ConfiguracionPatrocinioScreen(
                navController = navController
            )
        }

        composable(
            route = Screen.PerfilConsultado.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("returnTo") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("photoIndex") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val returnTo = backStackEntry.arguments?.getString("returnTo")
            val photoIndexArg = backStackEntry.arguments?.getInt("photoIndex", -1) ?: -1
            val photoIndex = if (photoIndexArg == -1) null else photoIndexArg
            val application = (context.applicationContext as android.app.Application)
            val perfilViewModel = remember {
                com.mision.biihlive.di.PerfilModule.providePerfilPublicoConsultadoViewModel(application)
            }

            LaunchedEffect(userId) {
                perfilViewModel.cargarPerfilDeUsuario(userId)
            }

            // Handle back navigation to preserve feed context
            LaunchedEffect(Unit) {
                // Log the navigation context for debugging
                Log.d("Navigation", "📱 Navegación al perfil - returnTo: $returnTo, photoIndex: $photoIndex")
            }

            com.mision.biihlive.presentation.perfil.PerfilPublicoConsultadoScreen(
                navController = navController,
                viewModel = perfilViewModel
            )
        }

        // TODO: Implementar pantalla de patrocinio cuando sea necesario
        // composable(
        //     route = Screen.Patrocinar.route,
        //     arguments = listOf(navArgument("userId") { type = NavType.StringType })
        // ) { backStackEntry ->
        //     val userId = backStackEntry.arguments?.getString("userId") ?: ""
        //     com.mision.biihlive.presentation.patrocinios.screens.PatrocinioSelectionScreen(
        //         navController = navController,
        //         userId = userId
        //     )
        // }

        composable(
            route = Screen.Suscripcion.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.mision.biihlive.presentation.suscripcion.screens.SuscripcionScreen(
                navController = navController,
                userId = userId
            )
        }

        // TODO: Implementar payment screens cuando sea necesario
        // composable(
        //     route = Screen.PaymentFlow.route,
        //     arguments = listOf(
        //         navArgument("creatorId") { type = NavType.StringType },
        //         navArgument("paymentType") {
        //             type = NavType.StringType
        //             defaultValue = "subscription"
        //         }
        //     )
        // ) { backStackEntry ->
        //     val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
        //     val paymentTypeStr = backStackEntry.arguments?.getString("paymentType") ?: "subscription"
        //     com.mision.biihlive.presentation.payments.screens.PaymentFlowScreen(
        //         creatorId = creatorId,
        //         paymentType = paymentTypeStr,
        //         navController = navController
        //     )
        // }

        // composable(Screen.PaymentMethods.route) {
        //     // TODO: Implementar PaymentMethodsScreen cuando sea necesario
        //     // com.mision.biihlive.presentation.payments.screens.PaymentMethodsScreen(
        //     //     navController = navController
        //     // )
        // }

        // composable(
        //     route = Screen.AddPaymentMethod.route,
        //     arguments = listOf(navArgument("provider") {
        //         type = NavType.StringType
        //         nullable = true
        //         defaultValue = "card"
        //     })
        // ) { backStackEntry ->
        //     com.mision.biihlive.presentation.payments.screens.AddPaymentMethodScreen(
        //         navController = navController,
        //         onPaymentMethodAdded = { paymentMethod ->
        //             // El callback se maneja dentro de la pantalla
        //             // La navegación de vuelta es automática
        //         }
        //     )
        // }

        composable(
            route = Screen.PaymentProcessing.route,
            arguments = listOf(navArgument("paymentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val paymentId = backStackEntry.arguments?.getString("paymentId") ?: ""
            // TODO: Implementar PaymentProcessingScreen independiente si es necesario
            // Por ahora se maneja dentro de PaymentFlowScreen
        }

        composable(
            route = Screen.PaymentSuccess.route,
            arguments = listOf(navArgument("subscriptionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subscriptionId = backStackEntry.arguments?.getString("subscriptionId") ?: ""
            // TODO: Implementar PaymentSuccessScreen independiente si es necesario
            // Por ahora se maneja dentro de PaymentFlowScreen
        }

        composable(
            route = Screen.Ranking.route,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("initialTab") {
                    type = NavType.IntType
                    nullable = false
                    defaultValue = -1  // -1 indica que debe usar el tab por defecto
                }
            )
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("userId")
            val initialTabRaw = backStackEntry.arguments?.getInt("initialTab") ?: -1
            val initialTab = if (initialTabRaw == -1) null else initialTabRaw
            com.mision.biihlive.presentation.ranking.screens.RankingScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() },
                targetUserId = targetUserId,
                initialTab = initialTab
            )
        }

        composable(
            route = Screen.FollowersFollowing.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("initialTab") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
            val application = (context.applicationContext as android.app.Application)

            val viewModel = remember {
                com.mision.biihlive.presentation.social.viewmodel.FollowersFollowingViewModel(
                    userId = userId,
                    initialTab = initialTab,
                    firestoreRepository = com.mision.biihlive.data.repository.FirestoreRepository(),
                    sessionManager = com.mision.biihlive.utils.SessionManager,
                    context = context
                )
            }

            com.mision.biihlive.presentation.social.screens.FollowersFollowingScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.Grupos.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val currentUserId = com.mision.biihlive.utils.SessionManager.getUserId(context)

            val viewModel = remember {
                com.mision.biihlive.presentation.grupos.viewmodel.GruposViewModel()
            }

            LaunchedEffect(userId) {
                viewModel.loadGrupos(userId, currentUserId)
            }

            com.mision.biihlive.presentation.grupos.screens.GruposScreen(
                navController = navController,
                userId = userId,
                viewModel = viewModel
            )
        }

        composable(Screen.Ajustes.route) {
            AjustesScreen(
                navController = navController,
                onLogout = {
                    Log.d("AppNavigation", "=== LOGOUT DESDE AJUSTES ===")
                    // Limpiar sesiones y navegar inmediatamente
                    authViewModel.signOut()
                    // Navegar inmediatamente a SignIn
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // TODO: Implementar AccountSettingsScreen cuando sea necesario
        // composable(Screen.AccountSettings.route) {
        //     com.mision.biihlive.presentation.settings.screens.AccountSettingsScreen(
        //         navController = navController,
        //         onAccountDeleted = {
        //             // Navegar al login después de eliminar cuenta
        //             navController.navigate(Screen.SignIn.route) {
        //                 popUpTo(0) { inclusive = true }
        //             }
        //         },
        //         authViewModel = authViewModel
        //     )
        // }

        // Users Search screen (moved from chat section)
        composable(Screen.UsersSearch.route) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "profile"
            val application = (context.applicationContext as android.app.Application)
            val viewModel = remember {
                com.mision.biihlive.presentation.users.viewmodel.UsersSearchViewModel(application)
            }
            com.mision.biihlive.presentation.users.screens.UsersSearchScreen(
                navController = navController,
                viewModel = viewModel,
                mode = mode
            )
        }

        // Gallery screens
        composable(
            route = Screen.PhotoFeed.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType; nullable = true },
                navArgument("initialIndex") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.takeIf { it != "null" }
            val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0

            // Verificar si es la galería personal del usuario actual
            var currentUserId by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                try {
                    val userIdManager = com.mision.biihlive.core.managers.UserIdManager.getInstance(context)
                    currentUserId = userIdManager.getCurrentUserId()
                } catch (e: Exception) {
                    currentUserId = null
                }
            }

            val isPersonalGallery = userId != null && userId == currentUserId

            // Si es galería personal, obtener el ViewModel para las acciones de edición/eliminación
            val perfilViewModel = if (isPersonalGallery) {
                remember {
                    val application = (context.applicationContext as android.app.Application)
                    com.mision.biihlive.di.PerfilModule.providePerfilPersonalLogueadoViewModel(application)
                }
            } else null

            presentation.components.PhotoFeed(
                modifier = Modifier.fillMaxSize()
            )
        }

        // Chat screens
        composable(Screen.MessagesList.route) {
            val messagesListViewModel = remember {
                com.mision.biihlive.presentation.chat.viewmodel.MessagesListViewModel(context)
            }
            com.mision.biihlive.presentation.chat.screens.MessageListScreen(
                navController = navController,
                viewModel = messagesListViewModel
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val encodedDisplayName = backStackEntry.arguments?.getString("displayName") ?: ""
            val displayName = URLDecoder.decode(encodedDisplayName, "UTF-8")

            val chatViewModel = remember(chatId) {
                com.mision.biihlive.presentation.chat.viewmodel.ChatViewModel(
                    chatId = chatId,
                    context = context
                )
            }

            com.mision.biihlive.presentation.chat.screens.ChatScreen(
                chatId = chatId,
                displayName = displayName,
                navController = navController,
                viewModel = chatViewModel
            )
        }
    }
}