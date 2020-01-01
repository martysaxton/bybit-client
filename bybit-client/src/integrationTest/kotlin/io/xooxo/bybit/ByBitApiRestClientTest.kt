package io.xooxo.bybit

import io.xooxo.bybit.impl.ByBitApiRestClientHttp4k
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test



import java.time.Instant;
import kotlin.math.abs

class ByBitApiRestClientTest {

    val client: ByBitApiRestClient = ByBitApiRestClientHttp4k(ByBitApiRestClientHttp4k.Type.TEST)

    @Test
    fun `ping`() {
//        client.ping()
    }

}