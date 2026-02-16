package com.ghostwan.snapcal.data.remote

sealed class GeminiAuth {
    data class ApiKey(val key: String) : GeminiAuth()
    data class OAuth(val accessToken: String) : GeminiAuth()
}
