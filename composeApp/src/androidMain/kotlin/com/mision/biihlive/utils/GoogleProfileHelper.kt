package com.mision.biihlive.utils

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object GoogleProfileHelper {
    private const val TAG = "GoogleProfileHelper"
    
    /**
     * Get Google account details from the device
     */
    fun getGoogleAccountInfo(context: Context, email: String): GoogleAccountDetails? {
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            
            // Find the specific account
            val account = accounts.firstOrNull { it.name == email }
            if (account != null) {
                // Extract name from email if no display name available
                val displayName = email.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                
                // Generate profile photo URL using Google's service
                val photoUrl = getGoogleProfilePhotoUrl(email)
                
                return GoogleAccountDetails(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting account info", e)
        }
        return null
    }
    
    /**
     * Generate Google profile photo URL
     * This uses Google's public profile photo service
     */
    private fun getGoogleProfilePhotoUrl(email: String): String {
        // Use Google's profile photo service
        // This returns a default avatar if no custom photo is set
        val emailHash = email.substringBefore("@")
        return "https://ui-avatars.com/api/?name=${emailHash}&background=FF7300&color=fff&size=200"
    }
    
    /**
     * Alternative: Try to get photo from Google People API (requires OAuth)
     * For now, we'll use a simpler approach
     */
    fun getGravatarUrl(email: String): String {
        val hash = java.security.MessageDigest.getInstance("MD5")
            .digest(email.lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "https://www.gravatar.com/avatar/$hash?s=200&d=identicon"
    }
    
    data class GoogleAccountDetails(
        val email: String,
        val displayName: String,
        val photoUrl: String
    )
    
    /**
     * Save account details to session
     */
    fun saveAccountDetailsToSession(
        context: Context,
        email: String
    ) {
        val accountInfo = getGoogleAccountInfo(context, email)
        if (accountInfo != null) {
            SessionManager.saveGoogleSession(
                context = context,
                email = accountInfo.email,
                name = accountInfo.displayName,
                photoUrl = accountInfo.photoUrl
            )
        } else {
            // Fallback: just save email
            SessionManager.saveGoogleSession(
                context = context,
                email = email,
                name = email.substringBefore("@"),
                photoUrl = getGravatarUrl(email)
            )
        }
    }
}