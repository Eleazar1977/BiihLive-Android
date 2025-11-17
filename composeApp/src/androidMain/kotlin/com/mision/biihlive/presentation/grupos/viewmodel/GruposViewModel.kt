package com.mision.biihlive.presentation.grupos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GruposUiState(
    val grupos: List<Any> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCurrentUser: Boolean = true,
    val userId: String? = null
)

class GruposViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GruposUiState())
    val uiState: StateFlow<GruposUiState> = _uiState.asStateFlow()

    fun loadGrupos(userId: String, currentUserId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userId = userId,
                isCurrentUser = currentUserId == null || userId == currentUserId,
                grupos = emptyList()
            )
        }
    }

    fun refresh() {
        // Placeholder para refresh
    }
}