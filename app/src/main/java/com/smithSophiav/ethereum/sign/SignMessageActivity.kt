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
import com.smithSophiav.ethereum.util.copyToClipboard
import com.smithsophiav.etherweb.EtherWeb
import kotlinx.coroutines.launch

class SignMessageActivity : ComponentActivity() {
    private var etherWeb: EtherWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        etherWeb = EtherWeb(applicationContext).apply { showLog = false }
        setContent { EthereumTheme { SignMessageScreen(etherWeb!!) } }
    }
    override fun onDestroy() {
        etherWeb?.release()
        etherWeb = null
        super.onDestroy()
    }
}

@Composable
private fun SignMessageScreen(etherWeb: EtherWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var privateKey by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var signature by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    LaunchedEffect(etherWeb) { val (ok, _) = etherWeb.setupAsync(); setupDone = ok }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Private key", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = privateKey, onValueChange = { privateKey = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Text("Message", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Message to sign") }, minLines = 3)
        Button(
            onClick = {
                if (!setupDone || loading) return@Button
                val pk = privateKey.trim(); val msg = message.trim()
                if (pk.isEmpty() || msg.isEmpty()) return@Button
                loading = true; signature = null
                scope.launch {
                    val sig = etherWeb.signMessageAsync(pk, msg)
                    loading = false
                    signature = sig
                    isError = sig == null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading && privateKey.trim().isNotEmpty() && message.trim().isNotEmpty()
        ) { Text(if (loading) "Signing…" else "Sign Message") }
        if (loading) CircularProgressIndicator(Modifier.padding(8.dp))
        signature?.let { sig ->
            Text("Signature", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(text = sig, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
            Button(onClick = { copyToClipboard(context, "signature", sig) }, modifier = Modifier.fillMaxWidth()) { Text("Copy Signature") }
        }
    }
}
