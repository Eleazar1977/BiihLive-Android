package com.mision.biihlive.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Initial : Screen("initial")
    object SignUp : Screen("signup")
    object SignIn : Screen("signin")
    object SigningIn : Screen("signing_in")
    object NativeGoogleSignIn : Screen("native_google_signin")
    object Confirmation : Screen("confirmation/{email}") {
        fun createRoute(email: String) = "confirmation/${java.net.URLEncoder.encode(email, "UTF-8")}"
    }
    object EmailVerification : Screen("email_verification")
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password/{email}") {
        fun createRoute(email: String) = "reset_password/${java.net.URLEncoder.encode(email, "UTF-8")}"
    }
    object PasswordRecovery : Screen("password_recovery")
    object PasswordRecoveryTemp : Screen("password_recovery_temp")
    object PhoneVerification : Screen("phone_verification")
    object Logout : Screen("logout")
    object Home : Screen("home?initialTab={initialTab}") {
        fun createRoute(initialTab: Int = 1) = "home?initialTab=$initialTab"
    }
    object PerfilUsuario : Screen("perfil_personal_logueado")
    object EditarPerfil : Screen("editar_perfil")
    object Suscripciones : Screen("suscripciones")
    object ConfiguracionSuscripcion : Screen("configuracion_suscripcion")
    object Patrocinios : Screen("patrocinios")
    object ConfiguracionPatrocinio : Screen("configuracion_patrocinio")
    object PerfilConsultado : Screen("perfil_publico_consultado/{userId}?returnTo={returnTo}&photoIndex={photoIndex}") {
        fun createRoute(
            userId: String,
            returnTo: String? = null,
            photoIndex: Int? = null
        ) = buildString {
            append("perfil_publico_consultado/$userId")
            val params = mutableListOf<String>()
            if (returnTo != null) params.add("returnTo=$returnTo")
            params.add("photoIndex=${photoIndex ?: -1}")
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
    }
    object Patrocinar : Screen("patrocinar/{userId}") {
        fun createRoute(userId: String) = "patrocinar/$userId"
    }
    object Suscripcion : Screen("suscripcion/{userId}") {
        fun createRoute(userId: String) = "suscripcion/$userId"
    }
    object Ajustes : Screen("ajustes")
    object AccountSettings : Screen("account_settings")

    // Social screens
    object FollowersFollowing : Screen("followers_following/{userId}/{initialTab}") {
        fun createRoute(userId: String, initialTab: Int) = "followers_following/$userId/$initialTab"
    }

    object Grupos : Screen("grupos/{userId}") {
        fun createRoute(userId: String) = "grupos/$userId"
    }

    // Ranking screens
    object Ranking : Screen("ranking?userId={userId}&initialTab={initialTab}") {
        fun createRoute(userId: String? = null, initialTab: Int? = null) = buildString {
            append("ranking")
            val params = mutableListOf<String>()
            if (userId != null) params.add("userId=$userId")
            // Usar -1 como valor por defecto cuando initialTab es null
            params.add("initialTab=${initialTab ?: -1}")
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
    }

    // Gallery screens
    object PhotoFeed : Screen("photo_feed?userId={userId}&initialIndex={initialIndex}") {
        fun createRoute(userId: String? = null, initialIndex: Int = 0) = buildString {
            append("photo_feed")
            val params = mutableListOf<String>()
            if (userId != null) params.add("userId=$userId")
            if (initialIndex > 0) params.add("initialIndex=$initialIndex")
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
    }

    // Chat screens
    object UsersSearch : Screen("users_search?mode={mode}") {
        fun createRoute(mode: String = "profile") = "users_search?mode=$mode"
    }
    object MessagesList : Screen("messages_list")
    object Chat : Screen("chat/{chatId}/{displayName}") {
        fun createRoute(chatId: String, displayName: String) = "chat/$chatId/${java.net.URLEncoder.encode(displayName, "UTF-8")}"
    }

    // Payment screens
    object PaymentFlow : Screen("payment_flow/{creatorId}/{paymentType}") {
        fun createRoute(creatorId: String, paymentType: String = "subscription") = "payment_flow/$creatorId/$paymentType"
    }
    object PaymentMethods : Screen("payment_methods")
    object AddPaymentMethod : Screen("add_payment_method?provider={provider}") {
        fun createRoute(provider: String = "mock") = "add_payment_method?provider=$provider"
    }
    object PaymentProcessing : Screen("payment_processing/{paymentId}") {
        fun createRoute(paymentId: String) = "payment_processing/$paymentId"
    }
    object PaymentSuccess : Screen("payment_success/{subscriptionId}") {
        fun createRoute(subscriptionId: String) = "payment_success/$subscriptionId"
    }
}