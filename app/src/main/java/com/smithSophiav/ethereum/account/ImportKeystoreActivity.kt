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

class ImportKeystoreActivity : ComponentActivity() {
    private var etherWeb: EtherWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        etherWeb = EtherWeb(applicationContext).apply { showLog = false }
        setContent { EthereumTheme { ImportKeystoreScreen(etherWeb!!) } }
    }
    override fun onDestroy() {
        etherWeb?.release()
        etherWeb = null
        super.onDestroy()
    }
}

@Composable
private fun ImportKeystoreScreen(etherWeb: EtherWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var keystoreJson by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<Map<String, Any>?>(null) }
    LaunchedEffect(etherWeb) { val (ok, _) = etherWeb.setupAsync(); setupDone = ok }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Keystore JSON", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = keystoreJson, onValueChange = { keystoreJson = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Paste keystore JSON") }, minLines = 4)
        Text("Password", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Keystore password") }, singleLine = true)
        Button(
            onClick = {
                if (!setupDone || loading) return@Button
                val json = keystoreJson.trim(); val pwd = password
                if (json.isEmpty() || pwd.isEmpty()) return@Button
                loading = true; resultText = ""; lastResult = null
                scope.launch {
                    val r = etherWeb.importAccountFromKeystoreAsync(json, pwd)
                    loading = false
                    if (r != null) {
                        lastResult = r
                        resultText = "Address: ${r["address"]}\n\nPrivate Key: ${r["privateKey"]}"
                        isError = false
                    } else { resultText = "Invalid keystore or password."; isError = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading && keystoreJson.trim().isNotEmpty() && password.isNotEmpty()
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
