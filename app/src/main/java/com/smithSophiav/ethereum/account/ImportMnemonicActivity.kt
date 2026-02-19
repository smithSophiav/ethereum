package com.smithSophiav.ethereum.account

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
import org.json.JSONObject

class ImportMnemonicActivity : ComponentActivity() {
    private var etherWeb: EtherWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        etherWeb = EtherWeb(applicationContext).apply { showLog = false }
        setContent { EthereumTheme { ImportMnemonicScreen(etherWeb!!) } }
    }
    override fun onDestroy() {
        etherWeb?.release()
        etherWeb = null
        super.onDestroy()
    }
}

@Composable
private fun ImportMnemonicScreen(etherWeb: EtherWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var mnemonic by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<Map<String, Any>?>(null) }
    LaunchedEffect(etherWeb) { val (ok, _) = etherWeb.setupAsync(); setupDone = ok }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mnemonic", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = mnemonic, onValueChange = { mnemonic = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("12 or 24 words") }, minLines = 3)
        Button(
            onClick = {
                if (!setupDone || loading) return@Button
                val m = mnemonic.trim().replace("\n", " "); if (m.isEmpty()) return@Button
                loading = true; resultText = ""; lastResult = null
                scope.launch {
                    val r = etherWeb.importAccountFromMnemonicAsync(m)
                    loading = false
                    if (r != null) {
                        lastResult = r
                        resultText = "Address: ${r["address"]}\n\nPrivate Key: ${r["privateKey"]}\n\nMnemonic: ${r["mnemonic"]}"
                        isError = false
                    } else { resultText = "Invalid mnemonic."; isError = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading && mnemonic.trim().isNotEmpty()
        ) { Text(if (loading) "Importing…" else "Import") }
        if (loading) CircularProgressIndicator(Modifier.padding(8.dp))
        if (resultText.isNotEmpty()) {
            Text("Result", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(text = resultText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
            Button(onClick = { lastResult?.let { copyToClipboard(context, "result", JSONObject(it).toString()) } }, modifier = Modifier.fillMaxWidth()) { Text("Copy result") }
        }
    }
}
