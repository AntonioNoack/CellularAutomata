package me.anno.cellau3d.grid

import me.anno.cellau3d.NibbleArray
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.hpc.WorkSplitter

open class NibbleGridX64(sx: Int, sy: Int, sz: Int, stateBits: Int) : Grid(sx, sy, sz,  stateBits) {

    // bounds for indices
    val sx4 = ceilDiv(sx, 4)
    val sy4 = ceilDiv(sy, 4)
    val sz4 = ceilDiv(sz, 4)
    val size4 = sx4 * sy4 * sz4 * 64

    val isSet = NibbleArray(1, size4)
    val states = NibbleArray(stateBits, size4)

    override fun clear() {
        isSet.clear()
        states.clear()
    }

    fun getIndex(x: Int, y: Int, z: Int): Int {
        // be smart about indices:
        val lx = x and 3
        val ly = y and 3
        val lz = z and 3
        val li = lx + ly.shl(2) + lz.shl(4)
        val hx = x shr 2
        val hy = y shr 2
        val hz = z shr 2
        return (hx + sx4 * (hy + sy4 * hz)).shl(6) + li
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

    private fun process64(majorIndex: Int, flagV: Long, run: (x: Int, y: Int, z: Int) -> Unit){
        // process sub chunk
        // (hx + sx4 * (hy + sy4 * hz)).shl(6) + li
        val hx = (majorIndex % sx4) shl 2
        val idxYZ = majorIndex / sx4
        val hy = (idxYZ % sy4) shl 2
        val hz = (idxYZ / sy4) shl 2
        for (si in 0 until 64) {
            val mask = 1L shl si
            if (flagV.and(mask) != 0L) {
                val lx = si and 3
                val ly = (si shr 2) and 3
                val lz = (si shr 4) and 3
                val x = lx + hx
                val y = ly + hy
                val z = lz + hz
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