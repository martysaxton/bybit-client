package io.xooxo.bybit.model

import java.util.*

data class InstrumentInfo(
        val id: Long,
        val symbol: String,                     //instrument name
        val last_price_e4: Long?,             //the latest price
        val last_tick_direction: String?,  //the direction of last tick:PlusTick,ZeroPlusTick,MinusTick,ZeroMinusTick
        val prev_price_24h_e4: Long?,         //the price of prev 24h
        val price_24h_pcnt_e6: Long?,                 //the current lastprice percentage change from prev 24h price
        val high_price_24h_e4: Long?,         //the highest price of prev 24h
        val low_price_24h_e4: Long?,           //the lowest price of prev 24h
        val prev_price_1h_e4: Long?,           //the price of prev 1h
        val price_1h_pcnt_e6: Long?,             //the current lastprice percentage change from prev 1h price
        val mark_price_e4: Long?,              //mark price
        val index_price_e4: Long?,             //index price
        val open_interest: Long?,                //open interest quantity - Attention, the update is not immediate - slowest update is 1 minute
        val open_value_e8: Long?,            //open value quantity - Attention, the update is not immediate - the slowest update is 1 minute
        val total_turnover_e8: Long?,      //total turnover
        val turnover_24h_e8: Long?,          //24h turnover
        val total_volume: Long?,               //total volume
        val volume_24h: Long?,                   //24h volume
        val funding_rate_e6: Long?,               //funding rate
        val predicted_funding_rate_e6: Long?,     //predicted funding rate
        val cross_seq: Long,                      //sequence
        val created_at: Date,
        val updated_at: Date,
        val next_funding_time: Date,//next funding time
        val countdown_hour: Long?                     //the rest time to settle funding fee
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

