package com.smithsophiav.etherweb.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.webkit.WebViewCompat

/**
 * WebViewJavascriptBridge - Main interface class that encapsulates Android WebView integration
 */
class WebViewJavascriptBridge(
    private val webView: WebView,
    private val otherJSCode: String = "",
    private val isHookConsole: Boolean = true
) {

    private val base = WebViewJavascriptBridgeBase()
    var consolePipeClosure: ((Any?) -> Unit)? = null

    init {
        base.setEvaluateJavascript { javascript, callback ->
            webView.evaluateJavascript(javascript) { result ->
                callback?.invoke(result)
            }
        }
        addJavaScriptInterface()
        injectJavascriptFile()
    }

    private fun addJavaScriptInterface() {
        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")
    }

    private fun injectJavascriptFile() {
        val bridgeJS = JavascriptCode.bridge()
        val hookConsoleJS = if (isHookConsole) JavascriptCode.hookConsole() else ""
        var finalJS = "$bridgeJS\n$hookConsoleJS"
        if (otherJSCode.isNotEmpty()) {
            finalJS = "$finalJS\n$otherJSCode"
        }
        WebViewCompat.addDocumentStartJavaScript(
            webView,
            finalJS,
            setOf("*")
        )
    }

    fun reset() {
        base.reset()
    }

    fun register(handlerName: String, handler: BridgeHandler) {
        base.register(handlerName, handler)
    }

    fun remove(handlerName: String): BridgeHandler? {
        return base.remove(handlerName)
    }

    fun call(handlerName: String, data: Any? = null, callback: BridgeCallback? = null) {
        base.send(handlerName, data, callback)
    }

    internal fun handleMessage(messageJSON: String) {
        base.flush(messageJSON)
    }

    internal fun handleConsoleLog(message: String) {
        consolePipeClosure?.invoke(message)
    }

    class AndroidBridge(private val bridge: WebViewJavascriptBridge) {

        @JavascriptInterface
        fun postMessage(messageJSON: String) {
            bridge.handleMessage(messageJSON)
        }

        @JavascriptInterface
        fun consoleLog(message: String) {
            bridge.handleConsoleLog(message)
        }
    }
}
