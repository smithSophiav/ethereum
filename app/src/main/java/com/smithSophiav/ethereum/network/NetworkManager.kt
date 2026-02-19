package com.smithSophiav.ethereum.network

import android.content.Context
import android.content.SharedPreferences

/**
 * 与 iOS NetworkManager 对齐：当前网络单例，持久化 key，提供 rpcUrl / chainId / label。
 */
object NetworkManager {

    data class NetworkModel(
        val key: String,
        val label: String,
        val rpcUrl: String,
        val chainId: String
    )

    private const val PREFS_NAME = "com.smithSophiav.ethereum.network"
    private const val KEY_CURRENT_NETWORK = "currentNetworkKey"

    private val networks = listOf(
        NetworkModel(
            key = "mainnet",
            label = "Mainnet",
            rpcUrl = "https://mainnet.infura.io/v3/fe816c09404d406f8f47af0b78413806",
            chainId = "1"
        ),
        NetworkModel(
            key = "sepolia",
            label = "Sepolia",
            rpcUrl = "https://sepolia.infura.io/v3/fe816c09404d406f8f47af0b78413806",
            chainId = "11155111"
        )
    )

    @Volatile
    private var _currentNetworkKey: String = "sepolia"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs?.getString(KEY_CURRENT_NETWORK, "sepolia") ?: "sepolia"
            _currentNetworkKey = if (networks.any { it.key == saved }) saved else "sepolia"
            if (_currentNetworkKey != saved) {
                prefs?.edit()?.putString(KEY_CURRENT_NETWORK, _currentNetworkKey)?.apply()
            }
        }
    }

    val currentNetworkKey: String get() = _currentNetworkKey

    fun allNetworks(): List<NetworkModel> = networks

    fun currentNetwork(): NetworkModel? = networks.firstOrNull { it.key == _currentNetworkKey }

    val currentRpcUrl: String
        get() = currentNetwork()?.rpcUrl ?: networks.firstOrNull()?.rpcUrl ?: ""

    val currentChainId: String
        get() = currentNetwork()?.chainId ?: networks.firstOrNull()?.chainId ?: ""

    val currentNetworkLabel: String
        get() = currentNetwork()?.label ?: _currentNetworkKey

    fun setCurrentNetwork(key: String) {
        if (!networks.any { it.key == key }) return
        _currentNetworkKey = key
        prefs?.edit()?.putString(KEY_CURRENT_NETWORK, key)?.apply()
    }
}
