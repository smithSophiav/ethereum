# EtherWeb (Android)

Android SDK for Ethereum wallet operations: create/import accounts, query balances, estimate gas, send ETH/ERC20, and sign/verify messages.
![language](https://img.shields.io/badge/Language-Kotlin-green)
![jitpack](https://img.shields.io/badge/support-jitpack-green)

---

## Features

- **Wallet**: Generate account, import from private key / mnemonic / keystore, derive address from private key
- **Balance & Gas**: ETH balance, ERC20 balance, gas price, suggested fees, estimate transfer gas
- **Transfer**: Send ETH and ERC20 with optional gas parameters (gas limit, gas price, EIP-1559)
- **Sign / Verify**: Sign message, verify signature, verify message against expected address
- **API style**: Every operation has both **callback** and **suspend (coroutine)** variants

---

## Requirements

| Item        | Requirement |
|------------|-------------|
| minSdk      | 24          |
| compileSdk | 34          |
| Kotlin     | 1.9+        |
| Kotlinx Coroutines | Used by `*Async` APIs (library brings it in) |

---

## Installation

### 1. Add JitPack repository

In your **root** `settings.gradle.kts` (or `settings.gradle`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add dependency

In your **app** (or feature) module `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.smithSophiav:ethereum:1.0.0")
}
```
---

## Quick Start

1. **Create** an instance (e.g. in Activity/Fragment), **initialize** once, then call APIs. **Release** when done (e.g. in `onDestroy`).

2. **Callback style:**

```kotlin
val etherWeb = EtherWeb(context)
etherWeb.showLog = true  // optional: Logcat logs from bridge/WebView

etherWeb.setup { success, error ->
    if (!success) return@setup
    // Now etherWeb.isInitialized == true

    etherWeb.getETHBalanceAsync(address, "https://eth.llamarpc.com", "1") { balance ->
        // balance: String? (wei as string, or null on error)
    }
}

// When leaving screen / no longer needed
etherWeb.release()
```

3. **Coroutine style (recommended):**

```kotlin
val etherWeb = EtherWeb(context)

lifecycleScope.launch {
    val (ok, error) = etherWeb.setupAsync()
    if (!ok) return@launch

    val balance = etherWeb.getETHBalanceAsync(
        address,
        "https://eth.llamarpc.com",
        "1"
    )
    val account = etherWeb.importAccountFromPrivateKeyAsync(privateKey)
    // ...
}

onDestroy() {
    etherWeb.release()
}
```

---

## Initialization

- **Must** call `setup(...)` or `setupAsync()` before any other API. The SDK loads a local HTML/JS bundle and waits for a “ready” signal from JS.
- **`isInitialized`** is `true` after setup completes successfully.
- **`showLog`**: set to `true` to print bridge and WebView logs to Logcat (handy for debugging).
- **`release()`**: call when the Activity/Fragment is destroyed or you no longer need the SDK (frees WebView and bridge).

---

## API Overview

All methods that return a single value have two forms:

- **Callback**: `method(..., completion: (Result?) -> Unit)`
- **Coroutine**: `suspend methodAsync(...): Result?`

Return types: `null` or failed state usually means an error; check the map/string for success data.

### Wallet

| Method | Description |
|--------|-------------|
| `generateAccount(password?)` | Create new account; returns map with address, privateKey, etc. |
| `importAccountFromPrivateKey(privateKey)` | Import from private key |
| `importAccountFromMnemonic(mnemonic)` | Import from BIP-39 mnemonic |
| `importAccountFromKeystore(json, password)` | Import from encrypted keystore JSON |
| `getAddressFromPrivateKey(privateKey)` | Derive Ethereum address (String) |
| `privateKeyToKeystore(privateKey, password)` | Export private key to keystore JSON (String) |

Account-import APIs return `Map<String, Any>?` (e.g. `address`, `privateKey`). Others return `String?` where noted.

### Sign / Verify

| Method | Description |
|--------|-------------|
| `signMessage(privateKey, message)` | Sign message; returns signature `String?` |
| `verifyMessage(message, signature)` | Recover signer address from signature |
| `verifyMessageSignature(message, signature, expectedAddress)` | Returns `Boolean`: signature matches expected address |

### Balance & Gas

| Method | Description |
|--------|-------------|
| `getETHBalance(address, rpcUrl, chainId)` | ETH balance (wei as `String?`) |
| `getERC20TokenBalance(tokenAddress, walletAddress, rpcUrl, chainId)` | ERC20 balance `Map<String, Any>?` |
| `getGasPrice(rpcUrl, chainId)` | Current gas price `Map<String, Any>?` |
| `getSuggestedFees(rpcUrl, chainId)` | Suggested fees (e.g. EIP-1559) `Map<String, Any>?` |
| `estimateEthTransferGas(fromAddress, to, valueEth, rpcUrl, chainId)` | Gas estimate for ETH transfer |
| `estimateErc20TransferGas(fromAddress, tokenAddress, to, amountHuman, decimals, rpcUrl, chainId)` | Gas estimate for ERC20 transfer |

Gas/estimate results typically include keys like `gasLimit`, `gasPrice`, `estimatedFeeWei`, `estimatedFeeEth`, etc.

### Transfer

| Method | Description |
|--------|-------------|
| `ethTransfer(privateKey, to, valueEth, gasLimit?, gasPrice?, maxFeePerGas?, maxPriorityFeePerGas?, rpcUrl, chainId)` | Send ETH. Optional gas params; pass `null` to use defaults. Returns tx result `Map<String, Any>?`. |
| `erc20Transfer(privateKey, tokenAddress, to, amountHuman, decimals, gasLimit?, gasPrice?, maxFeePerGas?, maxPriorityFeePerGas?, rpcUrl, chainId)` | Send ERC20. Same gas options. Returns tx result `Map<String, Any>?`. |

---

## Examples

### ETH balance and transfer with estimated gas

```kotlin
val rpcUrl = "https://eth.llamarpc.com"
val chainId = "1"

val balance = etherWeb.getETHBalanceAsync(address, rpcUrl, chainId)

val estimate = etherWeb.estimateEthTransferGasAsync(
    fromAddress, toAddress, valueEth, rpcUrl, chainId
)
val gasLimit = estimate?.get("gasLimit") as? String
val gasPrice = estimate?.get("gasPrice") as? String

val txResult = etherWeb.ethTransferAsync(
    privateKey, to, valueEth,
    gasLimit, gasPrice, null, null,
    rpcUrl, chainId
)
```

### ERC20 balance and transfer

```kotlin
val balanceMap = etherWeb.getERC20TokenBalanceAsync(
    tokenAddress, walletAddress, rpcUrl, chainId
)

val txResult = etherWeb.erc20TransferAsync(
    privateKey, tokenAddress, to, "1.5", 18,
    gasLimit, gasPrice, null, null,
    rpcUrl, chainId
)
```

### Sign and verify message

```kotlin
val signature = etherWeb.signMessageAsync(privateKey, "Hello, Ethereum")
val isValid = etherWeb.verifyMessageSignatureAsync(
    "Hello, Ethereum", signature!!, expectedAddress
)
```

---

## Lifecycle

- Create `EtherWeb(context)` when you need it (e.g. in Activity/Fragment).
- Call `setup()` or `setupAsync()` once before any other call.
- Call `release()` when the UI is destroyed or the feature is no longer needed (e.g. in `onDestroy()`). This destroys the WebView and clears the bridge.

---

## License

See the repository root or the [LICENSE](https://github.com/smithSophiav/ethereum/blob/main/LICENSE) file.
