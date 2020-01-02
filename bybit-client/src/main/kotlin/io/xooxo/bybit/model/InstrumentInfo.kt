package io.xooxo.bybit.model

import java.util.*

data class InstrumentInfo(
    val id: Long,
    val symbol: String,
    val last_price_e4: Long?,
    val last_tick_direction: String?,
    val prev_price_24h_e4: Long?,
    val price_24h_pcnt_e6: Long?,
    val high_price_24h_e4: Long?,
    val low_price_24h_e4: Long?,
    val prev_price_1h_e4: Long?,
    val price_1h_pcnt_e6: Long?,
    val mark_price_e4: Long?,
    val index_price_e4: Long?,
    val open_interest: Long?,
    val open_value_e8: Long?,
    val total_turnover_e8: Long?,
    val turnover_24h_e8: Long?,
    val total_volume: Long?,
    val volume_24h: Long?,
    val funding_rate_e6: Long?,
    val predicted_funding_rate_e6: Long?,
    val cross_seq: Long,
    val created_at: Date,
    val updated_at: Date,
    val next_funding_time: Date?,
    val countdown_hour: Long?
)

data class InstrumentInfoDeltaData(
    val delete: Array<InstrumentInfo>,
    val update: Array<InstrumentInfo>,
    val insert: Array<InstrumentInfo>
)

typealias InstrumentInfoSnapshotMessage = ByBitMessage<InstrumentInfo>
typealias InstrumentInfoDeltaMessage = ByBitMessage<InstrumentInfoDeltaData>

typealias InstrumentInfoSnapshotListener = (instrumentInfoSnapshotMessage: InstrumentInfoSnapshotMessage) -> Unit
typealias InstrumentInfoDeltaListener = (instrumentInfoDeltaMessage: InstrumentInfoDeltaMessage) -> Unit

