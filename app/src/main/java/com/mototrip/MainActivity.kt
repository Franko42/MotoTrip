package com.mototrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mototrip.navigation.MotoTripNavGraph
import com.mototrip.ui.theme.MotoTripTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotoTripTheme {
                MotoTripNavGraph()
            }
        }
    }
}
