package com.ghostwan.snapcal.data.remote

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.ghostwan.snapcal.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

class GoogleAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthManager"
        private const val GEMINI_SCOPE = "https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/generative-language.retriever"
        private const val REDIRECT_URI = "http://localhost"
        private const val GEMINI_PREFS = "gemini_oauth_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }

    // --- Google Sign-In for Drive ---

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

    suspend fun signOut() {
        signInClient.signOut()
        clearGeminiTokens()
    }

    // --- Gemini OAuth via WebView ---

    private val geminiPrefs by lazy {
        context.getSharedPreferences(GEMINI_PREFS, Context.MODE_PRIVATE)
    }

    private var pendingVerifier: String? = null

    fun buildGeminiAuthUrl(): String {
        val codeVerifier = generateCodeVerifier()
        pendingVerifier = codeVerifier
        val codeChallenge = generateCodeChallenge(codeVerifier)

        return buildString {
            append("https://accounts.google.com/o/oauth2/v2/auth?")
            append("client_id=").append(enc(BuildConfig.OAUTH_CLIENT_ID))
            append("&redirect_uri=").append(enc(REDIRECT_URI))
            append("&response_type=code")
            append("&scope=").append(enc(GEMINI_SCOPE))
            append("&code_challenge=").append(codeChallenge)
            append("&code_challenge_method=S256")
            append("&access_type=offline")
            append("&prompt=consent")
        }
    }

    fun isOAuthRedirect(url: String): Boolean {
        return url.startsWith(REDIRECT_URI)
    }

    suspend fun exchangeGeminiCode(code: String): Boolean {
        val verifier = pendingVerifier ?: throw Exception("No code verifier")

        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Exchanging auth code for tokens...")
            val url = URL("https://oauth2.googleapis.com/token")
            val body = buildString {
                append("code=").append(enc(code))
                append("&client_id=").append(enc(BuildConfig.OAUTH_CLIENT_ID))
                append("&client_secret=").append(enc(BuildConfig.OAUTH_CLIENT_SECRET))
                append("&redirect_uri=").append(enc(REDIRECT_URI))
                append("&grant_type=authorization_code")
                append("&code_verifier=").append(enc(verifier))
            }

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.outputStream.use { it.write(body.toByteArray()) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                Log.d(TAG, "Token exchange successful, expires_in=${json.optLong("expires_in")}")
                geminiPrefs.edit()
                    .putString(KEY_ACCESS_TOKEN, json.getString("access_token"))
                    .putString(KEY_REFRESH_TOKEN, json.optString("refresh_token", ""))
                    .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + json.getLong("expires_in") * 1000)
                    .apply()
                pendingVerifier = null
                true
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "Token exchange failed ($responseCode): $errorBody")
                throw Exception("Token exchange error $responseCode")
            }
        }
    }

    suspend fun getGeminiAccessToken(): String? {
        val token = geminiPrefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = geminiPrefs.getLong(KEY_EXPIRES_AT, 0)

        if (System.currentTimeMillis() < expiresAt - 60_000) return token

        return refreshGeminiToken()
    }

    private suspend fun refreshGeminiToken(): String? {
        val refreshToken = geminiPrefs.getString(KEY_REFRESH_TOKEN, null)
        if (refreshToken.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://oauth2.googleapis.com/token")
                val body = buildString {
                    append("refresh_token=").append(enc(refreshToken))
                    append("&client_id=").append(enc(BuildConfig.OAUTH_CLIENT_ID))
                    append("&client_secret=").append(enc(BuildConfig.OAUTH_CLIENT_SECRET))
                    append("&grant_type=refresh_token")
                }

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.outputStream.use { it.write(body.toByteArray()) }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val newToken = json.getString("access_token")
                    geminiPrefs.edit()
                        .putString(KEY_ACCESS_TOKEN, newToken)
                        .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + json.getLong("expires_in") * 1000)
                        .apply()
                    newToken
                } else {
                    Log.e(TAG, "Token refresh failed (${conn.responseCode})")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh error", e)
                null
            }
        }
    }

    fun hasGeminiTokens(): Boolean {
        return geminiPrefs.getString(KEY_ACCESS_TOKEN, null) != null
    }

    fun clearGeminiTokens() {
        geminiPrefs.edit().clear().apply()
        pendingVerifier = null
    }

    // --- PKCE helpers ---

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
