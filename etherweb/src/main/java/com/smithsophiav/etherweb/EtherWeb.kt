package com.smithsophiav.etherweb

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.smithsophiav.etherweb.bridge.WebViewJavascriptBridge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private fun Map<*, *>.toMapStringAny(): Map<String, Any> = entries.mapNotNull { (k, v) ->
    (k as? String)?.let { key -> key to (v as Any) }
}.toMap()

/**
 * EtherWeb - SDK for Ethereum wallet operations on Android.
 * Uses WebViewJavascriptBridge to call JS (mobile.js) handlers.
 * Call [setup] or [setupAsync] before using other APIs. Call [release] when done to free WebView.
 */
class EtherWeb(private val context: Context) {

    companion object {
        private const val TAG = "EtherWeb-SDK"
    }

    private var webView: WebView? = null
    private var bridge: WebViewJavascriptBridge? = null

    /** True after JS has signalled ready via FinishLoad. */
    var isInitialized: Boolean = false
        private set

    /** When true, bridge and WebView logs are printed to Logcat. */
    var showLog: Boolean = false

    init {
        setupWebView()
    }

    private fun setupWebView() {
        webView = WebView(context).apply {
            @Suppress("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (showLog) Log.d(TAG, "WebView finished loading: $url")
                }
            }
        }
        bridge = WebViewJavascriptBridge(webView!!, isHookConsole = true)
        bridge?.consolePipeClosure = { msg ->
            if (showLog) Log.d("EtherWeb-JS", msg.toString())
        }
    }

    /**
     * Initialize: register for JS ready signal (FinishLoad) then load assets.
     * Call this before using any other API.
     */
    fun setup(onCompleted: (Boolean, String?) -> Unit) {
        bridge?.register("FinishLoad") { _, callback ->
            if (showLog) Log.d(TAG, "JS Bridge ready (FinishLoad).")
            isInitialized = true
            onCompleted(true, null)
            callback?.invoke(mapOf("status" to "received"))
        }
        webView?.loadUrl("file:///android_asset/index.html")
    }

    /** Suspend until setup is complete. */
    suspend fun setupAsync(): Pair<Boolean, String?> = suspendCancellableCoroutine { cont ->
        setup { success, error ->
            cont.resume(Pair(success, error))
        }
    }

    /** Release WebView and bridge. Call from Activity/Fragment when no longer needed. */
    fun release() {
        bridge?.reset()
        bridge = null
        webView?.destroy()
        webView = null
        isInitialized = false
    }

    private fun unwrapResult(response: Any?): Map<String, Any>? {
        val wrapper = response as? Map<*, *> ?: return null
        if (wrapper["state"] != true) return null
        val result = wrapper["result"] ?: return null
        return if (result is Map<*, *>) result.toMapStringAny() else mapOf("result" to result)
    }

    private fun importUnwrap(response: Any?): Map<String, Any>? {
        val wrapper = response as? Map<*, *> ?: return null
        if (wrapper["state"] != true) return null
        return (wrapper["result"] as? Map<*, *>)?.toMapStringAny()
    }

    private fun call(handlerName: String, params: Map<String, Any?>, completion: (Any?) -> Unit) {
        val clean = params.mapNotNull { (k, v) -> if (v != null) k to v else null }.toMap()
        bridge?.call(handlerName, if (clean.isEmpty()) null else clean) { response ->
            completion(response)
        }
    }

    private suspend fun callAsync(handlerName: String, params: Map<String, Any?>): Any? =
        suspendCancellableCoroutine { cont ->
            call(handlerName, params) { cont.resume(it) }
        }

    // --- Wallet ---
    fun generateAccount(password: String? = null, completion: (Map<String, Any>?) -> Unit) {
        val params = mutableMapOf<String, Any?>()
        if (!password.isNullOrEmpty()) params["password"] = password
        call("generateAccount", params) { completion(importUnwrap(it)) }
    }

    suspend fun generateAccountAsync(password: String? = null): Map<String, Any>? =
        importUnwrap(callAsync("generateAccount", if (!password.isNullOrEmpty()) mapOf("password" to password) else emptyMap()))

    fun importAccountFromPrivateKey(privateKey: String, completion: (Map<String, Any>?) -> Unit) {
        call("importAccountFromPrivateKey", mapOf("privateKey" to privateKey)) { completion(importUnwrap(it)) }
    }

    suspend fun importAccountFromPrivateKeyAsync(privateKey: String): Map<String, Any>? =
        importUnwrap(callAsync("importAccountFromPrivateKey", mapOf("privateKey" to privateKey)))

    fun importAccountFromMnemonic(mnemonic: String, completion: (Map<String, Any>?) -> Unit) {
        call("importAccountFromMnemonic", mapOf("mnemonic" to mnemonic)) { completion(importUnwrap(it)) }
    }

    suspend fun importAccountFromMnemonicAsync(mnemonic: String): Map<String, Any>? =
        importUnwrap(callAsync("importAccountFromMnemonic", mapOf("mnemonic" to mnemonic)))

    fun importAccountFromKeystore(json: String, password: String, completion: (Map<String, Any>?) -> Unit) {
        call("importAccountFromKeystore", mapOf("json" to json, "password" to password)) { completion(importUnwrap(it)) }
    }

    suspend fun importAccountFromKeystoreAsync(json: String, password: String): Map<String, Any>? =
        importUnwrap(callAsync("importAccountFromKeystore", mapOf("json" to json, "password" to password)))

    fun privateKeyToKeystore(privateKey: String, password: String, completion: (String?) -> Unit) {
        call("privateKeyToKeystore", mapOf("privateKey" to privateKey, "password" to password)) { response ->
            val w = response as? Map<*, *>
            completion(if (w != null && w["state"] == true) w["result"] as? String else null)
        }
    }

    suspend fun privateKeyToKeystoreAsync(privateKey: String, password: String): String? {
        val r = callAsync("privateKeyToKeystore", mapOf("privateKey" to privateKey, "password" to password))
        val w = r as? Map<*, *>
        return if (w != null && w["state"] == true) w["result"] as? String else null
    }

    fun getAddressFromPrivateKey(privateKey: String, completion: (String?) -> Unit) {
        call("getAddressFromPrivateKey", mapOf("privateKey" to privateKey)) { response ->
            val w = response as? Map<*, *>
            completion(if (w != null && w["state"] == true) w["result"] as? String else null)
        }
    }

    suspend fun getAddressFromPrivateKeyAsync(privateKey: String): String? {
        val r = callAsync("getAddressFromPrivateKey", mapOf("privateKey" to privateKey))
        val w = r as? Map<*, *>
        return if (w != null && w["state"] == true) w["result"] as? String else null
    }

    // --- Sign / Verify ---
    fun signMessage(privateKey: String, message: String, completion: (String?) -> Unit) {
        call("signMessage", mapOf("privateKey" to privateKey, "message" to message)) { response ->
            val w = response as? Map<*, *>
            completion(if (w != null && w["state"] == true) w["result"] as? String else null)
        }
    }

    suspend fun signMessageAsync(privateKey: String, message: String): String? {
        val r = callAsync("signMessage", mapOf("privateKey" to privateKey, "message" to message))
        val w = r as? Map<*, *>
        return if (w != null && w["state"] == true) w["result"] as? String else null
    }

    fun verifyMessage(message: String, signature: String, completion: (String?) -> Unit) {
        call("verifyMessage", mapOf("message" to message, "signature" to signature)) { response ->
            val w = response as? Map<*, *>
            completion(if (w != null && w["state"] == true) w["result"] as? String else null)
        }
    }

    suspend fun verifyMessageAsync(message: String, signature: String): String? {
        val r = callAsync("verifyMessage", mapOf("message" to message, "signature" to signature))
        val w = r as? Map<*, *>
        return if (w != null && w["state"] == true) w["result"] as? String else null
    }

    fun verifyMessageSignature(message: String, signature: String, expectedAddress: String, completion: (Boolean) -> Unit) {
        call("verifyMessageSignature", mapOf("message" to message, "signature" to signature, "expectedAddress" to expectedAddress)) { response ->
            val w = response as? Map<*, *>
            completion(w != null && w["state"] == true && (w["result"] as? Boolean == true))
        }
    }

    suspend fun verifyMessageSignatureAsync(message: String, signature: String, expectedAddress: String): Boolean {
        val r = callAsync("verifyMessageSignature", mapOf("message" to message, "signature" to signature, "expectedAddress" to expectedAddress))
        val w = r as? Map<*, *>
        return w != null && w["state"] == true && (w["result"] as? Boolean == true)
    }

    // --- Balance / Gas ---
    fun getETHBalance(address: String, rpcUrl: String, chainId: String, completion: (String?) -> Unit) {
        call("getETHBalance", mapOf("address" to address, "rpcUrl" to rpcUrl, "chainId" to chainId)) { response ->
            val out = unwrapResult(response)
            completion(out?.get("result") as? String)
        }
    }

    suspend fun getETHBalanceAsync(address: String, rpcUrl: String, chainId: String): String? {
        val out = unwrapResult(callAsync("getETHBalance", mapOf("address" to address, "rpcUrl" to rpcUrl, "chainId" to chainId)))
        return out?.get("result") as? String
    }

    fun getERC20TokenBalance(
        tokenAddress: String,
        walletAddress: String,
        rpcUrl: String,
        chainId: String,
        completion: (Map<String, Any>?) -> Unit
    ) {
        call("getERC20TokenBalance", mapOf(
            "tokenAddress" to tokenAddress,
            "walletAddress" to walletAddress,
            "rpcUrl" to rpcUrl,
            "chainId" to chainId
        )) { completion(unwrapResult(it) as? Map<String, Any>) }
    }

    suspend fun getERC20TokenBalanceAsync(
        tokenAddress: String,
        walletAddress: String,
        rpcUrl: String,
        chainId: String
    ): Map<String, Any>? = unwrapResult(callAsync("getERC20TokenBalance", mapOf(
        "tokenAddress" to tokenAddress,
        "walletAddress" to walletAddress,
        "rpcUrl" to rpcUrl,
        "chainId" to chainId
    ))) as? Map<String, Any>

    fun getGasPrice(rpcUrl: String, chainId: String, completion: (Map<String, Any>?) -> Unit) {
        call("getGasPrice", mapOf("rpcUrl" to rpcUrl, "chainId" to chainId)) { completion(unwrapResult(it) as? Map<String, Any>) }
    }

    suspend fun getGasPriceAsync(rpcUrl: String, chainId: String): Map<String, Any>? =
        unwrapResult(callAsync("getGasPrice", mapOf("rpcUrl" to rpcUrl, "chainId" to chainId))) as? Map<String, Any>

    fun getSuggestedFees(rpcUrl: String, chainId: String, completion: (Map<String, Any>?) -> Unit) {
        call("getSuggestedFees", mapOf("rpcUrl" to rpcUrl, "chainId" to chainId)) { completion(unwrapResult(it) as? Map<String, Any>) }
    }

    suspend fun getSuggestedFeesAsync(rpcUrl: String, chainId: String): Map<String, Any>? =
        unwrapResult(callAsync("getSuggestedFees", mapOf("rpcUrl" to rpcUrl, "chainId" to chainId))) as? Map<String, Any>

    // --- Transfer ---
    fun estimateEthTransferGas(
        fromAddress: String,
        to: String,
        valueEth: String,
        rpcUrl: String,
        chainId: String,
        completion: (Map<String, Any>?) -> Unit
    ) {
        call("estimateEthTransferGas", mapOf(
            "fromAddress" to fromAddress,
            "to" to to,
            "valueEth" to valueEth,
            "rpcUrl" to rpcUrl,
            "chainId" to chainId
        )) { completion(unwrapResult(it) as? Map<String, Any>) }
    }

    suspend fun estimateEthTransferGasAsync(
        fromAddress: String,
        to: String,
        valueEth: String,
        rpcUrl: String,
        chainId: String
    ): Map<String, Any>? = unwrapResult(callAsync("estimateEthTransferGas", mapOf(
        "fromAddress" to fromAddress,
        "to" to to,
        "valueEth" to valueEth,
        "rpcUrl" to rpcUrl,
        "chainId" to chainId
    ))) as? Map<String, Any>

    fun ethTransfer(
        privateKey: String,
        to: String,
        valueEth: String,
        gasLimit: String?,
        gasPrice: String?,
        maxFeePerGas: String?,
        maxPriorityFeePerGas: String?,
        rpcUrl: String,
        chainId: String,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val params = mutableMapOf<String, Any?>(
            "privateKey" to privateKey,
            "to" to to,
            "valueEth" to valueEth,
            "rpcUrl" to rpcUrl,
            "chainId" to chainId
        )
        gasLimit?.let { params["gasLimit"] = it }
        gasPrice?.let { params["gasPrice"] = it }
        maxFeePerGas?.let { params["maxFeePerGas"] = it }
        maxPriorityFeePerGas?.let { params["maxPriorityFeePerGas"] = it }
        call("ethTransfer", params) { completion(unwrapResult(it) as? Map<String, Any>) }
    }

    suspend fun ethTransferAsync(
        privateKey: String,
        to: String,
        valueEth: String,
        gasLimit: String?,
        gasPrice: String?,
        maxFeePerGas: String?,
        maxPriorityFeePerGas: String?,
        rpcUrl: String,
        chainId: String
    ): Map<String, Any>? {
        val params = mutableMapOf<String, Any?>(
            "privateKey" to privateKey,
            "to" to to,
            "valueEth" to valueEth,
            "rpcUrl" to rpcUrl,
            "chainId" to chainId
        )
        gasLimit?.let { params["gasLimit"] = it }
        gasPrice?.let { params["gasPrice"] = it }
        maxFeePerGas?.let { params["maxFeePerGas"] = it }
        maxPriorityFeePerGas?.let { params["maxPriorityFeePerGas"] = it }
        return unwrapResult(callAsync("ethTransfer", params)) as? Map<String, Any>
    }

    fun estimateErc20TransferGas(
        fromAddress: String,
        tokenAddress: String,
        to: String,
        amountHuman: String,
        decimals: Int,
        rpcUrl: String,
        chainId: String,
        completion: (Map<String, Any>?) -> Unit
    ) {
        call("estimateErc20TransferGas", mapOf(
            "fromAddress" to fromAddress,
            "tokenAddress" to tokenAddress,
            "to" to to,
            "amountHuman" to amountHuman,
            "decimals" to decimals,
            "rpcUrl" to rpcUrl,
            "chainId" to chainId
        )) { completion(unwrapResult(it) as? Map<String, Any>) }
    }

    suspend fun estimateErc20TransferGasAsync(
        fromAddress: String,
        tokenAddress: String,
        to: String,
        amountHuman: String,
        decimals: Int,
        rpcUrl: String,
        chainId: String
    ): Map<String, Any>? = unwrapResult(callAsync("estimateErc20TransferGas", mapOf(
        "fromAddress" to fromAddress,
        "tokenAddress" to tokenAddress,
        "to" to to,
        "amountHuman" to amountHuman,
        "decimals" to decimals,
        "rpcUrl" to rpcUrl,
        "chainId" to chainId
    ))) as? Map<String, Any>

    fun erc20Transfer(
        privateKey: String,
        tokenAddress: String,
        to: String,
        amountHuman: String,
        decimals: Int,
        gasLimit: String?,
        gasPrice: String?,
        maxFeePerGas: String?,
        maxPriorityFeePerGas: String?,
        rpcUrl: String,
        chainId: String,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val params = mutableMapOf<String, Any?>(
            "privateKey" to privateKey,
            "tokenAddress" to tokenAddress,
            "to" to to,
            "amountHuman" to amountHuman,
            "decimals" to decimals,
            "rpcUrl" to rpcUrl,
            "chainId" to chainId
        )
        gasLimit?.let { params["gasLimit"] = it }
        gasPrice?.let { params["gasPrice"] = it }
        maxFeePerGas?.let { params["maxFeePerGas"] = it }
        maxPriorityFeePerGas?.let { params["maxPriorityFeePerGas"] = it }
        call("erc20Transfer", params) { completion(unwrapResult(it) as? Map<String, Any>) }
    }

    suspend fun erc20TransferAsync(
        privateKey: String,
        tokenAddress: String,
        to: String,
        amountHuman: String,
        decimals: Int,
        gasLimit: String?,
        gasPrice: String?,
        maxFeePerGas: String?,
        maxPriorityFeePerGas: String?,
        rpcUrl: String,
        chainId: String
    ): Map<String, Any>? {
        val params = mutableMapOf<String, Any?>(
            "privateKey" to privateKey,
            "tokenAddress" to tokenAddress,
            "to" to to,
            "amountHuman" to amountHuman,
            "decimals" to decimals,
            "rpcUrl" to rpcUrl,
            "chainId" to chainId
        )
        gasLimit?.let { params["gasLimit"] = it }
        gasPrice?.let { params["gasPrice"] = it }
        maxFeePerGas?.let { params["maxFeePerGas"] = it }
        maxPriorityFeePerGas?.let { params["maxPriorityFeePerGas"] = it }
        return unwrapResult(callAsync("erc20Transfer", params)) as? Map<String, Any>
    }
}
