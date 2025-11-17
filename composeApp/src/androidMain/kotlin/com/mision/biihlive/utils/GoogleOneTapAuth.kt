package com.mision.biihlive.utils

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.mision.biihlive.R
import kotlinx.coroutines.tasks.await

class GoogleOneTapAuth(private val activity: ComponentActivity) {
    
    private val oneTapClient: SignInClient = Identity.getSignInClient(activity)
    
    companion object {
        private const val TAG = "GoogleOneTap"
    }
    
    data class AccountInfo(
        val email: String,
        val displayName: String?,
        val givenName: String?,
        val familyName: String?,
        val profilePictureUri: String?,
        val idToken: String?
    )
    
    fun handleSignInResult(
        data: android.content.Intent?,
        resultCode: Int
    ): AccountInfo? {
        Log.d(TAG, "handleSignInResult called with resultCode: $resultCode, data: ${data != null}")
        
        // Handle both RESULT_OK and RESULT_CANCELED (sometimes Google returns 0 even on success)
        if (data != null) {
            try {
                Log.d(TAG, "Attempting to get credential from intent...")
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                
                Log.d(TAG, "Credential retrieved:")
                Log.d(TAG, "  - ID: ${credential.id}")
                Log.d(TAG, "  - Display Name: ${credential.displayName}")
                Log.d(TAG, "  - Given Name: ${credential.givenName}")
                Log.d(TAG, "  - Family Name: ${credential.familyName}")
                Log.d(TAG, "  - Profile URI: ${credential.profilePictureUri}")
                Log.d(TAG, "  - Has ID Token: ${credential.googleIdToken != null}")
                
                // Build display name if not provided
                val displayName = when {
                    !credential.displayName.isNullOrBlank() -> credential.displayName
                    credential.givenName != null && credential.familyName != null -> 
                        "${credential.givenName} ${credential.familyName}"
                    credential.givenName != null -> credential.givenName
                    else -> credential.id.substringBefore('@')
                }
                
                // Try to build profile picture URL if not provided
                val profilePictureUrl = credential.profilePictureUri?.toString() 
                    ?: if (credential.id.contains("@gmail.com")) {
                        // Fallback: Try to get Google profile picture using email
                        "https://lh3.googleusercontent.com/a/default-user"
                    } else null
                
                val accountInfo = AccountInfo(
                    email = credential.id,
                    displayName = displayName,
                    givenName = credential.givenName,
                    familyName = credential.familyName,
                    profilePictureUri = profilePictureUrl,
                    idToken = credential.googleIdToken
                )
                Log.d(TAG, "✅ One Tap successful: ${accountInfo.email}, ${accountInfo.displayName}")
                return accountInfo
            } catch (e: ApiException) {
                Log.e(TAG, "One Tap credential retrieval failed with code: ${e.statusCode}", e)
                Log.e(TAG, "Error message: ${e.message}")
                
                // Provide specific guidance based on error code
                when (e.statusCode) {
                    CommonStatusCodes.DEVELOPER_ERROR -> {
                        Log.e(TAG, "❌ DEVELOPER ERROR (10): Google Cloud Console configuration issue!")
                        Log.e(TAG, "Fix required:")
                        Log.e(TAG, "1. Enable Google+ API in Google Cloud Console")
                        Log.e(TAG, "2. Create BOTH Android AND Web OAuth 2.0 clients")
                        Log.e(TAG, "3. Ensure Web Client ID is used in strings.xml")
                        Log.e(TAG, "4. Check SHA-1: 86:CC:33:0E:46:7A:94:69:E8:7E:34:92:36:05:1B:73:23:C7:8B:27")
                        Log.e(TAG, "See: docs/google-one-tap-error-10-fix.md for full solution")
                    }
                    CommonStatusCodes.CANCELED -> {
                        if (resultCode == Activity.RESULT_CANCELED) {
                            Log.d(TAG, "User explicitly cancelled One Tap")
                        }
                    }
                    CommonStatusCodes.NETWORK_ERROR -> {
                        Log.e(TAG, "Network error - check internet connection")
                    }
                    else -> {
                        Log.e(TAG, "Unknown error code: ${e.statusCode}")
                    }
                }
                return null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error handling sign-in result", e)
                return null
            }
        } else {
            Log.d(TAG, "No data received from One Tap (resultCode: $resultCode)")
            return null
        }
    }
    
