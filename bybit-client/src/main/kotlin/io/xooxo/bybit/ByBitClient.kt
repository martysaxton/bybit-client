package io.xooxo.bybit

import io.xooxo.bybit.model.DepthDeltaListener
import io.xooxo.bybit.model.DepthSnapshotListener
import io.xooxo.bybit.model.TradeListener

interface ByBitClient {
    enum class Type(val baseUrl: String, val webSocketUrl: String) {
        LIVE("https://api.bybit.com", "wss://stream.bybit.com/realtime"),
        TEST("https://api-testnet.bybit.com", "wss://stream-testnet.bybit.com/realtime")
    }

    fun connectWebSocket()
    fun close()

    fun setConnectListener(connectListener: () -> Unit)
    fun setCloseListener(closeListener: () -> Unit)

    fun subscribeToTrades()
    fun setTradeListener(tradeListener: TradeListener)

    fun subscribeToOrderBook(symbol: String)
    fun setDepthListeners(depthSnapshotListener: DepthSnapshotListener, depthDeltaListener: DepthDeltaListener)

}