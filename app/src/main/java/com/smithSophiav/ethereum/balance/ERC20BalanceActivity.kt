package com.smithSophiav.ethereum.balance

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

class ERC20BalanceActivity : ComponentActivity() {
    private var etherWeb: EtherWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        etherWeb = EtherWeb(applicationContext).apply { showLog = false }
        setContent { EthereumTheme { ERC20BalanceScreen(etherWeb!!) } }
    }
    override fun onDestroy() {
        etherWeb?.release()
        etherWeb = null
        super.onDestroy()
    }
}

@Composable
private fun ERC20BalanceScreen(etherWeb: EtherWeb) {
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var tokenAddress by remember { mutableStateOf("") }
    var walletAddress by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    LaunchedEffect(etherWeb) { val (ok, _) = etherWeb.setupAsync(); setupDone = ok }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Network: ${NetworkManager.currentNetworkLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Token contract", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = tokenAddress, onValueChange = { tokenAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Text("Wallet address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = walletAddress, onValueChange = { walletAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Button(
            onClick = {
                if (!setupDone || loading) return@Button
                val t = tokenAddress.trim(); val w = walletAddress.trim()
                if (t.isEmpty() || w.isEmpty()) return@Button
                loading = true; resultText = null
                scope.launch {
                    val r = etherWeb.getERC20TokenBalanceAsync(t, w, NetworkManager.currentRpcUrl, NetworkManager.currentChainId)
                    loading = false
                    resultText = if (r != null) "Balance: ${r["balance"]} (decimals: ${r["decimals"]})" else "Failed to fetch balance."
                    isError = r == null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading && tokenAddress.trim().isNotEmpty() && walletAddress.trim().isNotEmpty()
        ) { Text(if (loading) "Querying…" else "Query Balance") }
        if (loading) CircularProgressIndicator(Modifier.padding(8.dp))
        resultText?.let { text ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(text = text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
