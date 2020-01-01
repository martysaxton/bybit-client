package io.xooxo.bybit

import io.xooxo.bybit.impl.ByBitClientImpl
import io.xooxo.bybit.model.OrderBook

fun main(args: Array<String>) {
    val client = ByBitClientImpl(ByBitClient.Type.LIVE)
    val book = OrderBook()
    client.setCloseListener { println("websocket closed") }
//    client.setTradeListener { println("trade: $it") }
//    client.setInstrumentInfoListeners(
//            { println("instrument info snapshot:  $it") },
//            { println("instrument info delta: $it") }
//    )
    client.setDepthListeners(
            {
                book.applySnapshot(it)
                printBook(book)
            },
            {
                book.applyDelta(it)
                printBook(book)
            }
    )
    client.setConnectListener {
        println("websocket connected")
//        client.subscribeToTrades()
//        client.subscribeToInstrumentInfo("BTCUSD")
        client.subscribeToOrderBook("BTCUSD")
    }
    client.connectWebSocket()

}

fun printBook(book: OrderBook) {
    val bids = book.getBids()
    val asks = book.getAsks()
    val firstBid = bids[0]
    val firstAsk = asks[0]
    println("bids bids=${bids.size} size=asks${asks.size}  fb=$firstBid fa=$firstAsk")

}