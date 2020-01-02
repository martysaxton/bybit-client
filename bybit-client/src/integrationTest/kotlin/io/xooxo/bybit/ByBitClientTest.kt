package io.xooxo.bybit


import io.xooxo.bybit.impl.ByBitClientImpl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ByBitClientTest {

    companion object {
        private const val TWO_SECONDS = 2000L
        private const val ONE_SECOND = 1000L
        private const val RETRIES = 60
    }

    private val client: ByBitClient = ByBitClientImpl(ByBitClient.Type.LIVE)

    @Test
    fun connectionTest() {
        var connectCalled = false
        client.setConnectListener { connectCalled = true }
        client.connectWebSocket()
        Thread.sleep(TWO_SECONDS)
        assertTrue(connectCalled)

        var closeCalled = false
        client.setCloseListener { closeCalled = true }
        client.close()
        Thread.sleep(TWO_SECONDS)
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
            Thread.sleep(ONE_SECOND)
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
        for (i in 1..RETRIES) {
            if (depthSnapshotReceived && depthDeltaReceived) {
                break
            }
            Thread.sleep(ONE_SECOND)
        }
        assertTrue(depthSnapshotReceived)
        assertTrue(depthDeltaReceived)
    }

    @Test
    fun instrumentInfo() {
        var instrumentInfoSnapshotReceived = false
        var instrumentInfoDeltaReceived = false
        client.setInstrumentInfoListeners(
            { instrumentInfoSnapshotReceived = true },
            { instrumentInfoDeltaReceived = true }
        )
        client.setConnectListener { client.subscribeToInstrumentInfo("BTCUSD") }
        client.connectWebSocket()
        for (i in 1..RETRIES) {
            if (instrumentInfoSnapshotReceived && instrumentInfoDeltaReceived) {
                break
            }
            Thread.sleep(ONE_SECOND)
        }
        assertTrue(instrumentInfoSnapshotReceived)
        assertTrue(instrumentInfoDeltaReceived)
    }
}

