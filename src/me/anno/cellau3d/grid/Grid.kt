package me.anno.cellau3d.grid

import me.anno.utils.hpc.WorkSplitter
import java.util.*

abstract class Grid(val sx: Int, val sy: Int, val sz: Int, val stateBits: Int) {

    val maxStates = 1 shl stateBits
    val size = sx * sy * sz

    // 0 - 27 -> not worth to cache it
    open fun get27(x: Int, y: Int, z: Int): Int {
        return get9xy(x, y, z - 1) + get9xy(x, y, z) + get9xy(x, y, z + 1)
    }

    // 0 - 9 -> 4 bits
    open fun get9xy(x: Int, y: Int, z: Int): Int {
        return get3x(x, y - 1, z) + get3x(x, y, z) + get3x(x, y + 1, z)
    }

    // to do this is so symmetrical, that we could cache values for 3d
    // 0 - 3 -> 2 bits
    open fun get3x(x: Int, y: Int, z: Int): Int {
        return get(x - 1, y, z, 0) + get(x, y, z, 0) + get(x + 1, y, z, 0)
    }

    abstract fun get(x: Int, y: Int, z: Int): Int
    abstract fun get(x: Int, y: Int, z: Int, d: Int): Int

    abstract fun set(x: Int, y: Int, z: Int, v: Int)

    abstract fun getState(x: Int, y: Int, z: Int): Int
    abstract fun setState(x: Int, y: Int, z: Int, state: Int)

    abstract fun forAllFilled(run: (x: Int, y: Int, z: Int) -> Unit)

    fun getStateIfFull(x: Int, y: Int, z: Int): Int {
        return if (get(x, y, z, 0) != 0) getState(x, y, z) else 0
    }

    open fun forAllFilled(pool: WorkSplitter, run: (x: Int, y: Int, z: Int) -> Unit) {
        forAllFilled(run)
    }

    abstract fun clear()

    open fun isEmpty(): Boolean {
        try {
            forAllFilled { _, _, _ -> throw RuntimeException() }
        } catch (e: RuntimeException) {
            return false
        }
        return true
    }

    // from https://github.com/TanTanDev/3d_celluar_automata/blob/main/src/utils.rs
    fun spawnNoise(radius: Int, density: Float, state: Int, random: Random = Random(1234L)) {

        // 1728 :3
        val x0 = sx / 2 - radius
        val y0 = sy / 2 - radius
        val z0 = sz / 2 - radius

        val ss = radius * 2 + 1
        val samples = (density * ss * ss * ss + random.nextFloat()).toInt()

        for (i in 0 until samples) {
            val x = x0 + random.nextInt(ss)
            val y = y0 + random.nextInt(ss)
            val z = z0 + random.nextInt(ss)
            if (x in 0 until sx && y in 0 until sy && z in 0 until sz) {
                set(x, y, z, 1)
                setState(x, y, z, state)
            }
        }

    }


}