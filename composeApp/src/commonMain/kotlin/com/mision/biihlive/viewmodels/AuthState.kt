package com.mision.biihlive.viewmodels

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object SignedOut : AuthState()
    object SignedIn : AuthState()
    data class SignUpRequiresConfirmation(val username: String) : AuthState()
    data class PasswordResetSent(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}