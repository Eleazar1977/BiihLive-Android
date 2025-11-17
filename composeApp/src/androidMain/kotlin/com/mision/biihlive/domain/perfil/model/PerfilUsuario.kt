package com.mision.biihlive.domain.perfil.model

// Estructura basada en APK decompilado
data class SubscriptionOption(
    val id: String = java.util.UUID.randomUUID().toString(),
    val price: String,
    val duration: String,
    val durationInDays: Int,
    val displayName: String = "",
    val isActive: Boolean = true
)

data class SubscriptionConfig(
    val isEnabled: Boolean = false,                // Si acepta suscripciones
    val currency: String = "€",                    // Moneda
    val description: String = "¡Únete a mi mundo en Biihlive! Suscríbete y no te pierdas ninguna de mis transmisiones en vivo.", // Descripción personalizable
    val options: List<SubscriptionOption> = listOf(
        SubscriptionOption(
            price = "9.99",
            duration = "1 mes",
            durationInDays = 30,
            displayName = "Plan Mensual"
        )
    )
)

data class PatrocinioConfig(
    val isEnabled: Boolean = false,                // Si acepta patrocinios
    val currency: String = "€",                    // Moneda
    val description: String = "¡Patrocina mi contenido en Biihlive! Ayúdame a seguir creando y forma parte de mi comunidad exclusiva.", // Descripción personalizable
    val options: List<SubscriptionOption> = listOf(
        SubscriptionOption(
            price = "19.99",
            duration = "1 mes",
            durationInDays = 30,
            displayName = "Plan Patrocinio Mensual"
        )
    )
)

data class PerfilUsuario(
    val userId: String,
    val nickname: String,
    val fullName: String,
    val description: String,
    val totalScore: Int,
    val tipo: String, // persona, empresa, etc
    val ubicacion: Ubicacion,
    val rankingPreference: String,
    val createdAt: Long,
    val photoUrl: String? = null, // URL legacy, se intentará cargar desde CloudFront primero
    val email: String? = null,
    val isVerified: Boolean? = false, // Estado de verificación del usuario

    // Datos calculados o adicionales
    val nivel: Int = 1,
    val seguidores: Int = 0,
    val siguiendo: Int = 0,
    val fotosCount: Int = 0,
    val videosCount: Int = 0,
    val rankingLocal: Int = 0,
    val rankingProvincial: Int = 0,
    val rankingNacional: Int = 0,
    val rankingMundial: Int = 0,

    // Estado de donación/ayuda
    val donacion: Boolean = false,  // true = mostrar "Donar", false = mostrar "Ayuda"

    // Control de privacidad - mostrar estado en línea
    val mostrarEstado: Boolean = true,  // true = mostrar estado en línea, false = ocultar

    // Configuración de suscripciones
    val subscriptionConfig: SubscriptionConfig = SubscriptionConfig(),

    // Configuración de patrocinios
    val patrocinioConfig: PatrocinioConfig = PatrocinioConfig()
)

data class Ubicacion(
    // Campos principales - estructura simplificada de Firestore
    val ciudad: String,
    val provincia: String,
    val pais: String,

    // Campos adicionales de Firestore
    val countryCode: String = "",        // Código de país (puede estar vacío)
    val formattedAddress: String = "",   // Dirección formateada completa
    val lat: Double? = null,            // Latitud (puede ser null)
    val lng: Double? = null,            // Longitud (puede ser null)
    val placeId: String = "",           // ID del lugar (puede estar vacío)
    val privacyLevel: String = "city"   // Nivel de privacidad (city, postal, neighborhood, etc.)
)