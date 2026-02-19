package com.smithSophiav.ethereum.account

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smithSophiav.ethereum.ui.theme.EthereumTheme
import com.smithSophiav.ethereum.util.copyToClipboard
import com.smithsophiav.etherweb.EtherWeb
import kotlinx.coroutines.launch

class ExportKeystoreActivity : ComponentActivity() {
    private var etherWeb: EtherWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        etherWeb = EtherWeb(applicationContext).apply { showLog = false }
        setContent { EthereumTheme { ExportKeystoreScreen(etherWeb!!) } }
    }

    override fun onDestroy() {
        etherWeb?.release()
        etherWeb = null
        super.onDestroy()
    }
}

@Composable
private fun ExportKeystoreScreen(etherWeb: EtherWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var privateKey by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var lastKeystore by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(etherWeb) {
        val (ok, _) = etherWeb.setupAsync()
        setupDone = ok
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Private key", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = privateKey, onValueChange = { privateKey = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0x...") }, singleLine = true)
        Text("Keystore password", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Password") }, singleLine = true)
        Button(
            onClick = {
                if (!setupDone || loading) return@Button
                val pk = privateKey.trim()
                val pwd = password
                if (pk.isEmpty() || pwd.isEmpty()) return@Button
                loading = true
                resultText = ""
                lastKeystore = null
                scope.launch {
                    val keystore = etherWeb.privateKeyToKeystoreAsync(pk, pwd)
                    loading = false
                    if (keystore != null) {
                        lastKeystore = keystore
                        resultText = keystore
                        isError = false
                    } else {
                        resultText = "Failed to generate keystore."
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading && privateKey.trim().isNotEmpty() && password.isNotEmpty()
        ) { Text(if (loading) "Generating…" else "Generate Keystore") }
        if (loading) Column(Modifier.fillMaxWidth()) { CircularProgressIndicator(Modifier.padding(8.dp)) }
        if (resultText.isNotEmpty()) {
            Text("Result", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(text = resultText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
            Button(onClick = { lastKeystore?.let { copyToClipboard(context, "keystore", it) } }, modifier = Modifier.fillMaxWidth()) { Text("Copy Keystore") }
        }
    }
}
