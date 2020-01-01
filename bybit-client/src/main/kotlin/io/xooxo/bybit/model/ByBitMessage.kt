package io.xooxo.bybit.model

data class ByBitMessage<T>(
        val topic: String,
        val type: String,
        val cross_seq: Long,
        val timestamp_e6: Long,
        val data: T

)
