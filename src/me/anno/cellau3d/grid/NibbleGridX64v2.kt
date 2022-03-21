package me.anno.cellau3d.grid

import me.anno.cellau3d.NeighborHood
import me.anno.cellau3d.NibbleArray
import kotlin.math.ceil
import kotlin.math.log2

open class NibbleGridX64v2(
    sx: Int, sy: Int, sz: Int, stateBits: Int,
    val neighborHood: NeighborHood
) : NibbleGridX64(sx, sy, sz, stateBits) {

    val neighborCount = NibbleArray(ceil(log2(neighborHood.neighbors.size + 1f)).toInt(), size4)

    override fun clear() {
        super.clear()
        neighborCount.clear()
    }

    fun getCount(x: Int, y: Int, z: Int): Int {
        return neighborCount[getIndex(x, y, z)]
    }

    override fun set(x: Int, y: Int, z: Int, v: Int) {
        val index = getIndex(x, y, z)
        synchronized(Unit) {
            val delta = isSet[index] - v
            val diVectors = neighborHood.neighbors
            for (i in diVectors.indices) {
                val di = diVectors[i]
                val xi = x + di.x
                val yi = y + di.y
                val zi = z + di.z
                if (xi in 0 until sx && yi in 0 until sy && zi in 0 until sz) {
                    neighborCount[getIndex(xi, yi, zi)] += delta
                }
            }
            isSet[index] = v
        }
    }

}