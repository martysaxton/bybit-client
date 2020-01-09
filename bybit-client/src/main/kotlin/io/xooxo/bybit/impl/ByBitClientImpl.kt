package io.xooxo.bybit.impl


import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.convertValue
import io.xooxo.bybit.ByBitClient
import io.xooxo.bybit.model.DepthDeltaListener
import io.xooxo.bybit.model.DepthDeltaMessage
import io.xooxo.bybit.model.DepthSnapshotListener
import io.xooxo.bybit.model.DepthSnapshotMessage
import io.xooxo.bybit.model.InstrumentInfoDeltaListener
import io.xooxo.bybit.model.InstrumentInfoDeltaMessage
import io.xooxo.bybit.model.InstrumentInfoSnapshotListener
import io.xooxo.bybit.model.InstrumentInfoSnapshotMessage
import io.xooxo.bybit.model.TradeListener
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

class ByBitClientImpl(private val type: ByBitClient.Type) : ByBitClient {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ByBitClientImpl::class.java)
        const val PING_INTERVAL = 10000L
    }

    init {
        log.debug("instantiating {}", ByBitClientImpl::class.simpleName)
    }

    private var connected = false

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

    override fun connectWebSocket() {
        log.info("connecting to {}", type.webSocketUrl)
        closing = false
        websocket = WebsocketClient.nonBlocking(Uri.of(type.webSocketUrl)) { it ->
            it.run {
                log.info("connected to {}", type.webSocketUrl)
                connected = true
                connectListener?.let { it() }
                fixedRateTimer("byBitPing", true, PING_INTERVAL, PING_INTERVAL) { executePing() }
            }
        }

        websocket?.run {
            onMessage(::onMessage)
//            onMessage {
//            }

            onClose {
                log.info("websocket closing")
                connected = false
                closeListener?.let { it() }
                if (!closing) {
                    connectWebSocket()
                }
            }

            onError { log.error("error", it) }
        }
    }

    private fun onMessage(message: WsMessage) {
        log.debug("websocket received: {}", message)
        val json = jsonLens(message)
        // test if op response
        if (json.has("success")) {
            handleOpResponse(json)
        } else {
            val topic: String = json["topic"].textValue()
            when {
                topic.startsWith("trade.") -> {
                    handleTradeMessage(json)
                }
                topic.startsWith("orderBookL2_25.") -> {
                    when (val type: String = json["type"].textValue()) {
                        "snapshot" -> {
                            handleDepthSnapshot(json)
                        }
                        "delta" -> {
                            handleDepthDelta(json)
                        }
                        else -> {
                            throw UnrecoverableByBitException("unknown depth event type: $type")
                        }
                    }
                }
                topic.startsWith("instrument_info.") -> {
                    when (val type: String = json["type"].textValue()) {
                        "snapshot" -> {
                            handleInstrumentInfoSnapshot(json)
                        }
                        "delta" -> {
                            handleInstrumentInfoDelta(json)
                        }
                        else -> {
                            throw UnrecoverableByBitException("unknown depth event type: $type")
                        }
                    }
                }
            }
        }
    }

    private fun executePing() {
        if (connected) {
            log.debug("sending ping")
            websocket?.send(WsMessage("{\"op\":\"ping\"}"))
        }
    }

    private fun handleInstrumentInfoDelta(json: JsonNode) {
        val instrumentInfoDeltaMessage: InstrumentInfoDeltaMessage = Jackson.mapper.convertValue(json)
        log.debug("received instrument info delta: {}", instrumentInfoDeltaMessage)
        instrumentInfoDeltaListener?.let { it(instrumentInfoDeltaMessage) }
    }

    private fun handleInstrumentInfoSnapshot(json: JsonNode) {
        val instrumentInfoSnapshotMessage: InstrumentInfoSnapshotMessage = Jackson.mapper.convertValue(json)
        log.debug("received instrument info snapshot: {}", instrumentInfoSnapshotMessage)
        instrumentInfoSnapshotListener?.let { it(instrumentInfoSnapshotMessage) }
    }

    private fun handleDepthDelta(json: JsonNode) {
        val depthDeltaMessage: DepthDeltaMessage = Jackson.mapper.convertValue(json)
        log.debug("received depth delta: {}", depthDeltaMessage)
        depthDeltaListener?.let { it(depthDeltaMessage) }
    }

    private fun handleDepthSnapshot(json: JsonNode) {
        val depthSnapshotMessage: DepthSnapshotMessage = Jackson.mapper.convertValue(json)
        log.debug("received depth snapshot: {}", depthSnapshotMessage)
        depthSnapshotListener?.let { it(depthSnapshotMessage) }
    }

    private fun handleTradeMessage(json: JsonNode) {
        val tradeMessage = Jackson.asA(json, TradeMessage::class)
        log.debug("received trade message: {}", tradeMessage)
        tradeListener?.let { it(tradeMessage) }
    }

    private fun handleOpResponse(json: JsonNode) {
        val success = json["success"].booleanValue()
        if (success) {
            log.debug("op response {}", json)
        } else {
            log.error("op response {}", json)
        }
    }

    override fun close() {
        closing = true
        websocket?.close()
    }

    override fun setConnectListener(connectListener: () -> Unit) {
        this.connectListener = connectListener
    }

    override fun setCloseListener(closeListener: () -> Unit) {
        this.closeListener = closeListener
    }

    override fun subscribeToTrades() {
        websocket!!.send(WsMessage("{\"op\":\"subscribe\",\"args\":[\"trade\"]}"))
    }

    override fun setTradeListener(tradeListener: (tradeMessage: TradeMessage) -> Unit) {
        this.tradeListener = tradeListener
    }

    override fun subscribeToDepth(symbol: String) {
        websocket!!.send(WsMessage("{\"op\": \"subscribe\", \"args\": [\"orderBookL2_25.$symbol\"]}"))
    }

    override fun setDepthListeners(
        depthSnapshotListener: DepthSnapshotListener,
        depthDeltaListener: DepthDeltaListener
    ) {
        this.depthSnapshotListener = depthSnapshotListener
        this.depthDeltaListener = depthDeltaListener
    }

    override fun subscribeToInstrumentInfo(symbol: String) {
        websocket!!.send(WsMessage("{\"op\":\"subscribe\",\"args\":[\"instrument_info.100ms.$symbol\"]}"))
    }

    override fun setInstrumentInfoListeners(
        instrumentInfoSnapshotListener: InstrumentInfoSnapshotListener,
        instrumentInfoDeltaListener: InstrumentInfoDeltaListener
    ) {
        this.instrumentInfoSnapshotListener = instrumentInfoSnapshotListener
        this.instrumentInfoDeltaListener = instrumentInfoDeltaListener
    }
}

