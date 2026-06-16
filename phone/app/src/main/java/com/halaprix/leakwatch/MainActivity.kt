package com.halaprix.leakwatch

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.halaprix.leakwatch.p2p.WearEngineReceiver
import com.halaprix.leakwatch.ui.LeakWatchScreen
import com.halaprix.leakwatch.ui.LeakWatchViewModel
import com.halaprix.leakwatch.ui.theme.LeakWatchTheme

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private lateinit var receiver: WearEngineReceiver
    private val viewModel: LeakWatchViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        receiver = WearEngineReceiver(this)
        
        setContent {
            LeakWatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LeakWatchScreen(viewModel = viewModel)
                }
            }
        }
        
        // Start mock P2P receiver
        receiver.startReceiving()
        Log.i(TAG, "Mock P2P receiver started")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        receiver.stopReceiving()
        Log.i(TAG, "Mock P2P receiver stopped")
    }
}
