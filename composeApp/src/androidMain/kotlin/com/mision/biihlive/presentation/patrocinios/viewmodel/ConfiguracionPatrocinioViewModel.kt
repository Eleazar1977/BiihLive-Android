package com.mision.biihlive.presentation.patrocinios.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.domain.perfil.model.PatrocinioConfig
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfiguracionPatrocinioUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val config: PatrocinioConfig = PatrocinioConfig(),
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
    val saveSuccess: Boolean = false
)

class ConfiguracionPatrocinioViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "ConfigPatrocinioVM"
    }

    private val _uiState = MutableStateFlow(ConfiguracionPatrocinioUiState())
    val uiState: StateFlow<ConfiguracionPatrocinioUiState> = _uiState.asStateFlow()

    // Mantener referencia al config original cargado desde Firestore
    private var originalConfig: PatrocinioConfig = PatrocinioConfig()

    init {
        loadUserPatrocinioConfig()
    }

    private fun loadUserPatrocinioConfig() {
        viewModelScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId.isNullOrBlank()) {
                Log.e(TAG, "💰 [PATROCINIO_CONFIG] ❌ Usuario no autenticado")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Usuario no autenticado"
                )
                return@launch
            }
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d(TAG, "💰 [PATROCINIO_CONFIG] 🔄 Cargando configuración para usuario: $currentUserId")

                val result = firestoreRepository.getPerfilUsuario(currentUserId)

                result.fold(
                    onSuccess = { perfil ->
                        if (perfil != null) {
                            Log.d(TAG, "💰 [PATROCINIO_CONFIG] ✅ Configuración cargada: ${perfil.patrocinioConfig}")

                            // Guardar config original y actualizar estado
                            originalConfig = perfil.patrocinioConfig
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                config = perfil.patrocinioConfig,
                                hasChanges = false, // Reset hasChanges ya que acabamos de cargar
                                error = null
                            )
                        } else {
                            Log.w(TAG, "💰 [PATROCINIO_CONFIG] ⚠️ Perfil no encontrado")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Perfil no encontrado"
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "💰 [PATROCINIO_CONFIG] ❌ Error cargando configuración", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error cargando configuración: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "💰 [PATROCINIO_CONFIG] ❌ Error inesperado", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }

    fun updatePrice(newPrice: String) {
        val currentConfig = _uiState.value.config
        val currentOption = currentConfig.options.firstOrNull() ?: return
        val updatedOption = currentOption.copy(price = newPrice)
        val updatedOptions = listOf(updatedOption) + currentConfig.options.drop(1)
        val newConfig = currentConfig.copy(options = updatedOptions)

        _uiState.value = _uiState.value.copy(
            config = newConfig,
            hasChanges = newConfig != originalConfig,
            saveSuccess = false
        )

        Log.d(TAG, "💰 [PATROCINIO_CONFIG] 💱 Precio actualizado: $newPrice")
    }

    fun updateDuration(newDuration: String) {
        val currentConfig = _uiState.value.config
        val currentOption = currentConfig.options.firstOrNull() ?: return
        val durationInDays = when (newDuration) {
            "1 mes" -> 30
            "3 meses" -> 90
            "anual" -> 365
            else -> 30
        }
        val updatedOption = currentOption.copy(
            duration = newDuration,
            durationInDays = durationInDays,
            displayName = "Plan Patrocinio ${newDuration.replaceFirstChar { it.uppercase() }}"
        )
        val updatedOptions = listOf(updatedOption) + currentConfig.options.drop(1)
        val newConfig = currentConfig.copy(options = updatedOptions)

        _uiState.value = _uiState.value.copy(
            config = newConfig,
            hasChanges = newConfig != originalConfig,
            saveSuccess = false
        )

        Log.d(TAG, "💰 [PATROCINIO_CONFIG] ⏰ Duración actualizada: $newDuration")
    }

    fun updateCurrency(newCurrency: String) {
        val currentConfig = _uiState.value.config
        val newConfig = currentConfig.copy(currency = newCurrency)

        _uiState.value = _uiState.value.copy(
            config = newConfig,
            hasChanges = newConfig != originalConfig,
            saveSuccess = false
        )

        Log.d(TAG, "💰 [PATROCINIO_CONFIG] 💰 Moneda actualizada: $newCurrency")
    }

    fun updateIsEnabled(newIsEnabled: Boolean) {
        val currentConfig = _uiState.value.config
        val newConfig = currentConfig.copy(isEnabled = newIsEnabled)

        _uiState.value = _uiState.value.copy(
            config = newConfig,
            hasChanges = newConfig != originalConfig,
            saveSuccess = false
        )

        Log.d(TAG, "💰 [PATROCINIO_CONFIG] 🔄 Estado habilitado actualizado: $newIsEnabled")
    }

    fun updateDescription(newDescription: String) {
        val currentConfig = _uiState.value.config
        val newConfig = currentConfig.copy(description = newDescription)

        _uiState.value = _uiState.value.copy(
            config = newConfig,
            hasChanges = newConfig != originalConfig,
            saveSuccess = false
        )

        Log.d(TAG, "💰 [PATROCINIO_CONFIG] 📝 Descripción actualizada: $newDescription")
    }

    fun saveConfiguration() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            Log.e(TAG, "💰 [PATROCINIO_CONFIG] ❌ Usuario no autenticado para guardar")
            return
        }

        val currentConfig = _uiState.value.config

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)

                Log.d(TAG, "💰 [PATROCINIO_CONFIG] 💾 Guardando configuración: $currentConfig")

                val firstOption = currentConfig.options.firstOrNull()
                val result = firestoreRepository.updateUserPatrocinioConfig(
                    userId = currentUserId,
                    price = firstOption?.price ?: "19.99",
                    duration = firstOption?.duration ?: "1 mes",
                    isEnabled = currentConfig.isEnabled,
                    currency = currentConfig.currency,
                    description = currentConfig.description
                )

                result.fold(
                    onSuccess = { updatedProfile ->
                        Log.d(TAG, "💰 [PATROCINIO_CONFIG] ✅ Configuración guardada exitosamente")

                        // Actualizar originalConfig con la configuración guardada
                        originalConfig = currentConfig

                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            hasChanges = false,
                            saveSuccess = true,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "💰 [PATROCINIO_CONFIG] ❌ Error guardando configuración", exception)
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = "Error guardando: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "💰 [PATROCINIO_CONFIG] ❌ Error inesperado guardando", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetChanges() {
        // Resetear el estado actual al originalConfig
        _uiState.value = _uiState.value.copy(
            config = originalConfig,
            hasChanges = false,
            saveSuccess = false,
            error = null
        )
        Log.d(TAG, "💰 [PATROCINIO_CONFIG] 🔄 Cambios reseteados al estado original")
    }
}