package io.xooxo.bybit.model

data class DepthElement(
        val price: String,
        val symbol: String,
        val id: Long,
        val side: String,
        val size: Long
)

data class DepthDeleteElement(
        val price: String,
        val symbol: String,
        val id: Long,
        val side: String
)

data class DepthUpdateData(
        val delete: Array<DepthDeleteElement>,
        val update: Array<DepthElement>,
        val insert: Array<DepthElement>
)

typealias DepthSnapshotMessage = ByBitMessage<Array<DepthElement>>
typealias DepthDeltaMessage = ByBitMessage<DepthUpdateData>

typealias DepthSnapshotListener = (depthSnapshot: DepthSnapshotMessage) -> Unit
typealias DepthDeltaListener = (depthSnapshot: DepthDeltaMessage) -> Unit

