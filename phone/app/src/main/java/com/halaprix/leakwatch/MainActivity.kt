package com.halaprix.leakwatch

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.halaprix.leakwatch.p2p.WearEngineReceiver
import com.halaprix.leakwatch.ui.theme.LeakWatchTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private lateinit var receiver: WearEngineReceiver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        receiver = WearEngineReceiver(this)
        
        setContent {
            LeakWatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onInsertMockData = { insertMockData() }
                    )
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
    
    private fun insertMockData() {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            receiver.insertMockBatch(50)
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onInsertMockData: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LeakWatch",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "v0.2.0-alpha.1",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Battery-frugal monitor for Huawei watches",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mock P2P receiver active — simulating watch data every 120s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onInsertMockData,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Insert 50 Mock Readings")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Check logcat for: 'Inserted mock reading' and 'Inserted X mock readings'",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
