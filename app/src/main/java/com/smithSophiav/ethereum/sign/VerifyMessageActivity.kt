package com.smithSophiav.ethereum.sign

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
import com.smithSophiav.ethereum.ui.theme.EthereumTheme
import com.smithsophiav.etherweb.EtherWeb
import kotlinx.coroutines.launch

class VerifyMessageActivity : ComponentActivity() {
    private var etherWeb: EtherWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        etherWeb = EtherWeb(applicationContext).apply { showLog = false }
        setContent { EthereumTheme { VerifyMessageScreen(etherWeb!!) } }
    }
    override fun onDestroy() {
        etherWeb?.release()
        etherWeb = null
        super.onDestroy()
    }
}

@Composable
private fun VerifyMessageScreen(etherWeb: EtherWeb) {
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var expectedAddress by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    LaunchedEffect(etherWeb) { val (ok, _) = etherWeb.setupAsync(); setupDone = ok }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Message", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Text("Signature", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = signature, onValueChange = { signature = it }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Text("Expected signer address (optional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = expectedAddress, onValueChange = { expectedAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Button(
            onClick = {
                if (!setupDone || loading) return@Button
                val msg = message.trim(); val sig = signature.trim()
                if (msg.isEmpty() || sig.isEmpty()) return@Button
                loading = true; resultText = null
                scope.launch {
                    val recovered = etherWeb.verifyMessageAsync(msg, sig)
                    if (!recovered.isNullOrEmpty()) {
                        val exp = expectedAddress.trim()
                        if (exp.isNotEmpty()) {
                            val match = etherWeb.verifyMessageSignatureAsync(msg, sig, exp)
                            resultText = "Recovered: $recovered\nMatch expected: $match"
                            isError = !match
                        } else {
                            resultText = "Recovered address: $recovered"
                            isError = false
                        }
                    } else {
                        resultText = "Invalid signature or message."
                        isError = true
                    }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading && message.trim().isNotEmpty() && signature.trim().isNotEmpty()
        ) { Text(if (loading) "Verifying…" else "Verify") }
        if (loading) CircularProgressIndicator(Modifier.padding(8.dp))
        resultText?.let { text ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(text = text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
