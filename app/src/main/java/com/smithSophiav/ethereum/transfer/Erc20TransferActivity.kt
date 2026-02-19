package com.smithSophiav.ethereum.transfer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smithSophiav.ethereum.network.NetworkManager
import com.smithSophiav.ethereum.ui.theme.EthereumTheme
import com.smithSophiav.ethereum.util.copyToClipboard
import com.smithsophiav.etherweb.EtherWeb
import kotlinx.coroutines.launch
import org.json.JSONObject

class Erc20TransferActivity : ComponentActivity() {
    private var etherWeb: EtherWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        etherWeb = EtherWeb(applicationContext).apply { showLog = false }
        setContent { EthereumTheme { Erc20TransferScreen(etherWeb!!) } }
    }
    override fun onDestroy() {
        etherWeb?.release()
        etherWeb = null
        super.onDestroy()
    }
}

@Composable
private fun Erc20TransferScreen(etherWeb: EtherWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var privateKey by remember { mutableStateOf("") }
    var fromAddress by remember { mutableStateOf("") }
    var tokenAddress by remember { mutableStateOf("") }
    var toAddress by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var decimals by remember { mutableStateOf("18") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var lastEstimate by remember { mutableStateOf<Map<String, Any>?>(null) }
    var lastTx by remember { mutableStateOf<Map<String, Any>?>(null) }
    val rpcUrl = NetworkManager.currentRpcUrl
    val chainId = NetworkManager.currentChainId

    LaunchedEffect(etherWeb) { val (ok, _) = etherWeb.setupAsync(); setupDone = ok }

    val decInt = decimals.trim().toIntOrNull()
    val decimalsValid = decInt != null && decInt in 0..255
    val canEstimate = setupDone && !loading && tokenAddress.trim().isNotEmpty() && toAddress.trim().isNotEmpty() && amount.trim().isNotEmpty() && decimalsValid && (fromAddress.trim().isNotEmpty() || privateKey.trim().isNotEmpty())
    val canSend = setupDone && !loading && privateKey.trim().isNotEmpty() && tokenAddress.trim().isNotEmpty() && toAddress.trim().isNotEmpty() && amount.trim().isNotEmpty() && decimalsValid

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Network: ${NetworkManager.currentNetworkLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Private key", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = privateKey, onValueChange = { privateKey = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Text("From address (optional, for estimate)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = fromAddress, onValueChange = { fromAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Text("Token contract", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = tokenAddress, onValueChange = { tokenAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Text("To address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = toAddress, onValueChange = { toAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Text("Amount", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g. 10") }, singleLine = true)
        Text("Decimals", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = decimals, onValueChange = { decimals = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("18") }, singleLine = true)
        if (decimals.trim().isNotEmpty() && !decimalsValid) {
            Text("Decimals must be 0–255", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (!canEstimate) return@Button
                val dec = decInt ?: return@Button
                val fromText = fromAddress.trim()
                val pk = privateKey.trim()
                val token = tokenAddress.trim()
                val to = toAddress.trim()
                val amt = amount.trim()
                if (token.isEmpty() || to.isEmpty() || amt.isEmpty()) return@Button
                if (fromText.isEmpty() && pk.isEmpty()) return@Button
                loading = true
                resultText = ""
                lastEstimate = null
                isError = false
                scope.launch {
                    var from = fromText
                    if (from.isEmpty()) {
                        from = etherWeb.getAddressFromPrivateKeyAsync(pk) ?: ""
                        if (from.isEmpty()) {
                            loading = false
                            resultText = "Invalid private key."
                            isError = true
                            return@launch
                        }
                    }
                    val estimate = etherWeb.estimateErc20TransferGasAsync(from, token, to, amt, dec, rpcUrl, chainId)
                    loading = false
                    lastEstimate = estimate
                    if (estimate != null) {
                        val lines = buildList {
                            estimate["gasLimit"]?.let { add("Gas limit: $it") }
                            estimate["gasPrice"]?.let { add("Gas price: $it wei") }
                            estimate["estimatedFeeEth"]?.let { add("Est. fee: $it ETH") }
                        }
                        resultText = lines.joinToString("\n")
                        isError = false
                    } else {
                        resultText = "Estimate failed."
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canEstimate
        ) { Text("Estimate Gas") }

        Button(
            onClick = {
                if (!canSend) return@Button
                val dec = decInt ?: return@Button
                val pk = privateKey.trim()
                val token = tokenAddress.trim()
                val to = toAddress.trim()
                val amt = amount.trim()
                loading = true
                resultText = "Sending..."
                isError = false
                val gasLimit = lastEstimate?.get("gasLimit") as? String
                val gasPrice = lastEstimate?.get("gasPrice") as? String
                scope.launch {
                    val tx = etherWeb.erc20TransferAsync(pk, token, to, amt, dec, gasLimit, gasPrice, null, null, rpcUrl, chainId)
                    loading = false
                    if (tx != null) {
                        lastTx = tx
                        resultText = "Tx hash: ${tx["hash"]}\nFrom: ${tx["from"]}\nTo: ${tx["to"]}"
                        isError = false
                    } else {
                        resultText = "Send failed."
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSend
        ) { Text(if (loading) "Sending…" else "Send") }

        if (loading) CircularProgressIndicator(Modifier.padding(8.dp))
        if (resultText.isNotEmpty()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(
                    text = resultText,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            lastTx?.let { Button(onClick = { copyToClipboard(context, "tx", JSONObject(it).toString()) }, modifier = Modifier.fillMaxWidth()) { Text("Copy Json") } }
        }
    }
}
