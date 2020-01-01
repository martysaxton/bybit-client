package io.xooxo.bybit.impl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.xooxo.bybit.ByBitClient
import io.xooxo.bybit.model.TradeMessage
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson.auto
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.concurrent.fixedRateTimer


object Jackson : ConfigurableJackson(KotlinModule()
        .asConfigurable()
        .withStandardMappings()
        .done()
        .deactivateDefaultTyping()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false)
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, false)
)

class ByBitClientImpl(val type: ByBitClient.Type) : ByBitClient {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ByBitClientImpl::class.java)
    }

    init {
        log.debug("instantiating {}", ByBitClientImpl::class.simpleName)
    }

    private var onTrade: ((tradeMessage: TradeMessage) -> Unit)? = null
    private var onConnect: (() -> Unit)? = null
    private var onClose: (() -> Unit)? = null
    private var closing = false
    private var websocket: Websocket? = null

    private val jsonLens = WsMessage.auto<JsonNode>().toLens()

    override fun connectWebSocket() {
        log.info("connecting to {}", type.webSocketUrl)
        closing = false
        websocket = WebsocketClient.nonBlocking(Uri.of(type.webSocketUrl)) { it ->
            it.run {
                log.info("connected to {}", type.webSocketUrl)
                onConnect?.let { it() }
                fixedRateTimer("byBitPing", true, 10000, 10000) {
                    log.debug("sending ping")
                    send(WsMessage("{\"op\":\"ping\"}"))
                }
            }
        }

        websocket?.run {
            onMessage {
                log.debug("websocket received: {}", it)
                val json = jsonLens(it)
                // test if op response
                if (json.has("success")) {
                    handleOpResponse(json)
                } else {
                    val topic: String = json["topic"].textValue()
                    if (topic.startsWith("trade.")) {
                        handleTradeMessage(json)
                    }
                }
            }

            onClose {
                log.info("websocket closing")
                onClose?.let { it() }
                if (!closing) {
                    connectWebSocket()
                }
            }

            onError { log.error("error", it) }
        }
    }


    private fun handleTradeMessage(json: JsonNode) {
        val tradeMessage = Jackson.asA(json, TradeMessage::class)
        log.debug("received trade message: {}", tradeMessage)
        onTrade?.let { it(tradeMessage) }
    }

    private fun handleOpResponse(json: JsonNode) {
        log.info("op response {}", json)
    }

    override fun close() {
        closing = true
        websocket?.close()
    }

    override fun setConnectListener(connectListener: () -> Unit) {
        this.onConnect = connectListener
    }

    override fun setCloseListener(connectListener: () -> Unit) {
        this.onClose = connectListener
    }

    override fun subscribeToTrades() {
        websocket!!.send(WsMessage("{\"op\":\"subscribe\",\"args\":[\"trade\"]}"))
    }

    override fun setTradeLisetner(tradeListener: (tradeMessage: TradeMessage) -> Unit) {
        onTrade = tradeListener
    }
}

