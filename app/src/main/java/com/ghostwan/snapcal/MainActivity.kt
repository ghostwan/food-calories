package com.ghostwan.snapcal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ghostwan.snapcal.presentation.navigation.SnapCalNavGraph
import com.ghostwan.snapcal.presentation.theme.SnapCalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startRoute = intent?.getStringExtra("navigate_to") ?: "dashboard"
        setContent {
            SnapCalTheme {
                SnapCalNavGraph(startRoute = startRoute)
            }
        }
    }
}
