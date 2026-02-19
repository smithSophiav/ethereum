# EtherWeb (Android)

Android 底层库：通过 WebView + JS Bridge 调用以太坊相关能力（钱包、余额、Gas、ETH/ERC20 转账、签名等），与 iOS EtherWeb 共用同一套 JS 逻辑（`pc/src/mobile.js` 打包产物）。

## 依赖方式（JitPack）

在项目根 `settings.gradle.kts` 或 `build.gradle.kts` 中确保有 JitPack 仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

在应用或模块的 `build.gradle.kts` 中：

```kotlin
dependencies {
    implementation("com.github.smithsophiav.eth-new-js:etherweb:1.0.0")
}
```

将 `1.0.0` 替换为你在 GitHub 上发布的 **Tag** 或 **Release** 版本号（JitPack 按 Tag 构建）。

## 环境要求

- **minSdk**: 24  
- **compileSdk**: 34  
- **Kotlin** + **Kotlinx Coroutines**（库已依赖）

## 快速使用

1. **创建并初始化**（需在 Activity/Fragment 生命周期内持有并释放）：

```kotlin
val etherWeb = EtherWeb(context).apply { showLog = false }
// 异步初始化（推荐）
lifecycleScope.launch {
    val (ok, error) = etherWeb.setupAsync()
    if (!ok) return@launch
    // 使用 API
}
// 使用完毕后释放
etherWeb.release()
```

2. **常用 API（均提供 callback 与 suspend 两种形式）**

| 分类 | 方法示例 |
|------|----------|
| 钱包 | `generateAccountAsync`, `importAccountFromPrivateKeyAsync`, `importAccountFromMnemonicAsync`, `importAccountFromKeystoreAsync`, `getAddressFromPrivateKeyAsync` |
| 余额 / Gas | `getETHBalanceAsync`, `getERC20TokenBalanceAsync`, `getGasPriceAsync`, `getSuggestedFeesAsync`, `estimateEthTransferGasAsync`, `estimateErc20TransferGasAsync` |
| 转账 | `ethTransferAsync`, `erc20TransferAsync` |
| 签名 | `signMessageAsync`, `verifyMessageAsync` |

3. **示例：ETH 余额 + 预估 Gas**

```kotlin
val rpcUrl = "https://eth.llamarpc.com"
val chainId = "1"
val balance = etherWeb.getETHBalanceAsync(address, rpcUrl, chainId)
val estimate = etherWeb.estimateEthTransferGasAsync(fromAddress, toAddress, valueEth, rpcUrl, chainId)
// estimate: gasLimit, gasPrice, estimatedFeeWei, estimatedFeeEth 等
```

4. **发送 ETH 时传入预估的 gas（可选）**

```kotlin
val gasLimit = estimate?.get("gasLimit") as? String
val gasPrice = estimate?.get("gasPrice") as? String
val tx = etherWeb.ethTransferAsync(privateKey, to, valueEth, gasLimit, gasPrice, null, null, rpcUrl, chainId)
```

## 模块结构

- `EtherWeb.kt`：对外入口，所有 API 在此类中。
- `bridge/`：WebView 与 JS 桥接（`WebViewJavascriptBridge`、协议与注入脚本）。
- `assets/`：`index.html` + `web3.js`（由 `pc/` 下 `npm run build:android` 生成并拷贝到此）。

设计说明见：[Android/docs/etherweb-js-bridge-design.md](../docs/etherweb-js-bridge-design.md)。

## 发布到 JitPack 前检查

发布前请对照 **[JITPACK.md](./JITPACK.md)** 逐项确认（仓库可见性、group/version、Gradle 任务、Tag/Release 等）。
