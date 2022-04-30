package me.anno.cellau3d

import me.anno.maths.Maths.ceilDiv

class NibbleArray(
    private val bitsPerInstance: Int,
    private val length: Int
) {

    init {
        if (bitsPerInstance !in 1..32) throw IllegalArgumentException("Bits per instance must be in 1 .. 32")
    }

    fun clear() {
        data.fill(0L)
    }

    private val instancesPerLong = 64 / bitsPerInstance
    private val longLength = ceilDiv(length, instancesPerLong)
    val data = LongArray(longLength)
    private val instanceMask = (1L shl bitsPerInstance) - 1

    operator fun get(index: Int): Int {
        if (index < 0 || index >= length) throw IndexOutOfBoundsException("Index $index is out of bounds (0 until $length)!")
        val arrayIndex = index / instancesPerLong
        val localIndex = index % instancesPerLong
        val shift = localIndex * bitsPerInstance
        return data[arrayIndex].shr(shift).and(instanceMask).toInt()
    }

    operator fun set(index: Int, value: Int) {
        if (index < 0 || index >= length) throw IndexOutOfBoundsException("Index $index is out of bounds (0 until $length)!")
        val arrayIndex = index / instancesPerLong
        val localIndex = index % instancesPerLong
        val shift = localIndex * bitsPerInstance
        val mask = instanceMask shl shift
        val clearValue = data[arrayIndex] and mask.inv()
        data[arrayIndex] = clearValue or ((value.toLong() shl shift) and mask)
    }
}