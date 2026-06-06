package com.example.threadslite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.threadslite.ui.ThreadsLiteApp
import com.example.threadslite.ui.theme.ThreadsLiteTheme

/**
 * Single-activity entry point for the app.
 * Compose navigation handles all screen transitions inside [ThreadsLiteApp].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThreadsLiteTheme {
                ThreadsLiteApp()
            }
        }
    }
}
