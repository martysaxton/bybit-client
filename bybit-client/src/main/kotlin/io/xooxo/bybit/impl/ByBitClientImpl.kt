package io.xooxo.bybit.impl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.xooxo.bybit.ByBitClient
import io.xooxo.bybit.model.*
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

    private var depthSnapshotListener: DepthSnapshotListener? = null
    private var depthDeltaListener: DepthDeltaListener? = null

    private var instrumentInfoSnapshotListener: InstrumentInfoSnapshotListener? = null
    private var instrumentInfoDeltaListener: InstrumentInfoDeltaListener? = null

    private var tradeListener: TradeListener? = null
    private var connectListener: (() -> Unit)? = null
    private var closeListener: (() -> Unit)? = null

    private var closing = false
    private var websocket: Websocket? = null

    private val jsonLens = WsMessage.auto<JsonNode>().toLens()

    val mapper = jacksonObjectMapper()


    override fun connectWebSocket() {
        log.info("connecting to {}", type.webSocketUrl)
        closing = false
        websocket = WebsocketClient.nonBlocking(Uri.of(type.webSocketUrl)) { it ->
            it.run {
                log.info("connected to {}", type.webSocketUrl)
                connectListener?.let { it() }
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
                    } else if (topic.startsWith("orderBookL2_25.")) {
                        when (val type: String = json["type"].textValue()) {
                            "snapshot" -> {
                                handleDepthSnapshot(json)
                            }
                            "delta" -> {
                                handleDepthDelta(json)
                            }
                            else -> {
                                throw RuntimeException("unknown depth event type: ${type}")
                            }
                        }
                    } else if (topic.startsWith("instrument_info.")) {
                        when (val type: String = json["type"].textValue()) {
                            "snapshot" -> {
                                handleInstrumentInfoSnapshot(json)
                            }
                            "delta" -> {
                                handleInstrumentInfoDelta(json)
                            }
                            else -> {
                                throw RuntimeException("unknown depth event type: ${type}")
                            }
                        }

                    }
                }
            }

            onClose {
                log.info("websocket closing")
                closeListener?.let { it() }
                if (!closing) {
                    connectWebSocket()
                }
            }

            onError { log.error("error", it) }
        }
    }

    private fun handleInstrumentInfoDelta(json: JsonNode) {
        val instrumentInfoDeltaMessage: InstrumentInfoDeltaMessage = Jackson.mapper.treeToValue(json)
        log.debug("received instrument info delta: {}", instrumentInfoDeltaMessage)
        instrumentInfoDeltaListener?.let { it(instrumentInfoDeltaMessage) }

    }

    private fun handleInstrumentInfoSnapshot(json: JsonNode) {
        val instrumentInfoSnapshotMessage: InstrumentInfoSnapshotMessage = Jackson.mapper.treeToValue(json)
        log.debug("received instrument info snapshot: {}", instrumentInfoSnapshotMessage)
        instrumentInfoSnapshotListener?.let { it(instrumentInfoSnapshotMessage) }
    }

    private fun handleDepthDelta(json: JsonNode) {
        val depthDeltaMessage: DepthDeltaMessage = Jackson.mapper.treeToValue(json)
        log.debug("received depth delta: {}", depthDeltaMessage)
        depthDeltaListener?.let { it(depthDeltaMessage) }
    }

    private fun handleDepthSnapshot(json: JsonNode) {
        val depthSnapshotMessage: DepthSnapshotMessage = Jackson.mapper.treeToValue(json)
        log.debug("received depth snapshot: {}", depthSnapshotMessage)
        depthSnapshotListener?.let { it(depthSnapshotMessage) }
    }


    private fun handleTradeMessage(json: JsonNode) {
        val tradeMessage = Jackson.asA(json, TradeMessage::class)
        log.debug("received trade message: {}", tradeMessage)
        tradeListener?.let { it(tradeMessage) }
    }

    private fun handleOpResponse(json: JsonNode) {
        log.info("op response {}", json)
    }

    override fun close() {
        closing = true
        websocket?.close()
    }

    override fun setConnectListener(connectListener: () -> Unit) {
        this.connectListener = connectListener
    }

    override fun setCloseListener(connectListener: () -> Unit) {
        this.closeListener = connectListener
    }

    override fun subscribeToTrades() {
        websocket!!.send(WsMessage("{\"op\":\"subscribe\",\"args\":[\"trade\"]}"))
    }

    override fun setTradeListener(tradeListener: (tradeMessage: TradeMessage) -> Unit) {
        this.tradeListener = tradeListener
    }

    override fun subscribeToOrderBook(symbol: String) {
        websocket!!.send(WsMessage("{\"op\": \"subscribe\", \"args\": [\"orderBookL2_25.$symbol\"]}"))
    }

    override fun setDepthListeners(depthSnapshotListener: DepthSnapshotListener, depthDeltaListener: DepthDeltaListener) {
        this.depthSnapshotListener = depthSnapshotListener
        this.depthDeltaListener = depthDeltaListener
    }

    override fun subscribeToInstrumentInfo(symbol: String) {
        websocket!!.send(WsMessage("{\"op\":\"subscribe\",\"args\":[\"instrument_info.100ms.$symbol\"]}"))
    }

    override fun setInstrumentInfoListeners(instrumentInfoSnapshotListener: InstrumentInfoSnapshotListener, instrumentInfoDeltaListener: InstrumentInfoDeltaListener) {
        this.instrumentInfoSnapshotListener = instrumentInfoSnapshotListener
        this.instrumentInfoDeltaListener = instrumentInfoDeltaListener
    }

}

