package com.vyrena.mconbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BridgeTheme { BridgeHomePlaceholder() } }
    }
}

@Composable
private fun BridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF8B70FF),
            background = Color(0xFF0B0D12),
            surface = Color(0xFF121722),
            onBackground = Color(0xFFF1EFFF),
            onSurface = Color(0xFFF1EFFF),
        ),
        content = content,
    )
}

@Composable
private fun BridgeHomePlaceholder() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("MCON Bridge", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("Azahar · Artemis · Kirin", color = MaterialTheme.colorScheme.primary)
                Text("Initial project scaffold", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
