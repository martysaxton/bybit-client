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
        client.setTradeLisetner { tradeReceived = true }
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

}

