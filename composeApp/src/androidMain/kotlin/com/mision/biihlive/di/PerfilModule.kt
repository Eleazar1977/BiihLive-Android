package com.mision.biihlive.di

import android.app.Application
import android.content.Context
import com.mision.biihlive.data.repository.FirestoreRepository
import android.util.Log
import com.mision.biihlive.domain.perfil.repository.PerfilRepository
import com.mision.biihlive.domain.perfil.usecase.ActualizarPerfilUseCase
import com.mision.biihlive.domain.perfil.usecase.ObtenerPerfilUseCase
import com.mision.biihlive.presentation.perfil.PerfilPersonalLogueadoViewModel
import com.mision.biihlive.presentation.perfil.PerfilPublicoConsultadoViewModel

object PerfilModule {
    
    private var perfilRepository: PerfilRepository? = null

    fun providePerfilRepository(context: Context? = null): PerfilRepository {
        if (perfilRepository == null) {
            Log.d("PerfilModule", "Inicializando PerfilRepository con Firestore")
            perfilRepository = FirestoreRepositoryAdapter()
        }
        return perfilRepository!!
    }

    // Adapter para usar FirestoreRepository como PerfilRepository
    private class FirestoreRepositoryAdapter : PerfilRepository {
        private val firestoreRepository = FirestoreRepository()

        override suspend fun obtenerPerfil(userId: String): Result<com.mision.biihlive.domain.perfil.model.PerfilUsuario> {
            return firestoreRepository.getPerfilUsuario(userId).map { perfil ->
                perfil ?: throw Exception("Perfil no encontrado")
            }
        }

        override suspend fun actualizarPerfil(perfil: com.mision.biihlive.domain.perfil.model.PerfilUsuario): Result<com.mision.biihlive.domain.perfil.model.PerfilUsuario> {
            return firestoreRepository.updateProfile(
                userId = perfil.userId,
                nickname = perfil.nickname,
                description = perfil.description,
                photoUrl = perfil.photoUrl,
                fullName = perfil.fullName
            ).map { perfilActualizado ->
                perfilActualizado ?: throw Exception("Error actualizando perfil")
            }
        }

        override suspend fun actualizarDescripcion(userId: String, descripcion: String): Result<Unit> {
            return firestoreRepository.updateProfile(userId = userId, description = descripcion)
                .map { Unit }
        }

        override suspend fun actualizarNickname(userId: String, nickname: String): Result<Unit> {
            return firestoreRepository.updateProfile(userId = userId, nickname = nickname)
                .map { Unit }
        }

        override suspend fun actualizarFotoPerfil(userId: String, photoUrl: String): Result<Unit> {
            return firestoreRepository.updateProfile(userId = userId, photoUrl = photoUrl)
                .map { Unit }
        }

        override suspend fun incrementarSeguidores(userId: String): Result<Unit> {
            // Implementación simple - en producción sería una operación atómica
            return Result.success(Unit)
        }

        override suspend fun decrementarSeguidores(userId: String): Result<Unit> {
            // Implementación simple - en producción sería una operación atómica
            return Result.success(Unit)
        }

        override suspend fun obtenerRanking(userId: String): Result<Map<String, Int>> {
            // Implementación simple - retornar datos del perfil
            return obtenerPerfil(userId).map { perfil ->
                mapOf(
                    "position" to 1,
                    "score" to perfil.totalScore
                )
            }
        }

        override fun observarPerfil(userId: String): kotlinx.coroutines.flow.Flow<com.mision.biihlive.domain.perfil.model.PerfilUsuario?> {
            return kotlinx.coroutines.flow.flow {
                try {
                    val result = obtenerPerfil(userId)
                    emit(result.getOrNull())
                } catch (e: Exception) {
                    emit(null)
                }
            }
        }
    }
    
    fun provideObtenerPerfilUseCase(context: Context? = null): ObtenerPerfilUseCase {
        return ObtenerPerfilUseCase(providePerfilRepository(context))
    }
    
    fun provideActualizarPerfilUseCase(context: Context? = null): ActualizarPerfilUseCase {
        return ActualizarPerfilUseCase(providePerfilRepository(context))
    }
    
    fun providePerfilPersonalLogueadoViewModel(application: Application): PerfilPersonalLogueadoViewModel {
        return PerfilPersonalLogueadoViewModel(
            application = application,
            obtenerPerfilUseCase = provideObtenerPerfilUseCase(application.applicationContext),
            actualizarPerfilUseCase = provideActualizarPerfilUseCase(application.applicationContext)
        )
    }

    fun providePerfilPublicoConsultadoViewModel(application: Application): PerfilPublicoConsultadoViewModel {
        return PerfilPublicoConsultadoViewModel(
            application = application,
            obtenerPerfilUseCase = provideObtenerPerfilUseCase(application.applicationContext)
        )
    }
}