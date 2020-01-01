package io.xooxo.bybit

import io.xooxo.bybit.impl.ByBitClientImpl

fun main(args: Array<String>) {
    val client = ByBitClientImpl(ByBitClient.Type.LIVE)
    client.setCloseListener { println("websocket closed") }
    client.setTradeListener { println("trade: $it") }
    client.setInstrumentInfoListeners(
            { println("instrument info snapshot:  $it") },
            { println("instrument info delta: $it") }
    )
    client.setDepthListeners(
            { println("depth snapshot: $it") },
            { println("depth delta $it") }
    )
    client.setConnectListener {
        println("websocket connected")
        client.subscribeToTrades()
        client.subscribeToInstrumentInfo("BTCUSD")
        client.subscribeToOrderBook("BTCUSD")
    }
    client.connectWebSocket()

}