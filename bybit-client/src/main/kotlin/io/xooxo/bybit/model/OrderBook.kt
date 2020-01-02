package io.xooxo.bybit.model

import java.math.BigDecimal
import java.util.SortedMap
import java.util.TreeMap


data class BidAsk(val price: BigDecimal, val size: BigDecimal)

private class BigDecimalDescendingComparator : Comparator<BigDecimal> {
    override fun compare(o1: BigDecimal?, o2: BigDecimal?): Int {
        if (o1 == null) {
            return 1
        }
        return o1.compareTo(o2) * -1
    }
}

class OrderBook {

    companion object {
        const val BIDS_SIDE = "Buy"
    }

    private val bids: SortedMap<BigDecimal, BidAsk> = TreeMap(BigDecimalDescendingComparator())
    private val asks: SortedMap<BigDecimal, BidAsk> = TreeMap()

    fun getBids(): List<BidAsk> {
        return bids.values.toList()
    }

    fun getAsks(): List<BidAsk> {
        return asks.values.toList()
    }

    fun applySnapshot(snapshot: DepthSnapshotMessage) {
        bids.clear()
        asks.clear()
        snapshot.data.forEach {
            applyDepthElement(it)
        }
    }

    fun applyDelta(delta: DepthDeltaMessage) {
        delta.data.delete.forEach {
            val price = BigDecimal(it.price)
            if (it.side == BIDS_SIDE) {
                bids.remove(price)
            } else {
                asks.remove(price)
            }
        }

        delta.data.update.forEach { applyDepthElement(it) }
        delta.data.insert.forEach { applyDepthElement(it) }
    }

    private fun applyDepthElement(depthElement: DepthElement) {
        val price = BigDecimal(depthElement.price)
        val bidAsk = BidAsk(price, BigDecimal(depthElement.size))
        if (depthElement.side == BIDS_SIDE) {
            bids[price] = bidAsk
        } else asks[price] = bidAsk
    }
}
