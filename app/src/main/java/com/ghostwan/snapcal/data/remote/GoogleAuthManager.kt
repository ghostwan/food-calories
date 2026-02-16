package com.ghostwan.snapcal.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthManager(private val context: Context) {

    companion object {
        private const val GEMINI_SCOPE = "https://www.googleapis.com/auth/generative-language"
    }

    private val signInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = signInClient.signInIntent

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun isSignedIn(): Boolean = getSignedInAccount() != null

    fun getSignedInEmail(): String? = getSignedInAccount()?.email

    fun handleSignInResult(data: Intent?): GoogleSignInAccount {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return task.getResult(Exception::class.java)
    }

    /**
     * Get a Gemini access token. Throws [UserRecoverableAuthException] if consent is needed.
     * Returns null only if no account is signed in.
     */
    @Throws(UserRecoverableAuthException::class)
    suspend fun getGeminiAccessToken(): String? {
        val account = getSignedInAccount() ?: return null
        return withContext(Dispatchers.IO) {
            GoogleAuthUtil.getToken(context, account.account!!, "oauth2:$GEMINI_SCOPE")
        }
    }

    suspend fun signOut() {
        signInClient.signOut()
    }
}