    suspend fun beginSignIn(
        onLaunchIntent: (IntentSenderRequest) -> Unit,
        onNoAccounts: () -> Unit
    ) {
        try {
            val webClientId = activity.getString(R.string.default_web_client_id)
            
            // Configure One Tap request with both token and password options
            val signInRequest = BeginSignInRequest.builder()
                .setPasswordRequestOptions(
                    BeginSignInRequest.PasswordRequestOptions.builder()
                        .setSupported(true)
                        .build()
                )
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(false) // Show ALL accounts
                        .setRequestVerifiedPhoneNumber(false) // Don't require phone
                        .build()
                )
                .setAutoSelectEnabled(false) // Force show picker even with one account
                .build()
            
            Log.d(TAG, "Attempting to show One Tap UI with SignInRequest...")
            
            val result = oneTapClient.beginSignIn(signInRequest).await()
            
            // Return the intent to be launched
            val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
            onLaunchIntent(intentSenderRequest)
            
            Log.d(TAG, "One Tap UI ready to launch with SignInRequest")
            
        } catch (e: Exception) {
            when (e) {
                is ApiException -> {
                    Log.d(TAG, "SignInRequest failed with status: ${e.statusCode}, trying SignUpRequest...")
                    
                    // Try with SignUpRequest as fallback (often shows better UI with photos)
                    val clientId = activity.getString(R.string.default_web_client_id)
                    trySignUpRequest(clientId, onLaunchIntent, onNoAccounts)
                }
                else -> {
                    Log.e(TAG, "Unexpected One Tap error", e)
                    onNoAccounts()
                }
            }
        }
    }
    
    private suspend fun trySignUpRequest(
        webClientId: String,
        onLaunchIntent: (IntentSenderRequest) -> Unit,
        onNoAccounts: () -> Unit
    ) {
        try {
            // Try SignUpRequest which often shows the full UI with photos
            val signUpRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .setRequestVerifiedPhoneNumber(false)
                        .build()
                )
                .build()
            
            Log.d(TAG, "Attempting with SignUpRequest...")
            
            val result = oneTapClient.beginSignIn(signUpRequest).await()
            
            val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
            onLaunchIntent(intentSenderRequest)
            
            Log.d(TAG, "One Tap UI ready to launch with SignUpRequest")
            
        } catch (e: Exception) {
            Log.e(TAG, "Both SignInRequest and SignUpRequest failed", e)
            onNoAccounts()
        }
    }
    
    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            Log.d(TAG, "Signed out from One Tap")
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
        }
    }
    
    /**
     * Diagnostic method to check configuration
     */
    fun diagnoseConfiguration() {
        try {
            val webClientId = activity.getString(R.string.default_web_client_id)
            Log.d(TAG, "====== Google One Tap Configuration Diagnosis ======")
            Log.d(TAG, "Package Name: ${activity.packageName}")
            Log.d(TAG, "Web Client ID: $webClientId")
            Log.d(TAG, "Web Client ID valid: ${webClientId.endsWith(".apps.googleusercontent.com")}")
            
            // Get current SHA-1 for reference
            Log.d(TAG, "Expected SHA-1 (Debug): 86:CC:33:0E:46:7A:94:69:E8:7E:34:92:36:05:1B:73:23:C7:8B:27")
            
            // Check Google Play Services
            val gms = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val gmsStatus = gms.isGooglePlayServicesAvailable(activity)
            Log.d(TAG, "Google Play Services: ${if (gmsStatus == com.google.android.gms.common.ConnectionResult.SUCCESS) "✅ Available" else "❌ Not Available (code: $gmsStatus)"}")
            
            Log.d(TAG, "====== End Diagnosis ======")
            Log.d(TAG, "If you see error 10, check:")
            Log.d(TAG, "1. Google+ API is enabled in Google Cloud Console")
            Log.d(TAG, "2. Web OAuth 2.0 client exists (not just Android)")
            Log.d(TAG, "3. The Web Client ID above matches your Google Cloud Console")
        } catch (e: Exception) {
            Log.e(TAG, "Error during diagnosis", e)
        }
    }
}