package com.foodcalories.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.foodcalories.app.presentation.navigation.FoodCaloriesNavGraph
import com.foodcalories.app.presentation.theme.FoodCaloriesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodCaloriesTheme {
                FoodCaloriesNavGraph()
            }
        }
    }
}
