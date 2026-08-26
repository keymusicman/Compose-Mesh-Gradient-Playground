package com.keymusicman.meshgradientplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.keymusicman.meshgradientplayground.ui.theme.MeshGradientPlaygroundTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeshGradientPlaygroundTheme {
                MeshGradientPlayground()
            }
        }
    }
}
