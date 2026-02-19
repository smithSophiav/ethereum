package com.smithSophiav.ethereum.gas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smithSophiav.ethereum.network.NetworkManager
import com.smithSophiav.ethereum.ui.theme.EthereumTheme
import com.smithsophiav.etherweb.EtherWeb
import kotlinx.coroutines.launch
import org.json.JSONObject

class GasActivity : ComponentActivity() {
    private var etherWeb: EtherWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        etherWeb = EtherWeb(applicationContext).apply { showLog = false }
        setContent { EthereumTheme { GasScreen(etherWeb!!) } }
    }
    override fun onDestroy() {
        etherWeb?.release()
        etherWeb = null
        super.onDestroy()
    }
}

@Composable
private fun GasScreen(etherWeb: EtherWeb) {
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    LaunchedEffect(etherWeb) { val (ok, _) = etherWeb.setupAsync(); setupDone = ok }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Network: ${NetworkManager.currentNetworkLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                if (!setupDone || loading) return@Button
                loading = true; resultText = ""
                scope.launch {
                    val gas = etherWeb.getGasPriceAsync(NetworkManager.currentRpcUrl, NetworkManager.currentChainId)
                    val fees = etherWeb.getSuggestedFeesAsync(NetworkManager.currentRpcUrl, NetworkManager.currentChainId)
                    loading = false
                    resultText = buildString {
                        appendLine("getGasPrice: ${gas?.let { JSONObject(it).toString() } ?: "null"}")
                        appendLine("getSuggestedFees: ${fees?.let { JSONObject(it).toString() } ?: "null"}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Querying…" else "Query Gas") }
        if (loading) CircularProgressIndicator(Modifier.padding(8.dp))
        if (resultText.isNotEmpty()) {
            Text("Result", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(text = resultText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
