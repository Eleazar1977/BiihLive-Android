package com.mision.biihlive.presentation.chat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.core.managers.UserIdManager
import com.mision.biihlive.data.chat.repository.ChatFirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel global para manejar el estado de mensajes no leídos en toda la app
 * Usado por NavigationBar y otras pantallas que necesiten el conteo total
 */
class GlobalChatViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "GlobalChatViewModel"
    }

    private val chatRepository = ChatFirestoreRepository(context)

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    init {
        observeUnreadMessages()
        updateUserPresenceOnline()
    }

    private fun observeUnreadMessages() {
        viewModelScope.launch {
            try {
                val currentUserId = UserIdManager.getInstance(context).getCurrentUserId()
                Log.d(TAG, "🔔 Iniciando observación global de mensajes no leídos para usuario: $currentUserId")

                chatRepository.observeUnreadCount(currentUserId)
                    .catch { error ->
                        Log.e(TAG, "❌ Error observando mensajes no leídos: ${error.message}")
                        _unreadCount.value = 0
                    }
                    .collect { count ->
                        Log.d(TAG, "📊 Total mensajes no leídos: $count")
                        _unreadCount.value = count
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inicializando observación de mensajes no leídos: ${e.message}")
                _unreadCount.value = 0
            }
        }
    }

    /**
     * Refresca manualmente el conteo de mensajes no leídos
     * Útil después de marcar mensajes como leídos
     */
    fun refreshUnreadCount() {
        viewModelScope.launch {
            try {
                val currentUserId = UserIdManager.getInstance(context).getCurrentUserId()
                Log.d(TAG, "🔄 Refrescando conteo de mensajes no leídos para usuario: $currentUserId")

                // Forzar nueva observación
                observeUnreadMessages()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error refrescando conteo de mensajes no leídos: ${e.message}")
            }
        }
    }

    /**
     * Actualiza el estado de presencia del usuario a online
     * Se llama cuando la app se abre
     */
    private fun updateUserPresenceOnline() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🟢 Marcando usuario como en línea...")
                val result = chatRepository.updateUserPresence(isOnline = true)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Usuario marcado como en línea exitosamente")
                } else {
                    Log.e(TAG, "❌ Error marcando usuario como en línea: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción marcando usuario como en línea: ${e.message}")
            }
        }
    }

    /**
     * Actualiza el estado de presencia del usuario a offline
     * Se llama cuando la app se cierra
     */
    private fun updateUserPresenceOffline() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔴 Marcando usuario como fuera de línea...")
                val result = chatRepository.updateUserPresence(isOnline = false)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Usuario marcado como fuera de línea exitosamente")
                } else {
                    Log.e(TAG, "❌ Error marcando usuario como fuera de línea: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción marcando usuario como fuera de línea: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateUserPresenceOffline()
        Log.d(TAG, "🧹 GlobalChatViewModel destruido")
    }
}