package com.mision.biihlive.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.mision.biihlive.config.FirebaseConfig
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.domain.perfil.model.Ubicacion
import com.mision.biihlive.domain.perfil.model.SubscriptionConfig
import com.mision.biihlive.domain.perfil.model.SubscriptionOption
import com.mision.biihlive.domain.perfil.model.PatrocinioConfig
import com.mision.biihlive.domain.users.model.UserPreview
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.mision.biihlive.utils.LevelCalculator

// Data class for ranking functionality
data class RankingUser(
    val userId: String,
    val nickname: String,
    val fullName: String,
    val totalScore: Int,
    val ubicacion: String,
    val ciudad: String,
    val provincia: String,
    val pais: String,
    val nivel: Int,
    val isVerified: Boolean,
    val profileImageUrl: String,
    val thumbnailImageUrl: String,
    val countryCode: String,
    val subdivisionCode: String,
    val postalCode: String
)

/**
 * Repository para Firestore que reemplaza AppSyncRepository
 * Mantiene la misma interfaz pero usa Firestore como backend
 */
class FirestoreRepository {

    // Obtener instancia configurada de Firestore (apunta a "basebiihlive")
    private val firestore = FirebaseConfig.getFirestore()

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val CLOUDFRONT_URL = "https://d183hg75gdabnr.cloudfront.net"

        // Timestamp conocido que funciona para la mayoría de usuarios
        private const val DEFAULT_TIMESTAMP = "1759240530172"

        // Colecciones de Firestore
        private const val USERS_COLLECTION = "users"
        private const val FOLLOWS_COLLECTION = "follows"  // Colección específica para relaciones de seguimiento
        private const val SOCIAL_COLLECTION = "social"     // Conservamos social para otras funciones
        private const val PRESENCE_COLLECTION = "presence"
        private const val RANKING_COLLECTION = "ranking"
        private const val SUSCRIPCIONES_COLLECTION = "suscripciones"  // Sistema de suscripciones escalable
        private const val PATROCINIOS_COLLECTION = "patrocinios"      // Sistema de patrocinios escalable

