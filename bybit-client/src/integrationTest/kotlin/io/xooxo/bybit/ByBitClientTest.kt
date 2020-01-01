package io.xooxo.bybit


import io.xooxo.bybit.impl.ByBitClientImpl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ByBitClientTest {

    val client: ByBitClient = ByBitClientImpl(ByBitClient.Type.LIVE)

    @Test
    fun `connection test`() {
        var connectCalled = false
        client.setConnectListener { connectCalled = true }
        client.connectWebSocket()
        Thread.sleep(2000)
        assertTrue(connectCalled)

        var closeCalled = false
        client.setCloseListener { closeCalled = true }
        client.close()
        Thread.sleep(2000)
        assertTrue(closeCalled)
    }

    @Test
    fun trades() {
        var tradeReceived = false
        client.setTradeListener { tradeReceived = true }
        client.setConnectListener { client.subscribeToTrades() }
        client.connectWebSocket()
        for (i in 1..60) {
            if (tradeReceived) {
                break
            }
            Thread.sleep(1000)
        }
        assertTrue(tradeReceived)
    }

    @Test
    fun orderBook() {
        var depthSnapshotReceived = false
        var depthDeltaReceived = false
        client.setDepthListeners({ depthSnapshotReceived = true }, { depthDeltaReceived = true })
        client.setConnectListener { client.subscribeToOrderBook("BTCUSD") }
        client.connectWebSocket()
        for (i in 1..60) {
            if (depthSnapshotReceived && depthDeltaReceived) {
                break
            }
            Thread.sleep(1000)
        }
        assertTrue(depthSnapshotReceived)
        assertTrue(depthDeltaReceived)
    }
}

