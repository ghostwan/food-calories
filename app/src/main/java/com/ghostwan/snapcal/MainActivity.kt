package com.ghostwan.snapcal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ghostwan.snapcal.presentation.navigation.SnapCalNavGraph
import com.ghostwan.snapcal.presentation.theme.SnapCalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute

    private val _pendingScanId = MutableStateFlow<String?>(null)
    val pendingScanId: StateFlow<String?> = _pendingScanId

    fun consumePendingRoute() {
        _pendingRoute.value = null
    }

    fun consumePendingScanId() {
        _pendingScanId.value = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startRoute = intent?.getStringExtra("navigate_to") ?: "dashboard"
        val initialScanId = intent?.getStringExtra("scan_id")
        if (initialScanId != null) {
            _pendingScanId.value = initialScanId
        }
        setContent {
            SnapCalTheme {
                SnapCalNavGraph(startRoute = startRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val route = intent.getStringExtra("navigate_to")
        val scanId = intent.getStringExtra("scan_id")
        if (scanId != null) {
            // When a notification carries a scan id, navigation is driven by
            // pendingScanId (which both loads the cached result and routes to
            // the result screen). Skip pendingRoute to avoid a redundant nav
            // that would briefly show stale state.
            _pendingScanId.value = scanId
        } else if (route != null) {
            _pendingRoute.value = route
        }
    }
}
