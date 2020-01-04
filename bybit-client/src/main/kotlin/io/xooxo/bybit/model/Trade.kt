package io.xooxo.bybit.model

import java.math.BigDecimal
import java.util.*

data class TradeMessage(val topic: String, val data: Array<Trade>)

data class Trade(
    val timestamp: Date,
    val symbol: String,
    val side: String,
    val size: Long,
    val price: BigDecimal,
    val tick_direction: String,
    val trade_id: String,
    val cross_seq: Long
)

typealias TradeListener = (tradeMessage: TradeMessage) -> Unit
