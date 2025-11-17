package com.mision.biihlive.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateInfo(
    val currentVersion: Int,
    val latestVersion: Int,
    val latestVersionName: String,
    val hasUpdate: Boolean,
    val updateUrl: String = ""
)

data class UpdateCheckState(
    val isLoading: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val error: String? = null
)

class UpdateCheckViewModel : ViewModel() {
    private val _state = MutableStateFlow(UpdateCheckState())
    val state: StateFlow<UpdateCheckState> = _state.asStateFlow()
    
    fun checkForUpdates(currentVersionCode: Int = 1) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // Simular llamada a servidor remoto
                val latestVersion = fetchLatestVersion()
                
                val updateInfo = UpdateInfo(
                    currentVersion = currentVersionCode,
                    latestVersion = latestVersion.versionCode,
                    latestVersionName = latestVersion.versionName,
                    hasUpdate = latestVersion.versionCode > currentVersionCode,
                    updateUrl = "https://play.google.com/store/apps/details?id=com.mision.biihlive"
                )
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    updateInfo = updateInfo,
                    showUpdateDialog = updateInfo.hasUpdate
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error verificando actualizaciones: ${e.message}"
                )
            }
        }
    }
    
    fun dismissUpdateDialog() {
        _state.value = _state.value.copy(showUpdateDialog = false)
    }
    
    fun showUpdateDialog() {
        _state.value = _state.value.copy(showUpdateDialog = true)
    }
    
    // Simula obtener la versión más reciente desde un servidor
    private suspend fun fetchLatestVersion(): VersionResponse {
        // Aquí harías la llamada real a tu servidor/API
        // Por ahora simulo que la versión más reciente es la 3
        return VersionResponse(
            versionCode = 3,
            versionName = "1.2"
        )
    }
}

data class VersionResponse(
    val versionCode: Int,
    val versionName: String
)