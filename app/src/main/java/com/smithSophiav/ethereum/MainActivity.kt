package com.smithSophiav.ethereum

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smithSophiav.ethereum.account.ExportKeystoreActivity
import com.smithSophiav.ethereum.account.GenerateAccountActivity
import com.smithSophiav.ethereum.account.ImportKeystoreActivity
import com.smithSophiav.ethereum.account.ImportMnemonicActivity
import com.smithSophiav.ethereum.account.ImportPrivateKeyActivity
import com.smithSophiav.ethereum.balance.ERC20BalanceActivity
import com.smithSophiav.ethereum.balance.ETHBalanceActivity
import com.smithSophiav.ethereum.gas.GasActivity
import com.smithSophiav.ethereum.network.NetworkActivity
import com.smithSophiav.ethereum.network.NetworkManager
import com.smithSophiav.ethereum.sign.SignMessageActivity
import com.smithSophiav.ethereum.sign.VerifyMessageActivity
import com.smithSophiav.ethereum.transfer.Erc20TransferActivity
import com.smithSophiav.ethereum.transfer.EthTransferActivity
import com.smithSophiav.ethereum.ui.theme.EthereumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            EthereumTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WalletMenu(
                        modifier = Modifier.padding(innerPadding),
                        onItemClick = { startActivity(Intent(this, it)) }
                    )
                }
            }
        }
    }
}

private data class MenuSection(
    val title: String,
    val items: List<Pair<String, Class<out ComponentActivity>>>
)

@Composable
private fun WalletMenu(
    modifier: Modifier = Modifier,
    onItemClick: (Class<out ComponentActivity>) -> Unit
) {
    val sections = listOf(
        MenuSection("Network", listOf(
            "Network" to NetworkActivity::class.java
        )),
        MenuSection("Account", listOf(
            "Generate Wallet" to GenerateAccountActivity::class.java,
            "Import from Private Key" to ImportPrivateKeyActivity::class.java,
            "Import from Mnemonic" to ImportMnemonicActivity::class.java,
            "Import from Keystore" to ImportKeystoreActivity::class.java,
            "Private Key to Keystore" to ExportKeystoreActivity::class.java
        )),
        MenuSection("Sign", listOf(
            "Sign Message" to SignMessageActivity::class.java,
            "Verify Message" to VerifyMessageActivity::class.java
        )),
        MenuSection("Balance", listOf(
            "ETH Balance" to ETHBalanceActivity::class.java,
            "ERC20 Balance" to ERC20BalanceActivity::class.java
        )),
        MenuSection("Transfer", listOf(
            "ETH Transfer" to EthTransferActivity::class.java,
            "ERC20 Transfer" to Erc20TransferActivity::class.java
        )),
        MenuSection("Gas", listOf(
            "Gas" to GasActivity::class.java
        ))
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.forEach { section ->
            item {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            items(section.items) { (title, activityClass) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(activityClass) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
