package me.anno.cellau3d.grid

import me.anno.cellau3d.NibbleArray
import me.anno.utils.hpc.WorkSplitter

open class NibbleGrid(sx: Int, sy: Int, sz: Int, stateBits: Int) : Grid(sx, sy, sz, stateBits) {

    val isSet = NibbleArray(1, size)
    val states = NibbleArray(stateBits, size)

    override fun clear() {
        isSet.clear()
        states.clear()
    }

    fun getIndex(x: Int, y: Int, z: Int): Int {
        return x + sx * (y + sy * z)
    }

    override fun forAllFilled(run: (x: Int, y: Int, z: Int) -> Unit) {
        val data = isSet.data
        for (majorIndex in data.indices) {
            val flagV = data[majorIndex]
            if (flagV != 0L) process64(majorIndex, flagV, run)
        }
    }

    override fun forAllFilled(pool: WorkSplitter, run: (x: Int, y: Int, z: Int) -> Unit) {
        pool.processBalanced(0, isSet.data.size, 16) { i0, i1 ->
            for (majorIndex in i0 until i1) {
                val data = isSet.data
                val flagV = data[majorIndex]
                if (flagV != 0L) process64(majorIndex, flagV, run)
            }
        }
    }

    private fun process64(majorIndex: Int, flagV: Long, run: (x: Int, y: Int, z: Int) -> Unit) {
        // process sub chunk
        // (hx + sx4 * (hy + sy4 * hz)).shl(6) + li
        for (si in 0 until 64) {
            val mask = 1L shl si
            if (flagV.and(mask) != 0L) {
                val index = majorIndex * 64 + si
                val x = index % sx
                val idxYZ = index / sx
                val y = idxYZ % sy
                val z = idxYZ / sy
                run(x, y, z)
            }
        }
    }

    override fun isEmpty(): Boolean {
        val data = isSet.data
        for (majorIndex in data.indices) {
            val flagV = data[majorIndex]
            if (flagV != 0L) return false
        }
        return true
    }

    override fun get(x: Int, y: Int, z: Int): Int {
        return isSet[getIndex(x, y, z)]
    }

    override fun get(x: Int, y: Int, z: Int, d: Int): Int {
        if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return d
        return isSet[getIndex(x, y, z)]
    }

    override fun set(x: Int, y: Int, z: Int, v: Int) {
        isSet[getIndex(x, y, z)] = v
    }

    override fun getState(x: Int, y: Int, z: Int): Int {
        return states[getIndex(x, y, z)]
    }

    override fun setState(x: Int, y: Int, z: Int, state: Int) {
        states[getIndex(x, y, z)] = state
    }
}