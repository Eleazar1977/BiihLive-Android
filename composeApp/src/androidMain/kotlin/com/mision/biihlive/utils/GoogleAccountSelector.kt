package com.mision.biihlive.utils

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import com.google.android.gms.common.AccountPicker
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.mision.biihlive.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object GoogleAccountSelector {
    private const val TAG = "GoogleAccountSelector"
    
    fun getDeviceGoogleAccounts(context: Context): List<String> {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType("com.google")
        return accounts.map { it.name }
    }
    
    fun hasGoogleAccounts(context: Context): Boolean {
        return getDeviceGoogleAccounts(context).isNotEmpty()
    }
    
    fun showNativeAccountPicker(
        launcher: ActivityResultLauncher<Intent>,
        showAddAccount: Boolean = false
    ) {
        val intent = AccountPicker.newChooseAccountIntent(
            null, // selectedAccount
            null, // allowableAccounts  
            arrayOf("com.google"), // allowableAccountTypes
            false, // alwaysPromptForAccount
            null, // descriptionOverrideText
            if (showAddAccount) "com.google" else null, // addAccountAuthTokenType
            null, // addAccountRequiredFeatures
            null  // optionsBundle
        )
        
        Log.d(TAG, "Launching native account picker")
        launcher.launch(intent)
    }
    
    data class GoogleAccountInfo(
        val email: String,
        val displayName: String?,
        val photoUrl: String?,
        val idToken: String // ← ID token para Firebase Auth
    )
    
    suspend fun tryModernGoogleSignIn(
        context: Context,
        onSuccess: (GoogleAccountInfo) -> Unit,
        onFailure: (String) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                val credentialManager = CredentialManager.create(context)
                
                // Get Web Client ID from resources
                val webClientId = context.getString(R.string.default_web_client_id)
                
                // Configure Google ID option with server client ID
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // Show all accounts
                    .setServerClientId(webClientId) // Required for getting profile info
                    .setAutoSelectEnabled(false) // Force show picker
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                Log.d(TAG, "Attempting modern Google sign-in with profile info")
                
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                
                when (val credential = result.credential) {
                    is CustomCredential -> {
                        if (credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try {
                                val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                                
                                // Extract profile information
                                val accountInfo = GoogleAccountInfo(
                                    email = googleIdTokenCredential.id,
                                    displayName = googleIdTokenCredential.displayName,
                                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                                    idToken = googleIdTokenCredential.idToken // ← ID token para Firebase Auth
                                )
                                
                                Log.d(TAG, "Successfully got Google account with profile: ${accountInfo.email}, ${accountInfo.displayName}")
                                onSuccess(accountInfo)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse Google ID token", e)
                                onFailure("Error al procesar la cuenta")
                            }
                        } else {
                            Log.w(TAG, "Unknown credential type: ${credential.type}")
                            onFailure("Tipo de credencial desconocido")
                        }
                    }
                    else -> {
                        Log.w(TAG, "Unexpected credential type")
                        onFailure("Credencial inesperada")
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "User cancelled the dialog")
                onFailure("Selección cancelada")
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No credentials available, falling back to AccountPicker", e)
                onFailure("Use AccountPicker") // Special signal to use fallback
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during sign-in", e)
                onFailure("Error: ${e.message}")
            }
        }
    }
    
    fun clearAllGoogleSessions(context: Context) {
        try {
            // Clear our SessionManager
            SessionManager.clearSession(context)
            
            // Clear any cached credentials
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    credentialManager.clearCredentialState(
                        ClearCredentialStateRequest()
                    )
                    Log.d(TAG, "Cleared credential manager state")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing credential state", e)
                }
            }
            
            Log.d(TAG, "Cleared all Google sessions")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing sessions", e)
        }
    }
}