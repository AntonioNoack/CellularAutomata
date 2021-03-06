package me.anno.cellau3d.grid

open class ByteArrayGrid(sx: Int, sy: Int, sz: Int, stateBits: Int) : Grid(sx, sy, sz, stateBits) {

    private val flags = ByteArray(size)
    private val states = ByteArray(size)

    override fun clear() {
        flags.fill(0)
        states.fill(0)
    }

    private fun getIndex(x: Int, y: Int, z: Int): Int {
        return x + sx * (y + sy * z)
    }

    override fun forAllFilled(run: (x: Int, y: Int, z: Int) -> Unit) {
        var index = 0
        val flags = flags
        for (z in 0 until sz) {
            for (y in 0 until sy) {
                for (x in 0 until sx) {
                    if (flags[index++] != 0.toByte()) {
                        run(x, y, z)
                    }
                }
            }
        }
    }

    override fun get(x: Int, y: Int, z: Int): Int {
        return flags[getIndex(x, y, z)].toInt().and(255)
    }

    override fun get(x: Int, y: Int, z: Int, d: Int): Int {
        if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return d
        return flags[getIndex(x, y, z)].toInt().and(255)
    }

    override fun set(x: Int, y: Int, z: Int, v: Int) {
        flags[getIndex(x, y, z)] = v.toByte()
    }

    override fun getState(x: Int, y: Int, z: Int): Int {
        return states[getIndex(x, y, z)].toInt().and(255)
    }

    override fun setState(x: Int, y: Int, z: Int, state: Int) {
        states[getIndex(x, y, z)] = state.toByte()
    }

}