        private fun generateThumbnailUrl(userId: String): String {
            return "$CLOUDFRONT_URL/userprofile/$userId/thumbnail_$DEFAULT_TIMESTAMP.png"
        }
    }

    /**
     * Genera la URL del thumbnail para un usuario
     * Usa un timestamp conocido que funciona para la mayoría
     */
    private fun generateThumbnailUrl(userId: String): String {
        return "$CLOUDFRONT_URL/userprofile/$userId/thumbnail_$DEFAULT_TIMESTAMP.png"
    }

    /**
     * Obtener mi perfil (perfil del usuario autenticado)
     */
    suspend fun getMyProfile(userId: String): Result<PerfilUsuario?> {
        return try {
            Log.d(TAG, "Obteniendo mi perfil para userId: $userId")

            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val perfil = document.toPerfilUsuario()
                Log.d(TAG, "Mi perfil obtenido exitosamente: ${perfil?.nickname}")
                Result.success(perfil)
            } else {
                Log.w(TAG, "Perfil no encontrado para userId: $userId")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo mi perfil", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener perfil de otro usuario
     */
    suspend fun getPerfilUsuario(userId: String): Result<PerfilUsuario?> {
        return try {
            Log.d(TAG, "Obteniendo perfil para userId: $userId")

            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val perfil = document.toPerfilUsuario()
                Log.d(TAG, "Perfil obtenido exitosamente: ${perfil?.nickname}")
                Result.success(perfil)
            } else {
                Log.w(TAG, "Perfil no encontrado para userId: $userId")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo perfil", e)
            Result.failure(e)
        }
    }

    /**
     * Actualizar perfil de usuario
     */
    suspend fun updateProfile(
        userId: String,
        nickname: String? = null,
        description: String? = null,
        photoUrl: String? = null,
        fullName: String? = null,
        rankingPreference: String? = null,
        tipo: String? = null,
        pais: String? = null,
        provincia: String? = null,
        ciudad: String? = null,
        mostrarEstado: Boolean? = null
    ): Result<PerfilUsuario?> {
        return try {
            Log.d(TAG, "Actualizando perfil para userId: $userId")

            val updates = mutableMapOf<String, Any>()
            nickname?.let { updates["nickname"] = it }
            description?.let { updates["description"] = it }
            photoUrl?.let { updates["photoUrl"] = it }
            fullName?.let { updates["fullName"] = it }
            rankingPreference?.let { updates["rankingPreference"] = it }
            tipo?.let { updates["tipo"] = it }
            mostrarEstado?.let { updates["mostrarEstado"] = it }

            // Actualizar ubicación si se proporcionan campos
            if (pais != null || provincia != null || ciudad != null) {
                val ubicacionUpdates = mutableMapOf<String, Any>()
                pais?.let { ubicacionUpdates["pais"] = it }
                provincia?.let { ubicacionUpdates["provincia"] = it }
                ciudad?.let { ubicacionUpdates["ciudad"] = it }

                ubicacionUpdates.forEach { (key, value) ->
                    updates["ubicacion.$key"] = value
                }
            }

            updates["lastUpdated"] = System.currentTimeMillis()

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .await()

            // Obtener el perfil actualizado
            getPerfilUsuario(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando perfil", e)
            Result.failure(e)
        }
    }

    /**
     * Actualizar configuración de suscripciones de usuario
     * Estructura basada en APK: isEnabled, currency, description + array de options
     */
    suspend fun updateUserSubscriptionConfig(
        userId: String,
        price: String,
        duration: String,
        isEnabled: Boolean,
        currency: String,
        description: String
    ): Result<PerfilUsuario?> {
        return try {
            Log.d(TAG, "💰 [SUBSCRIPTION_CONFIG] Actualizando configuración de suscripción para userId: $userId")
            Log.d(TAG, "💰 [SUBSCRIPTION_CONFIG] Nuevos valores - price: $price, duration: $duration, isEnabled: $isEnabled, currency: $currency")

            // Obtener configuración actual para preservar opciones existentes
            val currentProfile = getPerfilUsuario(userId).getOrNull()
            val currentOptions = currentProfile?.subscriptionConfig?.options ?: emptyList()

            // Actualizar la primera opción o crear una nueva
            val updatedOptions = if (currentOptions.isNotEmpty()) {
                // Actualizar primera opción existente
                currentOptions.mapIndexed { index, option ->
                    if (index == 0) {
                        option.copy(
                            price = price,
                            duration = duration,
                            durationInDays = when (duration) {
                                "1 mes" -> 30
                                "3 meses" -> 90
                                "anual" -> 365
                                else -> 30
                            },
                            displayName = "Plan ${duration.replaceFirstChar { it.uppercase() }}"
                        )
                    } else option
                }
            } else {
                // Crear primera opción si no existe
                listOf(
                    mapOf(
                        "id" to java.util.UUID.randomUUID().toString(),
                        "price" to price,
                        "duration" to duration,
                        "durationInDays" to when (duration) {
                            "1 mes" -> 30
                            "3 meses" -> 90
                            "anual" -> 365
                            else -> 30
                        },
                        "displayName" to "Plan ${duration.replaceFirstChar { it.uppercase() }}",
                        "isActive" to true
                    )
                )
            }

            // Crear la estructura correcta según APK
            val subscriptionConfigObject = mapOf(
                "isEnabled" to isEnabled,
                "currency" to currency,
                "description" to description,
                "options" to updatedOptions
            )

            val updates = mapOf(
                "subscriptionConfig" to subscriptionConfigObject,
                "lastUpdated" to System.currentTimeMillis()
            )

            Log.d(TAG, "💰 [SUBSCRIPTION_CONFIG] 🔄 Actualizando con estructura APK: $subscriptionConfigObject")

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()

            Log.d(TAG, "💰 [SUBSCRIPTION_CONFIG] ✅ Configuración actualizada exitosamente en Firestore")

            // Obtener el perfil actualizado
            getPerfilUsuario(userId)
        } catch (e: Exception) {
            Log.e(TAG, "💰 [SUBSCRIPTION_CONFIG] ❌ Error actualizando configuración de suscripción", e)
            Result.failure(e)
        }
    }

    /**
     * Listar usuarios con búsqueda y filtros
     * Ordena por popularidad (totalScore) descendente
     */
    suspend fun listUsuarios(
        searchTerm: String? = null,
        limit: Int = 20,
        lastDocument: String? = null
    ): Result<List<UserPreview>> {
        return try {
            Log.d(TAG, "Listando usuarios - search: $searchTerm, limit: $limit")

            var query: Query = firestore.collection(USERS_COLLECTION)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            // Si hay búsqueda, filtrar por nickname (Firestore limitation - solo un campo de búsqueda)
            searchTerm?.let { term ->
                query = query.whereGreaterThanOrEqualTo("nickname", term)
                    .whereLessThanOrEqualTo("nickname", term + "\uf8ff")
                    .orderBy("nickname")  // Requerido cuando usamos whereGreaterThanOrEqualTo
                    .limit(limit.toLong())
            }

            // Agregar paginación usando lastDocument si está disponible
            lastDocument?.let { lastDocId ->
                try {
                    val lastDocSnapshot = firestore.collection(USERS_COLLECTION)
                        .document(lastDocId)
                        .get()
                        .await()
                    if (lastDocSnapshot.exists()) {
                        query = query.startAfter(lastDocSnapshot)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error obteniendo lastDocument para paginación: $lastDocId", e)
                }
            }

            val documents = query.get().await()
            val usuarios = documents.mapNotNull { doc ->
                try {
                    val totalScore = doc.getLong("totalScore")?.toInt() ?: 0
                    UserPreview(
                        userId = doc.id,
                        nickname = doc.getString("nickname") ?: "",
                        fullName = doc.getString("fullName") ?: "",
                        description = doc.getString("description") ?: "",
                        totalScore = totalScore,
                        nivel = LevelCalculator.calculateLevel(totalScore), // ✅ Calculado dinámicamente
                        tipo = doc.getString("tipo") ?: "user",
                        rankingPreference = doc.getString("rankingPreference") ?: "local",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        photoUrl = doc.getString("photoUrl") ?: "",
                        email = doc.getString("email") ?: "",
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        seguidores = doc.getLong("seguidores")?.toInt() ?: 0,
                        siguiendo = doc.getLong("siguiendo")?.toInt() ?: 0,
                        isOnline = doc.getBoolean("isOnline") ?: false,
                        mostrarEstado = doc.getBoolean("mostrarEstado") ?: true,
                        // Campos de ubicación (estructura simplificada para Firestore)
                        ciudad = doc.getString("ciudad") ?: "",
                        provincia = doc.getString("provincia") ?: "",
                        pais = doc.getString("pais") ?: "",
                        countryCode = doc.getString("countryCode") ?: "",
                        formattedAddress = doc.getString("formattedAddress") ?: "",
                        lat = doc.getDouble("lat"),
                        lng = doc.getDouble("lng"),
                        placeId = doc.getString("placeId") ?: "",
                        privacyLevel = doc.getString("privacyLevel") ?: "city"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando usuario: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Usuarios listados exitosamente: ${usuarios.size}")
            Result.success(usuarios)
        } catch (e: Exception) {
            Log.e(TAG, "Error listando usuarios", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ranking de usuarios
     */
    suspend fun getRankingLocal(limit: Int = 50): Result<List<RankingUser>> {
        return try {
            Log.d(TAG, "Obteniendo ranking local - limit: $limit")

            val documents = firestore.collection(USERS_COLLECTION)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val ranking = documents.mapIndexedNotNull { index, doc ->
                try {
                    val totalScore = doc.getLong("totalScore")?.toInt() ?: 0
                    RankingUser(
                        userId = doc.id,
                        nickname = doc.getString("nickname") ?: "Usuario",
                        fullName = doc.getString("fullName") ?: "",
                        totalScore = totalScore,
                        ubicacion = "${doc.getString("ciudad") ?: ""}, ${doc.getString("provincia") ?: ""}, ${doc.getString("pais") ?: ""}".trim(',', ' '),
                        ciudad = doc.getString("ciudad") ?: "",
                        provincia = doc.getString("provincia") ?: "",
                        pais = doc.getString("pais") ?: "",
                        nivel = LevelCalculator.calculateLevel(totalScore), // ✅ Calculado dinámicamente
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        profileImageUrl = doc.getString("photoUrl") ?: "",
                        thumbnailImageUrl = doc.getString("photoUrl") ?: "",
                        countryCode = doc.getString("countryCode") ?: "ESP",
                        subdivisionCode = doc.getString("provincia") ?: "",
                        postalCode = doc.getString("postalCode") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando ranking user: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Ranking obtenido exitosamente: ${ranking.size} usuarios")
            Result.success(ranking)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ranking", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ranking mundial (todos los usuarios)
     */
    suspend fun getRankingMundial(limit: Int = 50): Result<List<RankingUser>> {
        return try {
            Log.d(TAG, "🌍 [RANKING] Obteniendo ranking mundial - limit: $limit")

            val documents = firestore.collection(USERS_COLLECTION)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val ranking = documents.mapIndexedNotNull { index, doc ->
                try {
                    val totalScore = doc.getLong("totalScore")?.toInt() ?: 0

                    // Obtener ubicación como objeto anidado
                    val ubicacionMap = doc.get("ubicacion") as? Map<String, Any>
                    val ciudad = ubicacionMap?.get("ciudad") as? String ?: ""
                    val provincia = ubicacionMap?.get("provincia") as? String ?: ""
                    val pais = ubicacionMap?.get("pais") as? String ?: ""

                    RankingUser(
                        userId = doc.id,
                        nickname = doc.getString("nickname") ?: "Usuario",
                        fullName = doc.getString("fullName") ?: "",
                        totalScore = totalScore,
                        ubicacion = "$ciudad, $provincia, $pais".trim(',', ' '),
                        ciudad = ciudad,
                        provincia = provincia,
                        pais = pais,
                        nivel = LevelCalculator.calculateLevel(totalScore), // ✅ Calculado dinámicamente
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        profileImageUrl = generateThumbnailUrl(doc.id),
                        thumbnailImageUrl = generateThumbnailUrl(doc.id),
                        countryCode = doc.getString("countryCode") ?: "ESP",
                        subdivisionCode = provincia,
                        postalCode = doc.getString("postalCode") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando ranking user: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "🌍 [RANKING] Ranking mundial obtenido: ${ranking.size} usuarios")
            Result.success(ranking)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ranking mundial", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ranking local basado en la ciudad del usuario actual
     */
    suspend fun getRankingLocal(currentUserId: String, limit: Int = 50): Result<List<RankingUser>> {
        return try {
            Log.d(TAG, "🏠 [RANKING] Obteniendo ranking local para usuario: $currentUserId")

            // Primero obtener la ubicación del usuario actual
            val currentUserDoc = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .get()
                .await()

            // Obtener ubicación como objeto anidado
            val ubicacionMap = currentUserDoc.get("ubicacion") as? Map<String, Any>
            val currentUserCiudad = ubicacionMap?.get("ciudad") as? String

            if (currentUserCiudad.isNullOrBlank()) {
                Log.w(TAG, "🏠 [RANKING] Usuario actual no tiene ciudad definida, usando ranking mundial")
                return getRankingMundial(limit)
            }

            Log.d(TAG, "🏠 [RANKING] Filtrando por ciudad: $currentUserCiudad")

            val documents = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("ubicacion.ciudad", currentUserCiudad)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val ranking = documents.mapIndexedNotNull { index, doc ->
                try {
                    val totalScore = doc.getLong("totalScore")?.toInt() ?: 0

                    // Obtener ubicación como objeto anidado
                    val ubicacionMap = doc.get("ubicacion") as? Map<String, Any>
                    val ciudad = ubicacionMap?.get("ciudad") as? String ?: ""
                    val provincia = ubicacionMap?.get("provincia") as? String ?: ""
                    val pais = ubicacionMap?.get("pais") as? String ?: ""

                    RankingUser(
                        userId = doc.id,
                        nickname = doc.getString("nickname") ?: "Usuario",
                        fullName = doc.getString("fullName") ?: "",
                        totalScore = totalScore,
                        ubicacion = "$ciudad, $provincia, $pais".trim(',', ' '),
                        ciudad = ciudad,
                        provincia = provincia,
                        pais = pais,
                        nivel = LevelCalculator.calculateLevel(totalScore), // ✅ Calculado dinámicamente
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        profileImageUrl = generateThumbnailUrl(doc.id),
                        thumbnailImageUrl = generateThumbnailUrl(doc.id),
                        countryCode = doc.getString("countryCode") ?: "ESP",
                        subdivisionCode = provincia,
                        postalCode = doc.getString("postalCode") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando ranking user: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "🏠 [RANKING] Ranking local obtenido: ${ranking.size} usuarios en ciudad $currentUserCiudad")
            Result.success(ranking)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ranking local", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ranking provincial basado en la provincia del usuario actual
     */
    suspend fun getRankingProvincial(currentUserId: String, limit: Int = 50): Result<List<RankingUser>> {
        return try {
            Log.d(TAG, "🏛️ [RANKING] Obteniendo ranking provincial para usuario: $currentUserId")

            // Primero obtener la ubicación del usuario actual
            val currentUserDoc = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .get()
                .await()

            // Obtener ubicación como objeto anidado
            val ubicacionMap = currentUserDoc.get("ubicacion") as? Map<String, Any>
            val currentUserProvincia = ubicacionMap?.get("provincia") as? String

            if (currentUserProvincia.isNullOrBlank()) {
                Log.w(TAG, "🏛️ [RANKING] Usuario actual no tiene provincia definida, usando ranking mundial")
                return getRankingMundial(limit)
            }

            Log.d(TAG, "🏛️ [RANKING] Filtrando por provincia: $currentUserProvincia")

            val documents = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("ubicacion.provincia", currentUserProvincia)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val ranking = documents.mapIndexedNotNull { index, doc ->
                try {
                    val totalScore = doc.getLong("totalScore")?.toInt() ?: 0

                    // Obtener ubicación como objeto anidado
                    val ubicacionMap = doc.get("ubicacion") as? Map<String, Any>
                    val ciudad = ubicacionMap?.get("ciudad") as? String ?: ""
                    val provincia = ubicacionMap?.get("provincia") as? String ?: ""
                    val pais = ubicacionMap?.get("pais") as? String ?: ""

                    RankingUser(
                        userId = doc.id,
                        nickname = doc.getString("nickname") ?: "Usuario",
                        fullName = doc.getString("fullName") ?: "",
                        totalScore = totalScore,
                        ubicacion = "$ciudad, $provincia, $pais".trim(',', ' '),
                        ciudad = ciudad,
                        provincia = provincia,
                        pais = pais,
                        nivel = LevelCalculator.calculateLevel(totalScore), // ✅ Calculado dinámicamente
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        profileImageUrl = generateThumbnailUrl(doc.id),
                        thumbnailImageUrl = generateThumbnailUrl(doc.id),
                        countryCode = doc.getString("countryCode") ?: "ESP",
                        subdivisionCode = provincia,
                        postalCode = doc.getString("postalCode") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando ranking user: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "🏛️ [RANKING] Ranking provincial obtenido: ${ranking.size} usuarios en provincia $currentUserProvincia")
            Result.success(ranking)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ranking provincial", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ranking nacional basado en el país del usuario actual
     */
    suspend fun getRankingNacional(currentUserId: String, limit: Int = 50): Result<List<RankingUser>> {
        return try {
            Log.d(TAG, "🇪🇸 [RANKING] Obteniendo ranking nacional para usuario: $currentUserId")

            // Primero obtener la ubicación del usuario actual
            val currentUserDoc = firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .get()
                .await()

            // Obtener ubicación como objeto anidado
            val ubicacionMap = currentUserDoc.get("ubicacion") as? Map<String, Any>
            val currentUserPais = ubicacionMap?.get("pais") as? String

            if (currentUserPais.isNullOrBlank()) {
                Log.w(TAG, "🇪🇸 [RANKING] Usuario actual no tiene país definido, usando ranking mundial")
                return getRankingMundial(limit)
            }

            Log.d(TAG, "🇪🇸 [RANKING] Filtrando por país: $currentUserPais")

            val documents = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("ubicacion.pais", currentUserPais)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val ranking = documents.mapIndexedNotNull { index, doc ->
                try {
                    val totalScore = doc.getLong("totalScore")?.toInt() ?: 0

                    // Obtener ubicación como objeto anidado
                    val ubicacionMap = doc.get("ubicacion") as? Map<String, Any>
                    val ciudad = ubicacionMap?.get("ciudad") as? String ?: ""
                    val provincia = ubicacionMap?.get("provincia") as? String ?: ""
                    val pais = ubicacionMap?.get("pais") as? String ?: ""

                    RankingUser(
                        userId = doc.id,
                        nickname = doc.getString("nickname") ?: "Usuario",
                        fullName = doc.getString("fullName") ?: "",
                        totalScore = totalScore,
                        ubicacion = "$ciudad, $provincia, $pais".trim(',', ' '),
                        ciudad = ciudad,
                        provincia = provincia,
                        pais = pais,
                        nivel = LevelCalculator.calculateLevel(totalScore), // ✅ Calculado dinámicamente
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        profileImageUrl = generateThumbnailUrl(doc.id),
                        thumbnailImageUrl = generateThumbnailUrl(doc.id),
                        countryCode = doc.getString("countryCode") ?: "ESP",
                        subdivisionCode = provincia,
                        postalCode = doc.getString("postalCode") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando ranking user: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "🇪🇸 [RANKING] Ranking nacional obtenido: ${ranking.size} usuarios en país $currentUserPais")
            Result.success(ranking)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ranking nacional", e)
            Result.failure(e)
        }
    }

    /**
     * Seguir a un usuario
     * ESTRUCTURA ESCALABLE CON SUBCOLECCIONES Y TRANSACCIONES:
     * users/{followerId}/following/{followedId} - timestamp
     * users/{followedId}/followers/{followerId} - timestamp
     * userStats/{userId} - followersCount, followingCount
     */
    suspend fun followUser(followerId: String, followedId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "👤 [FOLLOW_DEBUG] Usuario $followerId siguiendo a $followedId")

            // Usar transacción para operación atómica
            firestore.runTransaction { transaction ->
                // Referencias para subcolecciones
                val followerFollowingRef = firestore.collection(USERS_COLLECTION)
                    .document(followerId)
                    .collection("following")
                    .document(followedId)

                val followedFollowersRef = firestore.collection(USERS_COLLECTION)
                    .document(followedId)
                    .collection("followers")
                    .document(followerId)

                // Referencias para userStats
                val followerStatsRef = firestore.collection("userStats").document(followerId)
                val followedStatsRef = firestore.collection("userStats").document(followedId)

                // Referencias para contadores en users (mantener compatibilidad)
                val followerUserRef = firestore.collection(USERS_COLLECTION).document(followerId)
                val followedUserRef = firestore.collection(USERS_COLLECTION).document(followedId)

                // Crear relaciones en subcolecciones
                transaction.set(followerFollowingRef, mapOf(
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "followedId" to followedId
                ))

                transaction.set(followedFollowersRef, mapOf(
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "followerId" to followerId
                ))

                // Actualizar contadores en userStats
                transaction.update(followerStatsRef,
                    "followingCount", com.google.firebase.firestore.FieldValue.increment(1))

                transaction.update(followedStatsRef,
                    "followersCount", com.google.firebase.firestore.FieldValue.increment(1))

                // Mantener contadores legacy en users
                transaction.update(followerUserRef, "siguiendo", com.google.firebase.firestore.FieldValue.increment(1))
                transaction.update(followedUserRef, "seguidores", com.google.firebase.firestore.FieldValue.increment(1))

                null
            }.await()

            Log.d(TAG, "👤 [FOLLOW_DEBUG] ✅ Usuario seguido exitosamente con estructura escalable")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "👤 [FOLLOW_DEBUG] ❌ Error siguiendo usuario", e)
            Result.failure(e)
        }
    }

    /**
     * Dejar de seguir a un usuario
     * ESTRUCTURA ESCALABLE CON SUBCOLECCIONES Y TRANSACCIONES:
     * users/{followerId}/following/{followedId} - eliminar documento
     * users/{followedId}/followers/{followerId} - eliminar documento
     * userStats/{userId} - decrementar contadores
     */
    suspend fun unfollowUser(followerId: String, followedId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "👤 [FOLLOW_DEBUG] Usuario $followerId dejando de seguir a $followedId")

            // Usar transacción para operación atómica
            firestore.runTransaction { transaction ->
                // Referencias para subcolecciones
                val followerFollowingRef = firestore.collection(USERS_COLLECTION)
                    .document(followerId)
                    .collection("following")
                    .document(followedId)

                val followedFollowersRef = firestore.collection(USERS_COLLECTION)
                    .document(followedId)
                    .collection("followers")
                    .document(followerId)

                // Referencias para userStats
                val followerStatsRef = firestore.collection("userStats").document(followerId)
                val followedStatsRef = firestore.collection("userStats").document(followedId)

                // Referencias para contadores en users (mantener compatibilidad)
                val followerUserRef = firestore.collection(USERS_COLLECTION).document(followerId)
                val followedUserRef = firestore.collection(USERS_COLLECTION).document(followedId)

                // Eliminar relaciones en subcolecciones
                transaction.delete(followerFollowingRef)
                transaction.delete(followedFollowersRef)

                // Actualizar contadores en userStats
                transaction.update(followerStatsRef,
                    "followingCount", com.google.firebase.firestore.FieldValue.increment(-1))

                transaction.update(followedStatsRef,
                    "followersCount", com.google.firebase.firestore.FieldValue.increment(-1))

                // Mantener contadores legacy en users
                transaction.update(followerUserRef, "siguiendo", com.google.firebase.firestore.FieldValue.increment(-1))
                transaction.update(followedUserRef, "seguidores", com.google.firebase.firestore.FieldValue.increment(-1))

                null
            }.await()

            Log.d(TAG, "👤 [FOLLOW_DEBUG] ✅ Usuario no seguido exitosamente con estructura escalable")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "👤 [FOLLOW_DEBUG] ❌ Error dejando de seguir usuario", e)
            Result.failure(e)
        }
    }

    /**
     * Verificar si un usuario sigue a otro
     * ESTRUCTURA ESCALABLE CON SUBCOLECCIONES:
     * users/{followerId}/following/{followedId} - verificar existencia del documento
     */
    suspend fun isFollowing(followerId: String, followedId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "🔍 [FOLLOW_DEBUG] Verificando si $followerId sigue a $followedId")

            // Verificar existencia del documento en subcolección
            Log.d(TAG, "🔍 [FOLLOW_DEBUG] Buscando en subcolección 'users/$followerId/following/$followedId'...")
            val followingDoc = firestore.collection(USERS_COLLECTION)
                .document(followerId)
                .collection("following")
                .document(followedId)
                .get()
                .await()

            var isFollowing = followingDoc.exists()
            Log.d(TAG, "🔍 [FOLLOW_DEBUG] Verificación en subcolección: $isFollowing")

            // Fallback a estructura legacy si no hay datos en subcolecciones
            if (!isFollowing) {
                Log.d(TAG, "🔍 [FOLLOW_DEBUG] No encontrado en subcolecciones, buscando en estructura legacy...")

                // Buscar en estructura de arrays (legacy)
                val followerDoc = firestore.collection(FOLLOWS_COLLECTION)
                    .document(followerId)
                    .get()
                    .await()

                if (followerDoc.exists()) {
                    // Verificar si followedId está en el array 'following'
                    val followingArray = followerDoc.get("following") as? List<*>
                    isFollowing = followingArray?.contains(followedId) == true
                    Log.d(TAG, "🔍 [FOLLOW_DEBUG] Verificación en arrays legacy: $isFollowing")
                } else {
                    // Última opción: colección social
                    val socialQuery = firestore.collection(SOCIAL_COLLECTION)
                        .whereEqualTo("followerId", followerId)
                        .whereEqualTo("followedId", followedId)
                        .whereEqualTo("type", "follow")
                        .limit(1)
                        .get()
                        .await()

                    isFollowing = !socialQuery.isEmpty
                    Log.d(TAG, "🔍 [FOLLOW_DEBUG] Verificación en social legacy: $isFollowing")
                }
            }

            Log.d(TAG, "🔍 [FOLLOW_DEBUG] ✅ Resultado final: $isFollowing")
            Result.success(isFollowing)
        } catch (e: Exception) {
            Log.e(TAG, "🔍 [FOLLOW_DEBUG] ❌ Error verificando si sigue", e)
            Result.failure(e)
        }
    }

    /**
     * Crear o actualizar perfil de usuario
     */
    suspend fun createOrUpdateUser(
        userId: String,
        email: String,
        nickname: String? = null,
        fullName: String? = null,
        photoUrl: String? = null
    ): Result<Boolean> {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId)
            val existingDoc = userDoc.get().await()

            val userData = if (existingDoc.exists()) {
                // Actualizar usuario existente
                mutableMapOf<String, Any>(
                    "lastLoginAt" to System.currentTimeMillis(),
                    "isOnline" to true
                ).apply {
                    email.let { this["email"] = it }
                    nickname?.let { this["nickname"] = it }
                    fullName?.let { this["fullName"] = it }
                    photoUrl?.let { this["photoUrl"] = it }
                }
            } else {
                // Crear nuevo usuario
                mapOf(
                    "userId" to userId,
                    "email" to email,
                    "nickname" to (nickname ?: email.substringBefore("@")),
                    "fullName" to (fullName ?: ""),
                    "description" to "",
                    "photoUrl" to (photoUrl ?: ""),
                    "totalScore" to 0,
                    "nivel" to 1,
                    "tipo" to "user",
                    "seguidores" to 0,
                    "siguiendo" to 0,
                    "isVerified" to false,
                    "isOnline" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "lastLoginAt" to System.currentTimeMillis()
                )
            }

            userDoc.set(userData, SetOptions.merge()).await()
            Log.d(TAG, "Usuario creado/actualizado exitosamente: $userId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando/actualizando usuario", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener seguidores de un usuario con detalles completos
     * ESTRUCTURA ESCALABLE: users/{userId}/followers/{followerId} con fallback legacy
     */
    suspend fun getFollowersWithDetails(
        userId: String,
        limit: Int = 20
    ): Result<Pair<List<UserPreview>, String?>> {
        return try {
            Log.d(TAG, "👥 [FOLLOWERS_DEBUG] Obteniendo seguidores para userId: $userId, limit: $limit")

            // PASO 1: Intentar obtener desde estructura escalable (subcolecciones)
            val followersQuery = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("followers")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            var followerIds = mutableListOf<String>()

            // Obtener IDs desde subcolecciones escalables (estructura principal)
            followerIds = followersQuery.documents.map { doc ->
                doc.id // El ID del documento es el followerId
            }.toMutableList()

            Log.d(TAG, "👥 [FOLLOWERS_DEBUG] ✅ Subcolecciones escalables: encontrados ${followerIds.size} seguidores")

            if (followerIds.isEmpty()) {
                Log.d(TAG, "👥 [FOLLOWERS_DEBUG] ❌ No se encontraron followerIds válidos")
                return Result.success(Pair(emptyList(), null))
            }

            // PASO 3: Obtener detalles de los usuarios
            val usersDetails = mutableListOf<UserPreview>()

            // Firestore tiene un límite de 10 elementos en queries 'in', así que procesamos en lotes
            followerIds.chunked(10).forEach { chunk ->
                val usersQuery = firestore.collection(USERS_COLLECTION)
                    .whereIn("userId", chunk)
                    .get()
                    .await()

                usersQuery.documents.mapNotNull { doc ->
                    try {
                        val totalScore = doc.getLong("totalScore")?.toInt() ?: 0
                        UserPreview(
                            userId = doc.id,
                            nickname = doc.getString("nickname") ?: "",
                            fullName = doc.getString("fullName") ?: "",
                            description = doc.getString("description") ?: "",
                            totalScore = totalScore,
                            nivel = LevelCalculator.calculateLevel(totalScore), // ✅ Calculado dinámicamente
                            tipo = doc.getString("tipo") ?: "user",
                            rankingPreference = doc.getString("rankingPreference") ?: "local",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            photoUrl = generateThumbnailUrl(doc.id), // URL dinámica de S3
                            email = doc.getString("email") ?: "",
                            isVerified = doc.getBoolean("isVerified") ?: false,
                            seguidores = doc.getLong("seguidores")?.toInt() ?: 0,
                            siguiendo = doc.getLong("siguiendo")?.toInt() ?: 0,
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            mostrarEstado = doc.getBoolean("mostrarEstado") ?: true,
                            ciudad = doc.getString("ciudad") ?: "",
                            provincia = doc.getString("provincia") ?: "",
                            pais = doc.getString("pais") ?: "",
                            countryCode = doc.getString("countryCode") ?: "",
                            formattedAddress = doc.getString("formattedAddress") ?: "",
                            lat = doc.getDouble("lat"),
                            lng = doc.getDouble("lng"),
                            placeId = doc.getString("placeId") ?: "",
                            privacyLevel = doc.getString("privacyLevel") ?: "city"
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "👥 [FOLLOWERS_DEBUG] Error parseando follower user: ${doc.id}", e)
                        null
                    }
                }.let { users ->
                    usersDetails.addAll(users)
                }
            }

            Log.d(TAG, "👥 [FOLLOWERS_DEBUG] ✅ Seguidores obtenidos exitosamente: ${usersDetails.size} usuarios")

            // TODO: Implementar paginación real cuando sea necesario
            Result.success(Pair(usersDetails, null))
        } catch (e: Exception) {
            Log.e(TAG, "👥 [FOLLOWERS_DEBUG] ❌ Error obteniendo seguidores con detalles", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener usuarios que sigue un usuario (siguiendo) con detalles completos
     * ESTRUCTURA ESCALABLE: users/{userId}/following/{followingId} con fallback legacy
     */
    suspend fun getFollowingWithDetails(
        userId: String,
        limit: Int = 20
    ): Result<Pair<List<UserPreview>, String?>> {
        return try {
            Log.d(TAG, "👤 [FOLLOWING_DEBUG] Obteniendo siguiendo para userId: $userId, limit: $limit")

            // PASO 1: Intentar obtener desde estructura escalable (subcolecciones)
            val followingQuery = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("following")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            var followingIds = mutableListOf<String>()

            // Obtener IDs desde subcolecciones escalables (estructura principal)
            followingIds = followingQuery.documents.map { doc ->
                doc.id // El ID del documento es el followingId
            }.toMutableList()

            Log.d(TAG, "👤 [FOLLOWING_DEBUG] ✅ Subcolecciones escalables: encontrados ${followingIds.size} siguiendo")

            if (followingIds.isEmpty()) {
                Log.d(TAG, "👤 [FOLLOWING_DEBUG] ❌ No se encontraron followingIds válidos")
                return Result.success(Pair(emptyList(), null))
            }

            // PASO 3: Obtener detalles de los usuarios
            val usersDetails = mutableListOf<UserPreview>()

            // Firestore tiene un límite de 10 elementos en queries 'in', así que procesamos en lotes
            followingIds.chunked(10).forEach { chunk ->
                val usersQuery = firestore.collection(USERS_COLLECTION)
                    .whereIn("userId", chunk)
                    .get()
                    .await()

                usersQuery.documents.mapNotNull { doc ->
                    try {
                        val totalScore = doc.getLong("totalScore")?.toInt() ?: 0
                        UserPreview(
                            userId = doc.id,
                            nickname = doc.getString("nickname") ?: "",
                            fullName = doc.getString("fullName") ?: "",
                            description = doc.getString("description") ?: "",
                            totalScore = totalScore,
                            nivel = LevelCalculator.calculateLevel(totalScore), // ✅ Calculado dinámicamente
                            tipo = doc.getString("tipo") ?: "user",
                            rankingPreference = doc.getString("rankingPreference") ?: "local",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            photoUrl = generateThumbnailUrl(doc.id), // URL dinámica de S3
                            email = doc.getString("email") ?: "",
                            isVerified = doc.getBoolean("isVerified") ?: false,
                            seguidores = doc.getLong("seguidores")?.toInt() ?: 0,
                            siguiendo = doc.getLong("siguiendo")?.toInt() ?: 0,
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            mostrarEstado = doc.getBoolean("mostrarEstado") ?: true,
                            ciudad = doc.getString("ciudad") ?: "",
                            provincia = doc.getString("provincia") ?: "",
                            pais = doc.getString("pais") ?: "",
                            countryCode = doc.getString("countryCode") ?: "",
                            formattedAddress = doc.getString("formattedAddress") ?: "",
                            lat = doc.getDouble("lat"),
                            lng = doc.getDouble("lng"),
                            placeId = doc.getString("placeId") ?: "",
                            privacyLevel = doc.getString("privacyLevel") ?: "city"
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "👤 [FOLLOWING_DEBUG] Error parseando following user: ${doc.id}", e)
                        null
                    }
                }.let { users ->
                    usersDetails.addAll(users)
                }
            }

            Log.d(TAG, "👤 [FOLLOWING_DEBUG] ✅ Siguiendo obtenidos exitosamente: ${usersDetails.size} usuarios")

            // TODO: Implementar paginación real cuando sea necesario
            Result.success(Pair(usersDetails, null))
        } catch (e: Exception) {
            Log.e(TAG, "👤 [FOLLOWING_DEBUG] ❌ Error obteniendo siguiendo con detalles", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener IDs de usuarios que sigue un usuario específico
     * ESTRUCTURA ESCALABLE CON SUBCOLECCIONES:
     * users/{userId}/following/{followingId} - timestamp, migratedFrom
     */
    suspend fun getFollowingIds(userId: String): Result<Set<String>> {
        return try {
            Log.d(TAG, "🔍 [FOLLOW_DEBUG] Obteniendo IDs de usuarios seguidos para userId: $userId")

            // Obtener documentos de la subcolección 'following'
            Log.d(TAG, "🔍 [FOLLOW_DEBUG] Buscando en subcolección 'users/$userId/following'...")
            val followingQuery = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("following")
                .get()
                .await()

            Log.d(TAG, "🔍 [FOLLOW_DEBUG] Documentos encontrados en subcolección: ${followingQuery.documents.size}")

            var followingIds = followingQuery.documents.map { doc ->
                Log.d(TAG, "🔍 [FOLLOW_DEBUG] Following documento: ${doc.id}")
                doc.id // El ID del documento es el followingId
            }.toSet()

            Log.d(TAG, "🔍 [FOLLOW_DEBUG] ✅ IDs seguidos desde subcolección: $followingIds (${followingIds.size} usuarios)")

            // Fallback a estructura legacy si no hay datos en subcolecciones
            if (followingIds.isEmpty()) {
                Log.d(TAG, "🔍 [FOLLOW_DEBUG] No hay datos en subcolecciones, buscando en estructura legacy...")

                // Buscar en estructura de arrays (legacy)
                val userFollowDoc = firestore.collection(FOLLOWS_COLLECTION)
                    .document(userId)
                    .get()
                    .await()

                if (userFollowDoc.exists()) {
                    val followingArray = userFollowDoc.get("following") as? List<*>
                    followingIds = followingArray?.mapNotNull { it as? String }?.toSet() ?: emptySet()
                    Log.d(TAG, "🔍 [FOLLOW_DEBUG] ✅ IDs seguidos desde arrays legacy: $followingIds (${followingIds.size} usuarios)")
                } else {
                    // Última opción: colección social
                    val socialQuery = firestore.collection(SOCIAL_COLLECTION)
                        .whereEqualTo("followerId", userId)
                        .whereEqualTo("type", "follow")
                        .get()
                        .await()

                    followingIds = socialQuery.documents.mapNotNull { doc ->
                        doc.getString("followedId")
                    }.toSet()

                    Log.d(TAG, "🔍 [FOLLOW_DEBUG] ✅ IDs seguidos desde social legacy: $followingIds (${followingIds.size} usuarios)")
                }
            }

            Log.d(TAG, "🔍 [FOLLOW_DEBUG] ✅ RESULTADO FINAL: ${followingIds.size} usuarios -> $followingIds")
            Result.success(followingIds)
        } catch (e: Exception) {
            Log.e(TAG, "🔍 [FOLLOW_DEBUG] ❌ Error obteniendo IDs de seguidos", e)
            Result.failure(e)
        }
    }


    /**
     * Obtener contadores de userStats (seguidores/siguiendo) desde la estructura escalable
     * ESTRUCTURA: userStats/{userId} - followersCount, followingCount
     */
    suspend fun getUserStats(userId: String): Result<Pair<Int, Int>> {
        return try {
            Log.d(TAG, "📊 [STATS_DEBUG] Obteniendo userStats para userId: $userId")

            val userStatsDoc = firestore.collection("userStats")
                .document(userId)
                .get()
                .await()

            if (userStatsDoc.exists()) {
                val followersCount = userStatsDoc.getLong("followersCount")?.toInt() ?: 0
                val followingCount = userStatsDoc.getLong("followingCount")?.toInt() ?: 0

                Log.d(TAG, "📊 [STATS_DEBUG] ✅ UserStats encontrados: $followersCount seguidores, $followingCount siguiendo")
                Result.success(Pair(followersCount, followingCount))
            } else {
                Log.d(TAG, "📊 [STATS_DEBUG] ⚠️ UserStats no encontrados, usando contadores legacy del perfil")

                // Fallback a contadores legacy en users collection
                val userDoc = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    val seguidores = userDoc.getLong("seguidores")?.toInt() ?: 0
                    val siguiendo = userDoc.getLong("siguiendo")?.toInt() ?: 0

                    Log.d(TAG, "📊 [STATS_DEBUG] ✅ Contadores legacy: $seguidores seguidores, $siguiendo siguiendo")
                    Result.success(Pair(seguidores, siguiendo))
                } else {
                    Log.w(TAG, "📊 [STATS_DEBUG] ❌ Usuario no encontrado")
                    Result.success(Pair(0, 0))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "📊 [STATS_DEBUG] ❌ Error obteniendo userStats", e)
            Result.failure(e)
        }
    }

    /**
     * Helper function to build location string from individual components
     */
    private fun buildUbicacionString(ciudad: String, provincia: String, pais: String): String {
        return listOf(ciudad, provincia, pais)
            .filter { it.isNotEmpty() }
            .joinToString(", ")
    }

    // ================================================================================================
    // FUNCIONES DE SUSCRIPCIÓN - ESTRUCTURA ESCALABLE CON SUBCOLECCIONES
    // ================================================================================================

    /**
     * Suscribir usuario a otro usuario
     * ESTRUCTURA ESCALABLE CON SUBCOLECCIONES Y TRANSACCIONES:
     * users/{suscriptorId}/suscripciones/{suscritoId} - timestamp + metadatos
     * users/{suscritoId}/suscriptores/{suscriptorId} - timestamp + metadatos
     * userStats/{userId} - suscripcionesCount, suscriptoresCount
     */
    suspend fun suscribirUsuario(suscriptorId: String, suscritoId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Usuario $suscriptorId suscribiéndose a $suscritoId")

            // Usar transacción para operación atómica
            firestore.runTransaction { transaction ->
                val currentTimestamp = System.currentTimeMillis()
                val expirationTimestamp = currentTimestamp + (30L * 24 * 60 * 60 * 1000) // 30 días

                // Referencias para subcolecciones
                val suscriptorSuscripcionesRef = firestore.collection(USERS_COLLECTION)
                    .document(suscriptorId)
                    .collection("suscripciones")
                    .document(suscritoId)

                val suscritoSuscriptoresRef = firestore.collection(USERS_COLLECTION)
                    .document(suscritoId)
                    .collection("suscriptores")
                    .document(suscriptorId)

                // Referencias para userStats
                val suscriptorStatsRef = firestore.collection("userStats").document(suscriptorId)
                val suscritoStatsRef = firestore.collection("userStats").document(suscritoId)

                // Referencias para contadores en users (mantener compatibilidad)
                val suscriptorUserRef = firestore.collection(USERS_COLLECTION).document(suscriptorId)
                val suscritoUserRef = firestore.collection(USERS_COLLECTION).document(suscritoId)

                // Datos de suscripción con metadatos
                val suscripcionData = mapOf(
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "suscritoId" to suscritoId,
                    "fechaInicio" to currentTimestamp,
                    "fechaFin" to expirationTimestamp,
                    "tipo" to "premium",
                    "estado" to "activa",
                    "precio" to "EUR 15/mes",
                    "renovacionAutomatica" to true
                )

                val suscriptorData = mapOf(
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "suscriptorId" to suscriptorId,
                    "fechaInicio" to currentTimestamp,
                    "fechaFin" to expirationTimestamp,
                    "tipo" to "premium",
                    "estado" to "activa",
                    "precio" to "EUR 15/mes",
                    "renovacionAutomatica" to true
                )

                // Crear relaciones en subcolecciones
                transaction.set(suscriptorSuscripcionesRef, suscripcionData)
                transaction.set(suscritoSuscriptoresRef, suscriptorData)

                // Actualizar contadores en userStats
                transaction.update(suscriptorStatsRef, "suscripcionesCount", com.google.firebase.firestore.FieldValue.increment(1))
                transaction.update(suscritoStatsRef, "suscriptoresCount", com.google.firebase.firestore.FieldValue.increment(1))

                // Mantener contadores legacy en users (compatibilidad)
                transaction.update(suscriptorUserRef, "suscripciones", com.google.firebase.firestore.FieldValue.increment(1))
                transaction.update(suscritoUserRef, "suscriptores", com.google.firebase.firestore.FieldValue.increment(1))

                Log.d(TAG, "💳 [SUSCR_DEBUG] ✅ Suscripción creada exitosamente con subcolecciones")
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "💳 [SUSCR_DEBUG] ❌ Error en suscripción", e)
            Result.failure(e)
        }
    }

    /**
     * Cancelar suscripción de usuario
     * ESTRUCTURA ESCALABLE CON SUBCOLECCIONES Y TRANSACCIONES:
     * users/{suscriptorId}/suscripciones/{suscritoId} - actualizar estado
     * users/{suscritoId}/suscriptores/{suscriptorId} - actualizar estado
     * userStats/{userId} - decrementar contadores
     */
    suspend fun cancelarSuscripcion(suscriptorId: String, suscritoId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Usuario $suscriptorId cancelando suscripción a $suscritoId")

            // Usar transacción para operación atómica
            firestore.runTransaction { transaction ->
                // Referencias para subcolecciones
                val suscriptorSuscripcionesRef = firestore.collection(USERS_COLLECTION)
                    .document(suscriptorId)
                    .collection("suscripciones")
                    .document(suscritoId)

                val suscritoSuscriptoresRef = firestore.collection(USERS_COLLECTION)
                    .document(suscritoId)
                    .collection("suscriptores")
                    .document(suscriptorId)

                // Referencias para userStats
                val suscriptorStatsRef = firestore.collection("userStats").document(suscriptorId)
                val suscritoStatsRef = firestore.collection("userStats").document(suscritoId)

                // Referencias para contadores en users (mantener compatibilidad)
                val suscriptorUserRef = firestore.collection(USERS_COLLECTION).document(suscriptorId)
                val suscritoUserRef = firestore.collection(USERS_COLLECTION).document(suscritoId)

                // Verificar si la suscripción existe
                val suscripcionDoc = transaction.get(suscriptorSuscripcionesRef)
                if (suscripcionDoc.exists()) {
                    // Actualizar estado a cancelada en lugar de eliminar (mantener historial)
                    val cancelacionData = mapOf(
                        "estado" to "cancelada",
                        "fechaCancelacion" to System.currentTimeMillis(),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    transaction.update(suscriptorSuscripcionesRef, cancelacionData)
                    transaction.update(suscritoSuscriptoresRef, cancelacionData)

                    // Decrementar contadores en userStats
                    transaction.update(suscriptorStatsRef, "suscripcionesCount", com.google.firebase.firestore.FieldValue.increment(-1))
                    transaction.update(suscritoStatsRef, "suscriptoresCount", com.google.firebase.firestore.FieldValue.increment(-1))

                    // Decrementar contadores legacy en users (compatibilidad)
                    transaction.update(suscriptorUserRef, "suscripciones", com.google.firebase.firestore.FieldValue.increment(-1))
                    transaction.update(suscritoUserRef, "suscriptores", com.google.firebase.firestore.FieldValue.increment(-1))

                    Log.d(TAG, "💳 [SUSCR_DEBUG] ✅ Suscripción cancelada exitosamente")
                } else {
                    Log.w(TAG, "💳 [SUSCR_DEBUG] No se encontró suscripción activa para cancelar")
                }
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "💳 [SUSCR_DEBUG] ❌ Error cancelando suscripción", e)
            Result.failure(e)
        }
    }

    /**
     * Verificar si un usuario está suscrito a otro
     * Consulta: users/{suscriptorId}/suscripciones/{suscritoId} exists + estado activa
     */
    suspend fun estaSuscrito(suscriptorId: String, suscritoId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Verificando si $suscriptorId está suscrito a $suscritoId")

            // Verificar existencia en subcolección
            val suscripcionDoc = firestore.collection(USERS_COLLECTION)
                .document(suscriptorId)
                .collection("suscripciones")
                .document(suscritoId)
                .get()
                .await()

            val estaSuscrito = if (suscripcionDoc.exists()) {
                val estado = suscripcionDoc.getString("estado")
                estado == "activa"
            } else {
                false
            }

            Log.d(TAG, "💳 [SUSCR_DEBUG] Usuario está suscrito: $estaSuscrito")
            Result.success(estaSuscrito)
        } catch (e: Exception) {
            Log.e(TAG, "💳 [SUSCR_DEBUG] ❌ Error verificando suscripción", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener IDs de usuarios a los que estoy suscrito
     * Consulta: users/{userId}/suscripciones/ donde estado = activa
     */
    suspend fun getSuscripcionesIds(userId: String): Result<List<String>> {
        return try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Obteniendo IDs de suscripciones para: $userId")

            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("suscripciones")
                .whereEqualTo("estado", "activa")
                .get()
                .await()

            val suscripcionesIds = querySnapshot.documents.mapNotNull { doc ->
                doc.id // Document ID es el userId del suscrito
            }

            Log.d(TAG, "💳 [SUSCR_DEBUG] ✅ Suscripciones encontradas: ${suscripcionesIds.size}")
            Result.success(suscripcionesIds)
        } catch (e: Exception) {
            Log.e(TAG, "💳 [SUSCR_DEBUG] ❌ Error obteniendo suscripciones", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener IDs de usuarios suscritos a mí
     * Consulta: users/{userId}/suscriptores/ donde estado = activa
     */
    suspend fun getSuscriptoresIds(userId: String): Result<List<String>> {
        return try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Obteniendo IDs de suscriptores para: $userId")

            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("suscriptores")
                .whereEqualTo("estado", "activa")
                .get()
                .await()

            val suscriptoresIds = querySnapshot.documents.mapNotNull { doc ->
                doc.id // Document ID es el userId del suscriptor
            }

            Log.d(TAG, "💳 [SUSCR_DEBUG] ✅ Suscriptores encontrados: ${suscriptoresIds.size}")
            Result.success(suscriptoresIds)
        } catch (e: Exception) {
            Log.e(TAG, "💳 [SUSCR_DEBUG] ❌ Error obteniendo suscriptores", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener detalles completos de usuarios a los que estoy suscrito
     * Similar a getFollowingWithDetails() pero para suscripciones
     */
    suspend fun getSuscripcionesWithDetails(userId: String): Result<List<UserPreview>> {
        return try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Obteniendo detalles de suscripciones para: $userId")

            val suscripcionesIdsResult = getSuscripcionesIds(userId)
            if (suscripcionesIdsResult.isFailure) {
                return Result.failure(suscripcionesIdsResult.exceptionOrNull()!!)
            }

            val suscripcionesIds = suscripcionesIdsResult.getOrNull() ?: emptyList()
            val users = mutableListOf<UserPreview>()

            for (suscritoId in suscripcionesIds) {
                try {
                    val profileResult = getPerfilUsuario(suscritoId)
                    profileResult.fold(
                        onSuccess = { perfil ->
                            if (perfil != null) {
                                val userPreview = UserPreview(
                                    userId = perfil.userId,
                                    nickname = perfil.nickname,
                                    fullName = perfil.fullName ?: perfil.nickname,
                                    description = perfil.description ?: "Suscripción activa",
                                    totalScore = perfil.totalScore,
                                    nivel = perfil.nivel,
                                    tipo = perfil.tipo,
                                    rankingPreference = perfil.rankingPreference,
                                    createdAt = perfil.createdAt,
                                    photoUrl = generateThumbnailUrl(perfil.userId),
                                    isVerified = perfil.isVerified ?: false
                                )
                                users.add(userPreview)
                                Log.d(TAG, "💳 [SUSCR_DEBUG] Usuario suscrito obtenido: ${perfil.nickname}")
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error obteniendo perfil de usuario suscrito $suscritoId", e)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando usuario suscrito $suscritoId", e)
                }
            }

            Log.d(TAG, "💳 [SUSCR_DEBUG] ✅ Detalles de suscripciones obtenidos: ${users.size}")
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "💳 [SUSCR_DEBUG] ❌ Error obteniendo detalles de suscripciones", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener detalles completos de usuarios suscritos a mí
     * Similar a getFollowersWithDetails() pero para suscriptores
     */
    suspend fun getSuscriptoresWithDetails(userId: String): Result<List<UserPreview>> {
        return try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Obteniendo detalles de suscriptores para: $userId")

            val suscriptoresIdsResult = getSuscriptoresIds(userId)
            if (suscriptoresIdsResult.isFailure) {
                return Result.failure(suscriptoresIdsResult.exceptionOrNull()!!)
            }

            val suscriptoresIds = suscriptoresIdsResult.getOrNull() ?: emptyList()
            val users = mutableListOf<UserPreview>()

            for (suscriptorId in suscriptoresIds) {
                try {
                    val profileResult = getPerfilUsuario(suscriptorId)
                    profileResult.fold(
                        onSuccess = { perfil ->
                            if (perfil != null) {
                                val userPreview = UserPreview(
                                    userId = perfil.userId,
                                    nickname = perfil.nickname,
                                    fullName = perfil.fullName ?: perfil.nickname,
                                    description = perfil.description ?: "Suscriptor activo",
                                    totalScore = perfil.totalScore,
                                    nivel = perfil.nivel,
                                    tipo = perfil.tipo,
                                    rankingPreference = perfil.rankingPreference,
                                    createdAt = perfil.createdAt,
                                    photoUrl = generateThumbnailUrl(perfil.userId),
                                    isVerified = perfil.isVerified ?: false
                                )
                                users.add(userPreview)
                                Log.d(TAG, "💳 [SUSCR_DEBUG] Suscriptor obtenido: ${perfil.nickname}")
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error obteniendo perfil de suscriptor $suscriptorId", e)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando suscriptor $suscriptorId", e)
                }
            }

            Log.d(TAG, "💳 [SUSCR_DEBUG] ✅ Detalles de suscriptores obtenidos: ${users.size}")
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "💳 [SUSCR_DEBUG] ❌ Error obteniendo detalles de suscriptores", e)
            Result.failure(e)
        }
    }


    // ================================================================================================
    // FUNCIONES DE PATROCINIO
    // ================================================================================================

    /**
     * Patrocinar usuario a otro usuario
     * ESTRUCTURA ESCALABLE CON SUBCOLECCIONES Y TRANSACCIONES:
     * users/{patrocinadorId}/patrocinios/{patrocinadoId} - registro de patrocinio (1:N)
     * users/{patrocinadoId}/patrocinadores/{patrocinadorId} - registro de patrocinador (N:N)
     * userStats/{userId} - contadores automáticos
     */
    suspend fun patrocinarUsuario(patrocinadorId: String, patrocinadoId: String, valorPatrocinio: String = "EUR 70/mes"): Result<Boolean> {
        return try {
            Log.d(TAG, "💰 [PATR_DEBUG] Usuario $patrocinadorId patrocinando a $patrocinadoId")

            // Usar transacción para operación atómica
            firestore.runTransaction { transaction ->
                val currentTimestamp = System.currentTimeMillis()
                val expirationTimestamp = currentTimestamp + (30L * 24 * 60 * 60 * 1000) // 30 días

                // Referencias para subcolecciones escalables
                val patrocinioRef = firestore.collection(USERS_COLLECTION)
                    .document(patrocinadorId)
                    .collection("patrocinios")
                    .document(patrocinadoId)

                val patrocinadorRef = firestore.collection(USERS_COLLECTION)
                    .document(patrocinadoId)
                    .collection("patrocinadores")
                    .document(patrocinadorId)

                // Referencias para userStats
                val patrocinadorStatsRef = firestore.collection("userStats").document(patrocinadorId)
                val patrocinadoStatsRef = firestore.collection("userStats").document(patrocinadoId)

                // Datos para subcolección patrocinios (1:N)
                val patrocinioData = mapOf(
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "patrocinadoId" to patrocinadoId,
                    "valorPatrocinio" to valorPatrocinio,
                    "fechaInicio" to currentTimestamp,
                    "fechaFin" to expirationTimestamp,
                    "tipo" to "premium",
                    "estado" to "activo",
                    "renovacionAutomatica" to true
                )

                // Datos para documento único patrocinador (N:1)
                val patrocinadorData = mapOf(
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "patrocinadorId" to patrocinadorId,
                    "valorPatrocinio" to valorPatrocinio,
                    "fechaInicio" to currentTimestamp,
                    "fechaFin" to expirationTimestamp,
                    "tipo" to "premium",
                    "estado" to "activo",
                    "renovacionAutomatica" to true
                )

                // Crear relaciones en sistema escalable
                transaction.set(patrocinioRef, patrocinioData)
                transaction.set(patrocinadorRef, patrocinadorData)

                // Actualizar contadores en userStats
                transaction.update(patrocinadorStatsRef, "patrociniosCount", com.google.firebase.firestore.FieldValue.increment(1))
                transaction.update(patrocinadoStatsRef, "esPatrocinado", true)
                transaction.update(patrocinadoStatsRef, "valorPatrocinioRecibido", valorPatrocinio)
                transaction.update(patrocinadoStatsRef, "patrocinadorId", patrocinadorId)

                Log.d(TAG, "💰 [PATR_DEBUG] ✅ Patrocinio creado exitosamente en sistema escalable")
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATR_DEBUG] ❌ Error en patrocinio", e)
            Result.failure(e)
        }
    }

    /**
     * Cancelar patrocinio de usuario
     * ESTRUCTURA ESCALABLE CON SUBCOLECCIONES Y TRANSACCIONES:
     * users/{patrocinadorId}/patrocinios/{patrocinadoId} - actualizar estado
     * users/{patrocinadoId}/patrocinadores/{patrocinadorId} - actualizar estado
     * userStats/{userId} - actualizar contadores
     */
    suspend fun cancelarPatrocinio(patrocinadorId: String, patrocinadoId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "💰 [PATR_DEBUG] Usuario $patrocinadorId cancelando patrocinio a $patrocinadoId")

            // Usar transacción para operación atómica
            firestore.runTransaction { transaction ->
                // Referencias para subcolecciones escalables
                val patrocinioRef = firestore.collection(USERS_COLLECTION)
                    .document(patrocinadorId)
                    .collection("patrocinios")
                    .document(patrocinadoId)

                val patrocinadorRef = firestore.collection(USERS_COLLECTION)
                    .document(patrocinadoId)
                    .collection("patrocinadores")
                    .document(patrocinadorId)

                // Referencias para userStats
                val patrocinadorStatsRef = firestore.collection("userStats").document(patrocinadorId)
                val patrocinadoStatsRef = firestore.collection("userStats").document(patrocinadoId)

                // Verificar si el patrocinio existe
                val patrocinioDoc = transaction.get(patrocinioRef)
                if (patrocinioDoc.exists()) {
                    // Actualizar estado a cancelado (mantener historial)
                    val cancelacionData = mapOf(
                        "estado" to "cancelado",
                        "fechaCancelacion" to System.currentTimeMillis(),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    transaction.update(patrocinioRef, cancelacionData)
                    transaction.update(patrocinadorRef, cancelacionData)

                    // Actualizar contadores en userStats
                    transaction.update(patrocinadorStatsRef, "patrociniosCount", com.google.firebase.firestore.FieldValue.increment(-1))
                    transaction.update(patrocinadoStatsRef, "esPatrocinado", false)
                    transaction.update(patrocinadoStatsRef, "valorPatrocinioRecibido", "")
                    transaction.update(patrocinadoStatsRef, "patrocinadorId", "")

                    Log.d(TAG, "💰 [PATR_DEBUG] ✅ Patrocinio cancelado exitosamente")
                } else {
                    Log.w(TAG, "💰 [PATR_DEBUG] No se encontró patrocinio activo para cancelar")
                }
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATR_DEBUG] ❌ Error cancelando patrocinio", e)
            Result.failure(e)
        }
    }

    /**
     * Verificar si un usuario está patrocinando a otro
     * Consulta: users/{patrocinadorId}/patrocinios/{patrocinadoId} exists + estado activo
     */
    suspend fun estaPatrocinando(patrocinadorId: String, patrocinadoId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "💰 [PATR_DEBUG] Verificando si $patrocinadorId está patrocinando a $patrocinadoId")

            // Verificar existencia en subcolección
            val patrocinioDoc = firestore.collection(USERS_COLLECTION)
                .document(patrocinadorId)
                .collection("patrocinios")
                .document(patrocinadoId)
                .get()
                .await()

            val estaPatrocinando = if (patrocinioDoc.exists()) {
                val estado = patrocinioDoc.getString("estado")
                estado == "activo"
            } else {
                false
            }

            Log.d(TAG, "💰 [PATR_DEBUG] Usuario está patrocinando: $estaPatrocinando")
            Result.success(estaPatrocinando)
        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATR_DEBUG] ❌ Error verificando patrocinio", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener información del patrocinio activo
     * Consulta: users/{patrocinadorId}/patrocinios/{patrocinadoId} documento específico
     */
    suspend fun getPatrocinioInfo(patrocinadorId: String, patrocinadoId: String): Result<Map<String, Any>?> {
        return try {
            Log.d(TAG, "💰 [PATR_DEBUG] Obteniendo info del patrocinio $patrocinadorId → $patrocinadoId")

            // Obtener documento específico de subcolección
            val patrocinioDoc = firestore.collection(USERS_COLLECTION)
                .document(patrocinadorId)
                .collection("patrocinios")
                .document(patrocinadoId)
                .get()
                .await()

            if (patrocinioDoc.exists() && patrocinioDoc.getString("estado") == "activo") {
                val patrocinioInfo = patrocinioDoc.data
                Log.d(TAG, "💰 [PATR_DEBUG] ✅ Info del patrocinio obtenida: $patrocinioInfo")
                Result.success(patrocinioInfo)
            } else {
                Log.d(TAG, "💰 [PATR_DEBUG] No se encontró patrocinio activo")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATR_DEBUG] ❌ Error obteniendo info del patrocinio", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener el patrocinador actual de un usuario
     * Consulta: users/{patrocinadoId}/patrocinador/current documento único
     */
    suspend fun getPatrocinadorActual(patrocinadoId: String): Result<PerfilUsuario?> {
        return try {
            Log.d(TAG, "💰 [PATR_DEBUG] Buscando patrocinador actual para usuario: $patrocinadoId")

            // Obtener documento único de patrocinador
            val patrocinadorDoc = firestore.collection(USERS_COLLECTION)
                .document(patrocinadoId)
                .collection("patrocinador")
                .document("current")
                .get()
                .await()

            if (patrocinadorDoc.exists() && patrocinadorDoc.getString("estado") == "activo") {
                val patrocinadorId = patrocinadorDoc.getString("patrocinadorId")

                if (patrocinadorId != null) {
                    Log.d(TAG, "💰 [PATR_DEBUG] ✅ Patrocinador encontrado: $patrocinadorId")

                    // Obtener perfil completo del patrocinador
                    val perfilResult = getPerfilUsuario(patrocinadorId)
                    perfilResult.fold(
                        onSuccess = { perfil ->
                            Log.d(TAG, "💰 [PATR_DEBUG] ✅ Perfil del patrocinador obtenido: ${perfil?.nickname}")
                            Result.success(perfil)
                        },
                        onFailure = { error ->
                            Log.e(TAG, "💰 [PATR_DEBUG] ❌ Error obteniendo perfil del patrocinador", error)
                            Result.failure(error)
                        }
                    )
                } else {
                    Log.w(TAG, "💰 [PATR_DEBUG] patrocinadorId es null en el documento")
                    Result.success(null)
                }
            } else {
                Log.d(TAG, "💰 [PATR_DEBUG] No se encontró patrocinador activo")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATR_DEBUG] ❌ Error buscando patrocinador actual", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener IDs de usuarios a los que patrocino
     * Similar a getSuscripcionesIds() pero para patrocinios
     */
    suspend fun getPatrociniosIds(userId: String): Result<List<String>> {
        return try {
            Log.d(TAG, "💰 [PATROCINIO_DEBUG] Obteniendo IDs de usuarios patrocinados por: $userId")

            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("patrocinios")
                .whereEqualTo("estado", "activo")
                .get()
                .await()

            val patrociniosIds = querySnapshot.documents.mapNotNull { doc ->
                doc.id // Document ID es el userId del patrocinado
            }

            Log.d(TAG, "💰 [PATROCINIO_DEBUG] ✅ Patrocinios encontrados: ${patrociniosIds.size}")
            Result.success(patrociniosIds)

        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATROCINIO_DEBUG] ❌ Error obteniendo patrocinios", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener IDs de usuarios que me patrocinan
     * Consulta userStats para obtener contadores o fallback a consulta directa
     */
    suspend fun getPatrocinadoresIds(userId: String): Result<List<String>> {
        return try {
            Log.d(TAG, "💰 [PATROCINIO_DEBUG] Obteniendo IDs de patrocinadores para: $userId")

            // NUEVA ESTRUCTURA: users/{userId}/patrocinadores/{patrocinadorId}
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("patrocinadores")
                .whereEqualTo("estado", "activo")
                .get()
                .await()

            val patrocinadoresIds = querySnapshot.documents.mapNotNull { doc ->
                doc.id // Document ID es el userId del patrocinador
            }.toMutableList()

            Log.d(TAG, "💰 [PATROCINIO_DEBUG] 🆕 Estructura nueva: ${patrocinadoresIds.size} patrocinadores")

            // FALLBACK LEGACY: users/{userId}/patrocinador/current (temporal)
            if (patrocinadoresIds.isEmpty()) {
                Log.d(TAG, "💰 [PATROCINIO_DEBUG] 🔄 Intentando estructura legacy...")

                val legacyDoc = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection("patrocinador")
                    .document("current")
                    .get()
                    .await()

                if (legacyDoc.exists() && legacyDoc.getString("estado") == "activo") {
                    val patrocinadorId = legacyDoc.getString("patrocinadorId")
                    if (!patrocinadorId.isNullOrEmpty()) {
                        patrocinadoresIds.add(patrocinadorId)
                        Log.d(TAG, "💰 [PATROCINIO_DEBUG] 🔄 Legacy encontrado: $patrocinadorId")
                    }
                }
            }

            Log.d(TAG, "💰 [PATROCINIO_DEBUG] ✅ Total patrocinadores encontrados: ${patrocinadoresIds.size}")
            Result.success(patrocinadoresIds.toList())

        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATROCINIO_DEBUG] ❌ Error obteniendo patrocinadores", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener detalles completos de usuarios que patrocino
     * Similar a getSuscripcionesWithDetails() pero para patrocinios
     */
    suspend fun getPatrociniosWithDetails(userId: String): Result<List<UserPreview>> {
        return try {
            Log.d(TAG, "💰 [PATROCINIO_DEBUG] Obteniendo detalles de patrocinios para: $userId")

            val patrociniosIdsResult = getPatrociniosIds(userId)
            if (patrociniosIdsResult.isFailure) {
                return Result.failure(patrociniosIdsResult.exceptionOrNull()!!)
            }

            val patrociniosIds = patrociniosIdsResult.getOrNull() ?: emptyList()
            val users = mutableListOf<UserPreview>()

            for (patrocinadoId in patrociniosIds) {
                try {
                    val profileResult = getPerfilUsuario(patrocinadoId)
                    profileResult.fold(
                        onSuccess = { perfil ->
                            if (perfil != null) {
                                val userPreview = UserPreview(
                                    userId = perfil.userId,
                                    nickname = perfil.nickname,
                                    fullName = perfil.fullName ?: perfil.nickname,
                                    description = perfil.description ?: "Patrocinio activo",
                                    totalScore = perfil.totalScore,
                                    nivel = perfil.nivel,
                                    tipo = perfil.tipo,
                                    rankingPreference = perfil.rankingPreference,
                                    createdAt = perfil.createdAt,
                                    photoUrl = generateThumbnailUrl(perfil.userId),
                                    isVerified = perfil.isVerified ?: false
                                )
                                users.add(userPreview)
                                Log.d(TAG, "💰 [PATROCINIO_DEBUG] Usuario patrocinado obtenido: ${perfil.nickname}")
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error obteniendo perfil de usuario patrocinado $patrocinadoId", e)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando usuario patrocinado $patrocinadoId", e)
                }
            }

            Log.d(TAG, "💰 [PATROCINIO_DEBUG] ✅ Detalles de patrocinios obtenidos: ${users.size}")
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATROCINIO_DEBUG] ❌ Error obteniendo detalles de patrocinios", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener detalles completos de usuarios que me patrocinan
     * Similar a getSuscriptoresWithDetails() pero para patrocinadores
     */
    suspend fun getPatrocinadoresWithDetails(userId: String): Result<List<UserPreview>> {
        return try {
            Log.d(TAG, "💰 [PATROCINIO_DEBUG] Obteniendo detalles de patrocinadores para: $userId")

            val patrocinadoresIdsResult = getPatrocinadoresIds(userId)
            if (patrocinadoresIdsResult.isFailure) {
                return Result.failure(patrocinadoresIdsResult.exceptionOrNull()!!)
            }

            val patrocinadoresIds = patrocinadoresIdsResult.getOrNull() ?: emptyList()
            val users = mutableListOf<UserPreview>()

            for (patrocinadorId in patrocinadoresIds) {
                try {
                    val profileResult = getPerfilUsuario(patrocinadorId)
                    profileResult.fold(
                        onSuccess = { perfil ->
                            if (perfil != null) {
                                val userPreview = UserPreview(
                                    userId = perfil.userId,
                                    nickname = perfil.nickname,
                                    fullName = perfil.fullName ?: perfil.nickname,
                                    description = perfil.description ?: "Patrocinador activo",
                                    totalScore = perfil.totalScore,
                                    nivel = perfil.nivel,
                                    tipo = perfil.tipo,
                                    rankingPreference = perfil.rankingPreference,
                                    createdAt = perfil.createdAt,
                                    photoUrl = generateThumbnailUrl(perfil.userId),
                                    isVerified = perfil.isVerified ?: false
                                )
                                users.add(userPreview)
                                Log.d(TAG, "💰 [PATROCINIO_DEBUG] Patrocinador obtenido: ${perfil.nickname}")
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error obteniendo perfil de patrocinador $patrocinadorId", e)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando patrocinador $patrocinadorId", e)
                }
            }

            Log.d(TAG, "💰 [PATROCINIO_DEBUG] ✅ Detalles de patrocinadores obtenidos: ${users.size}")
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATROCINIO_DEBUG] ❌ Error obteniendo detalles de patrocinadores", e)
            Result.failure(e)
        }
    }

    /**
     * Actualizar configuración de patrocinio del usuario
     * Similar a updateUserSubscriptionConfig() pero para patrocinios
     */
    suspend fun updateUserPatrocinioConfig(
        userId: String,
        price: String,
        duration: String,
        isEnabled: Boolean,
        currency: String,
        description: String
    ): Result<PerfilUsuario?> {
        return try {
            Log.d(TAG, "💰 [PATROCINIO_CONFIG] Actualizando configuración de patrocinio para userId: $userId")
            Log.d(TAG, "💰 [PATROCINIO_CONFIG] Nuevos valores - price: $price, duration: $duration, isEnabled: $isEnabled, currency: $currency")

            // Obtener configuración actual para preservar opciones existentes
            val currentProfile = getPerfilUsuario(userId).getOrNull()
            val currentOptions = currentProfile?.patrocinioConfig?.options ?: emptyList()

            // Actualizar la primera opción o crear una nueva
            val updatedOptions = if (currentOptions.isNotEmpty()) {
                // Actualizar primera opción existente
                currentOptions.mapIndexed { index, option ->
                    if (index == 0) {
                        option.copy(
                            price = price,
                            duration = duration,
                            durationInDays = when (duration) {
                                "1 mes" -> 30
                                "3 meses" -> 90
                                "anual" -> 365
                                else -> 30
                            },
                            displayName = "Plan Patrocinio ${duration.replaceFirstChar { it.uppercase() }}"
                        )
                    } else option
                }
            } else {
                // Crear primera opción si no existe
                listOf(
                    mapOf(
                        "id" to java.util.UUID.randomUUID().toString(),
                        "price" to price,
                        "duration" to duration,
                        "durationInDays" to when (duration) {
                            "1 mes" -> 30
                            "3 meses" -> 90
                            "anual" -> 365
                            else -> 30
                        },
                        "displayName" to "Plan Patrocinio ${duration.replaceFirstChar { it.uppercase() }}",
                        "isActive" to true
                    )
                )
            }

            // Estructura para actualizar campos separados en Firestore
            val updateData = mutableMapOf<String, Any>(
                "patrocinioConfigIsEnabled" to isEnabled,
                "patrocinioConfigCurrency" to currency,
                "patrocinioConfigDescription" to description,
                "patrocinioConfigOptions" to updatedOptions
            )

            Log.d(TAG, "💰 [PATROCINIO_CONFIG] Datos a actualizar: $updateData")

            // Actualizar en Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updateData)
                .await()

            Log.d(TAG, "💰 [PATROCINIO_CONFIG] ✅ Configuración de patrocinio actualizada exitosamente")

            // Recargar perfil actualizado
            getPerfilUsuario(userId)

        } catch (e: Exception) {
            Log.e(TAG, "💰 [PATROCINIO_CONFIG] ❌ Error actualizando configuración de patrocinio", e)
            Result.failure(e)
        }
    }

    // ================================================================================================
    // FUNCIONES DE RANKING POSICIÓN
    // ================================================================================================

    /**
     * Calcular la posición del usuario en el ranking según su preferencia
     * Retorna: Pair(posición como "1º", ámbito como "Madrid/España/Mundial")
     */
    suspend fun getUserRankingPosition(userId: String): Result<Pair<String, String>> {
        return try {
            Log.d(TAG, "🏆 [RANKING_POS] Calculando posición para usuario: $userId")

            // Obtener perfil del usuario para conocer su preferencia y ubicación
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                Log.w(TAG, "🏆 [RANKING_POS] Usuario no encontrado")
                return Result.success(Pair("N/A", "Sin datos"))
            }

            val rankingPreference = userDoc.getString("rankingPreference")?.lowercase() ?: "local"
            val userTotalScore = userDoc.getLong("totalScore") ?: 0L
            val nickname = userDoc.getString("nickname") ?: "Usuario"

            // Obtener ubicación del usuario
            val ubicacionMap = userDoc.get("ubicacion") as? Map<String, Any>
            val ciudad = (ubicacionMap?.get("ciudad") as? String)?.trim() ?: ""
            val provincia = (ubicacionMap?.get("provincia") as? String)?.trim() ?: ""
            val pais = (ubicacionMap?.get("pais") as? String)?.trim() ?: ""

            Log.d(TAG, "🏆 [RANKING_POS] Usuario: $nickname")
            Log.d(TAG, "🏆 [RANKING_POS] Preferencia: '$rankingPreference', Score: $userTotalScore")
            Log.d(TAG, "🏆 [RANKING_POS] Ubicación: ciudad='$ciudad', provincia='$provincia', pais='$pais'")

            // Calcular posición según preferencia (case-insensitive)
            val (position, scope) = when (rankingPreference) {
                "local" -> calculateLocalPosition(userId, userTotalScore, ciudad)
                "provincial" -> calculateProvincialPosition(userId, userTotalScore, provincia)
                "nacional" -> calculateNacionalPosition(userId, userTotalScore, pais)
                "mundial", "global" -> calculateMundialPosition(userId, userTotalScore)
                else -> {
                    Log.w(TAG, "🏆 [RANKING_POS] Preferencia desconocida: '$rankingPreference', usando local como fallback")
                    calculateLocalPosition(userId, userTotalScore, ciudad)
                }
            }

            val positionText = if (position > 0) "${position}º" else "N/A"

            Log.d(TAG, "🏆 [RANKING_POS] ✅ Posición calculada: $positionText en '$scope'")
            Result.success(Pair(positionText, scope))

        } catch (e: Exception) {
            Log.e(TAG, "🏆 [RANKING_POS] ❌ Error calculando posición", e)
            Result.failure(e)
        }
    }

    /**
     * Calcular posición en ranking local (ciudad)
     * Usa la misma estrategia que getRankingLocal que ya funciona
     */
    private suspend fun calculateLocalPosition(userId: String, userScore: Long, ciudad: String): Pair<Int, String> {
        return try {
            if (ciudad.isBlank()) {
                Log.w(TAG, "🏆 [RANKING_POS] Ciudad vacía, usando ranking mundial como fallback")
                return calculateMundialPosition(userId, userScore)
            }

            Log.d(TAG, "🏆 [RANKING_POS] Calculando posición local en ciudad: '$ciudad'")

            // Usar la misma consulta que getRankingLocal (que funciona)
            val documents = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("ubicacion.ciudad", ciudad)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .get()
                .await()

            // Encontrar la posición del usuario en la lista ordenada
            var position = 0
            for ((index, doc) in documents.withIndex()) {
                if (doc.id == userId) {
                    position = index + 1
                    break
                }
            }

            if (position == 0) {
                Log.w(TAG, "🏆 [RANKING_POS] Usuario no encontrado en ranking local de '$ciudad'")
                position = documents.size() + 1 // Al final si no se encuentra
            }

            Log.d(TAG, "🏆 [RANKING_POS] Local: $position de ${documents.size()} en '$ciudad'")
            Pair(position, ciudad)
        } catch (e: Exception) {
            Log.e(TAG, "🏆 [RANKING_POS] Error en ranking local para ciudad '$ciudad'", e)
            Pair(0, ciudad.ifBlank { "Sin ciudad" })
        }
    }

    /**
     * Calcular posición en ranking provincial
     * Usa la misma estrategia que getRankingProvincial que ya funciona
     */
    private suspend fun calculateProvincialPosition(userId: String, userScore: Long, provincia: String): Pair<Int, String> {
        return try {
            if (provincia.isBlank()) {
                Log.w(TAG, "🏆 [RANKING_POS] Provincia vacía, usando ranking mundial como fallback")
                return calculateMundialPosition(userId, userScore)
            }

            Log.d(TAG, "🏆 [RANKING_POS] Calculando posición provincial en: '$provincia'")

            // Usar la misma consulta que getRankingProvincial (que funciona)
            val documents = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("ubicacion.provincia", provincia)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .get()
                .await()

            // Encontrar la posición del usuario en la lista ordenada
            var position = 0
            for ((index, doc) in documents.withIndex()) {
                if (doc.id == userId) {
                    position = index + 1
                    break
                }
            }

            if (position == 0) {
                Log.w(TAG, "🏆 [RANKING_POS] Usuario no encontrado en ranking provincial de '$provincia'")
                position = documents.size() + 1 // Al final si no se encuentra
            }

            Log.d(TAG, "🏆 [RANKING_POS] Provincial: $position de ${documents.size()} en '$provincia'")
            Pair(position, provincia)
        } catch (e: Exception) {
            Log.e(TAG, "🏆 [RANKING_POS] Error en ranking provincial para '$provincia'", e)
            Pair(0, provincia.ifBlank { "Sin provincia" })
        }
    }

    /**
     * Calcular posición en ranking nacional
     * Usa la misma estrategia que getRankingNacional que ya funciona
     */
    private suspend fun calculateNacionalPosition(userId: String, userScore: Long, pais: String): Pair<Int, String> {
        return try {
            if (pais.isBlank()) {
                Log.w(TAG, "🏆 [RANKING_POS] País vacío, usando ranking mundial como fallback")
                return calculateMundialPosition(userId, userScore)
            }

            Log.d(TAG, "🏆 [RANKING_POS] Calculando posición nacional en: '$pais'")

            // Usar la misma consulta que getRankingNacional (que funciona)
            val documents = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("ubicacion.pais", pais)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .get()
                .await()

            // Encontrar la posición del usuario en la lista ordenada
            var position = 0
            for ((index, doc) in documents.withIndex()) {
                if (doc.id == userId) {
                    position = index + 1
                    break
                }
            }

            if (position == 0) {
                Log.w(TAG, "🏆 [RANKING_POS] Usuario no encontrado en ranking nacional de '$pais'")
                position = documents.size() + 1 // Al final si no se encuentra
            }

            Log.d(TAG, "🏆 [RANKING_POS] Nacional: $position de ${documents.size()} en '$pais'")
            Pair(position, pais)
        } catch (e: Exception) {
            Log.e(TAG, "🏆 [RANKING_POS] Error en ranking nacional para '$pais'", e)
            Pair(0, pais.ifBlank { "Sin país" })
        }
    }

    /**
     * Calcular posición en ranking mundial
     * Usa la misma estrategia que getRankingMundial que ya funciona
     */
    private suspend fun calculateMundialPosition(userId: String, userScore: Long): Pair<Int, String> {
        return try {
            Log.d(TAG, "🏆 [RANKING_POS] Calculando posición mundial")

            // Usar la misma consulta que getRankingMundial (que funciona)
            val documents = firestore.collection(USERS_COLLECTION)
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .get()
                .await()

            // Encontrar la posición del usuario en la lista ordenada
            var position = 0
            for ((index, doc) in documents.withIndex()) {
                if (doc.id == userId) {
                    position = index + 1
                    break
                }
            }

            if (position == 0) {
                Log.w(TAG, "🏆 [RANKING_POS] Usuario no encontrado en ranking mundial")
                position = documents.size() + 1 // Al final si no se encuentra
            }

            Log.d(TAG, "🏆 [RANKING_POS] Mundial: $position de ${documents.size()} usuarios globalmente")
            Pair(position, "Mundial")
        } catch (e: Exception) {
            Log.e(TAG, "🏆 [RANKING_POS] Error en ranking mundial", e)
            Pair(0, "Mundial")
        }
    }
}

/**
 * Extensión para convertir DocumentSnapshot a PerfilUsuario
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toPerfilUsuario(): PerfilUsuario? {
    return try {
        // Obtener ubicación desde Firestore
        val ubicacion = getUbicacionFromDocument()

        PerfilUsuario(
            userId = id,
            nickname = getString("nickname") ?: "",
            fullName = getString("fullName") ?: "",
            description = getString("description") ?: "",
            totalScore = getLong("totalScore")?.toInt() ?: 0,
            nivel = getLong("nivel")?.toInt() ?: 1,
            tipo = getString("tipo") ?: "user",
            ubicacion = ubicacion,
            rankingPreference = getString("rankingPreference") ?: "local",
            createdAt = getLong("createdAt") ?: 0L,
            photoUrl = getString("photoUrl") ?: "",
            email = getString("email") ?: "",
            seguidores = getLong("seguidores")?.toInt() ?: 0,
            siguiendo = getLong("siguiendo")?.toInt() ?: 0,
            isVerified = getBoolean("isVerified") ?: false,
            donacion = getBoolean("donacion") ?: false, // Mapear campo donacion
            mostrarEstado = getBoolean("mostrarEstado") ?: true, // Control de privacidad estado en línea
            subscriptionConfig = getSubscriptionConfigFromDocument(), // Mapear configuración de suscripciones
            patrocinioConfig = getPatrocinioConfigFromDocument() // Mapear configuración de patrocinios
        )
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error convirtiendo documento a PerfilUsuario", e)
        null
    }
}

/**
 * Función para extraer ubicación de un DocumentSnapshot de Firestore
 */
private fun com.google.firebase.firestore.DocumentSnapshot.getUbicacionFromDocument(): Ubicacion {
    return try {
        // La ubicación puede estar como un subcampo o directamente en el documento
        val ubicacionMap = get("ubicacion") as? Map<String, Any>

        if (ubicacionMap != null) {
            // Ubicación está como subcampo (estructura anidada)
            Ubicacion(
                ciudad = ubicacionMap["ciudad"] as? String ?: "",
                provincia = ubicacionMap["provincia"] as? String ?: "",
                pais = ubicacionMap["pais"] as? String ?: "",
                countryCode = ubicacionMap["countryCode"] as? String ?: "",
                formattedAddress = ubicacionMap["formattedAddress"] as? String ?: "",
                lat = ubicacionMap["lat"] as? Double,
                lng = ubicacionMap["lng"] as? Double,
                placeId = ubicacionMap["placeId"] as? String ?: "",
                privacyLevel = ubicacionMap["privacyLevel"] as? String ?: "city"
            )
        } else {
            // Ubicación puede estar directamente en el documento (campos planos)
            Ubicacion(
                ciudad = getString("ciudad") ?: "",
                provincia = getString("provincia") ?: "",
                pais = getString("pais") ?: "",
                countryCode = getString("countryCode") ?: "",
                formattedAddress = getString("formattedAddress") ?: "",
                lat = getDouble("lat"),
                lng = getDouble("lng"),
                placeId = getString("placeId") ?: "",
                privacyLevel = getString("privacyLevel") ?: "city"
            )
        }
    } catch (e: Exception) {
        Log.w("FirestoreRepository", "Error parseando ubicación, usando ubicación por defecto", e)
        // Ubicación por defecto si hay error
        Ubicacion(
            ciudad = "",
            provincia = "",
            pais = "",
            countryCode = "",
            formattedAddress = "",
            lat = null,
            lng = null,
            placeId = "",
            privacyLevel = "city"
        )
    }

}

/**
 * Función para extraer configuración de suscripciones de un DocumentSnapshot de Firestore
 */
private fun com.google.firebase.firestore.DocumentSnapshot.getSubscriptionConfigFromDocument(): SubscriptionConfig {
    return try {
        Log.d("FirestoreRepository", "🔍 [SUBSCRIPTION_DEBUG] Parseando subscriptionConfig APK estructura para userId: $id")

        // Debug: Mostrar todas las claves disponibles en el documento
        val allData = data
        Log.d("FirestoreRepository", "🔍 [SUBSCRIPTION_DEBUG] Todas las claves del documento: ${allData?.keys}")

        // Debug: Verificar si hay un objeto subscriptionConfig anidado
        val subscriptionConfigObject = get("subscriptionConfig")
        Log.d("FirestoreRepository", "🔍 [SUBSCRIPTION_DEBUG] Objeto subscriptionConfig: $subscriptionConfigObject (type: ${subscriptionConfigObject?.javaClass?.simpleName})")

        // Variables para estructura APK
        var isEnabled = false
        var currency = "€"
        var description = "¡Únete a mi mundo en Biihlive! Suscríbete y no te pierdas ninguna de mis transmisiones en vivo."
        var options: List<SubscriptionOption> = emptyList()

        if (subscriptionConfigObject is Map<*, *>) {
            Log.d("FirestoreRepository", "🔍 [SUBSCRIPTION_DEBUG] Leyendo desde objeto anidado subscriptionConfig")
            val configMap = subscriptionConfigObject as Map<String, Any>
            Log.d("FirestoreRepository", "🔍 [SUBSCRIPTION_DEBUG] Claves en configMap: ${configMap.keys}")

            currency = configMap["currency"] as? String ?: "€"
            description = configMap["description"] as? String ?: "¡Únete a mi mundo en Biihlive! Suscríbete y no te pierdas ninguna de mis transmisiones en vivo."

            val isEnabledRaw = configMap["isEnabled"]
            Log.d("FirestoreRepository", "🔍 [SUBSCRIPTION_DEBUG] Raw isEnabled desde map: $isEnabledRaw (type: ${isEnabledRaw?.javaClass?.simpleName})")

            isEnabled = when (isEnabledRaw) {
                is Boolean -> isEnabledRaw
                is String -> isEnabledRaw.lowercase() == "true"
                is Long -> isEnabledRaw != 0L
                is Double -> isEnabledRaw != 0.0
                else -> false
            }

            // Leer array de opciones según estructura APK
            val optionsRaw = configMap["options"]
            options = when (optionsRaw) {
                is List<*> -> {
                    optionsRaw.mapNotNull { optionData ->
                        if (optionData is Map<*, *>) {
                            val optionMap = optionData as Map<String, Any>
                            try {
                                SubscriptionOption(
                                    id = optionMap["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                                    price = optionMap["price"] as? String ?: "9.99",
                                    duration = optionMap["duration"] as? String ?: "1 mes",
                                    durationInDays = (optionMap["durationInDays"] as? Number)?.toInt() ?: 30,
                                    displayName = optionMap["displayName"] as? String ?: "Plan Mensual",
                                    isActive = optionMap["isActive"] as? Boolean ?: true
                                )
                            } catch (e: Exception) {
                                Log.w("FirestoreRepository", "Error parseando opción: $optionData", e)
                                null
                            }
                        } else null
                    }
                }
                else -> {
                    // Si no hay opciones en estructura APK, crear una por defecto a partir de campos legacy
                    val legacyPrice = configMap["price"] as? String ?: "9.99"
                    val legacyDuration = configMap["duration"] as? String ?: "1 mes"
                    listOf(
                        SubscriptionOption(
                            price = legacyPrice,
                            duration = legacyDuration,
                            durationInDays = when (legacyDuration) {
                                "1 mes" -> 30
                                "3 meses" -> 90
                                "anual" -> 365
                                else -> 30
                            },
                            displayName = "Plan ${legacyDuration.replaceFirstChar { it.uppercase() }}"
                        )
                    )
                }
            }
        } else {
            // Fallback para estructura legacy o sin subscriptionConfig
            Log.d("FirestoreRepository", "🔍 [SUBSCRIPTION_DEBUG] Usando fallback a configuración por defecto")
            options = listOf(
                SubscriptionOption(
                    price = "9.99",
                    duration = "1 mes",
                    durationInDays = 30,
                    displayName = "Plan Mensual"
                )
            )
        }

        Log.d("FirestoreRepository", "✅ [SUBSCRIPTION_DEBUG] Valores finales APK: isEnabled=$isEnabled, currency=$currency, options=${options.size}")

        val config = SubscriptionConfig(
            isEnabled = isEnabled,
            currency = currency,
            description = description,
            options = options
        )

        Log.d("FirestoreRepository", "🔍 [SUBSCRIPTION_DEBUG] Config final: $config")
        return config

    } catch (e: Exception) {
        Log.e("FirestoreRepository", "❌ [SUBSCRIPTION_DEBUG] Error parseando configuración de suscripción", e)
        // Configuración por defecto si hay error
        return SubscriptionConfig()
    }
}

/**
 * Función para extraer configuración de patrocinios de un DocumentSnapshot de Firestore
 */
private fun com.google.firebase.firestore.DocumentSnapshot.getPatrocinioConfigFromDocument(): PatrocinioConfig {
    return try {
        Log.d("FirestoreRepository", "🔍 [PATROCINIO_DEBUG] Parseando patrocinioConfig APK estructura para userId: $id")

        val patrocinioConfigObject = get("patrocinioConfig")
        Log.d("FirestoreRepository", "🔍 [PATROCINIO_DEBUG] Objeto patrocinioConfig: $patrocinioConfigObject")

        if (patrocinioConfigObject is Map<*, *>) {
            val configMap = patrocinioConfigObject as Map<String, Any>
            Log.d("FirestoreRepository", "🔍 [PATROCINIO_DEBUG] Claves en configMap: ${configMap.keys}")

            // Leer campos de nivel superior según estructura APK
            val isEnabled = when (val isEnabledRaw = configMap["isEnabled"]) {
                is Boolean -> isEnabledRaw
                is String -> isEnabledRaw.lowercase() == "true"
                else -> false
            }
            val currency = configMap["currency"] as? String ?: "€"
            val description = configMap["description"] as? String ?:
                "¡Patrocina mi contenido en Biihlive! Ayúdame a seguir creando y forma parte de mi comunidad exclusiva."

            // Leer array de opciones
            val optionsRaw = configMap["options"]
            val options = when (optionsRaw) {
                is List<*> -> {
                    optionsRaw.mapNotNull { optionData ->
                        if (optionData is Map<*, *>) {
                            val optionMap = optionData as Map<String, Any>
                            try {
                                SubscriptionOption(
                                    id = optionMap["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                                    price = optionMap["price"] as? String ?: "19.99",
                                    duration = optionMap["duration"] as? String ?: "1 mes",
                                    durationInDays = (optionMap["durationInDays"] as? Number)?.toInt() ?: 30,
                                    displayName = optionMap["displayName"] as? String ?: "Plan Patrocinio Mensual",
                                    isActive = optionMap["isActive"] as? Boolean ?: true
                                )
                            } catch (e: Exception) {
                                Log.w("FirestoreRepository", "Error parseando opción patrocinio: $optionData", e)
                                null
                            }
                        } else null
                    }
                }
                else -> {
                    // Si no hay opciones en estructura APK, crear una por defecto a partir de campos legacy
                    val legacyPrice = configMap["price"] as? String ?: "19.99"
                    val legacyDuration = configMap["duration"] as? String ?: "1 mes"
                    listOf(
                        SubscriptionOption(
                            price = legacyPrice,
                            duration = legacyDuration,
                            durationInDays = when (legacyDuration) {
                                "1 mes" -> 30
                                "3 meses" -> 90
                                "anual" -> 365
                                else -> 30
                            },
                            displayName = "Plan Patrocinio ${legacyDuration.replaceFirstChar { it.uppercase() }}"
                        )
                    )
                }
            }

            val config = PatrocinioConfig(
                isEnabled = isEnabled,
                currency = currency,
                description = description,
                options = options
            )

            Log.d("FirestoreRepository", "🔍 [PATROCINIO_DEBUG] Config APK final: isEnabled=$isEnabled, options=${options.size}")
            return config

        } else {
            // Fallback para estructura legacy o sin patrocinioConfig
            Log.d("FirestoreRepository", "🔍 [PATROCINIO_DEBUG] Usando fallback a configuración por defecto")
            return PatrocinioConfig()
        }

    } catch (e: Exception) {
        Log.e("FirestoreRepository", "❌ [PATROCINIO_DEBUG] Error parseando configuración APK", e)
        return PatrocinioConfig()
    }
}
