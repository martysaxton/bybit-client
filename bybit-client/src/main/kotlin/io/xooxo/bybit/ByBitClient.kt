package io.xooxo.bybit

import io.xooxo.bybit.model.TradeMessage

interface ByBitClient {
    enum class Type(val baseUrl: String, val webSocketUrl: String) {
        LIVE("https://api.bybit.com", "wss://stream.bybit.com/realtime"),
        TEST("https://api-testnet.bybit.com", "wss://stream-testnet.bybit.com/realtime")
    }

    fun connectWebSocket()
    fun close()

    fun setConnectListener(connectListener: () -> Unit)
    fun setCloseListener(connectListener: () -> Unit)

    fun subscribeToTrades()
    fun setTradeLisetner(tradeListener: (tradeMessage: TradeMessage) -> Unit)


}