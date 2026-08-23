package com.vyrena.mconbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.vyrena.mconbridge.domain.BridgeLink
import com.vyrena.mconbridge.domain.LaunchResult
import com.vyrena.mconbridge.ui.BridgeApp
import com.vyrena.mconbridge.ui.BridgeEvent
import com.vyrena.mconbridge.ui.BridgeTheme
import com.vyrena.mconbridge.ui.BridgeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: BridgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BridgeTheme { BridgeApp(viewModel) } }
        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is BridgeEvent.Message -> Toast.makeText(this@MainActivity, event.text, Toast.LENGTH_LONG).show()
                    is BridgeEvent.StartIntent -> runCatching { startActivity(event.intent) }
                        .onFailure { Toast.makeText(this@MainActivity, it.message ?: "No compatible app found", Toast.LENGTH_LONG).show() }
                }
            }
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val id = BridgeLink.parse(intent.data)
        if (id == null) {
            Toast.makeText(this, "Invalid MCON Bridge link", Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            when (val result = (application as MconBridgeApplication).container.launcher.launch(this@MainActivity, id)) {
                LaunchResult.Started -> finish()
                is LaunchResult.Error -> Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
